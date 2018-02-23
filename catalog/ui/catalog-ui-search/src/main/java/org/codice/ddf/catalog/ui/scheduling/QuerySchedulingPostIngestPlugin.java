/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.catalog.ui.scheduling;

import static ddf.util.Fallible.error;
import static ddf.util.Fallible.forEach;
import static ddf.util.Fallible.of;
import static ddf.util.Fallible.ofNullable;
import static ddf.util.Fallible.success;

import ddf.catalog.CatalogFramework;
import ddf.catalog.Constants;
import ddf.catalog.data.Metacard;
import ddf.catalog.federation.FederationException;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PostIngestPlugin;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.security.Subject;
import ddf.util.Fallible;
import ddf.util.MapUtils;
import java.nio.charset.Charset;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.cache.CacheException;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteException;
import org.apache.ignite.IgniteScheduler;
import org.apache.ignite.IgniteState;
import org.apache.ignite.Ignition;
import org.apache.ignite.scheduler.SchedulerFuture;
import org.apache.ignite.transactions.TransactionException;
import org.boon.json.JsonException;
import org.boon.json.JsonFactory;
import org.codice.ddf.catalog.ui.metacard.workspace.QueryMetacardTypeImpl;
import org.codice.ddf.catalog.ui.metacard.workspace.WorkspaceAttributes;
import org.codice.ddf.catalog.ui.metacard.workspace.WorkspaceMetacardImpl;
import org.codice.ddf.catalog.ui.metacard.workspace.WorkspaceTransformer;
import org.codice.ddf.catalog.ui.scheduling.subscribers.QueryCourier;
import org.codice.ddf.persistence.PersistenceException;
import org.codice.ddf.persistence.PersistentStore;
import org.codice.ddf.persistence.PersistentStore.PersistenceType;
import org.codice.ddf.security.common.Security;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuerySchedulingPostIngestPlugin implements PostIngestPlugin {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(QuerySchedulingPostIngestPlugin.class);

  public static final String DELIVERY_METHODS_KEY = "deliveryMethods";

  public static final String DELIVERY_METHOD_ID_KEY = "deliveryId";

  public static final String DELIVERY_PARAMETERS_KEY = "deliveryParameters";

  public static final String QUERIES_CACHE_NAME = "scheduled queries";

  public static final long QUERY_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(10);

  private static final DateTimeFormatter ISO_8601_DATE_FORMAT =
      DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ").withZoneUTC();

  private static final Security SECURITY = Security.getInstance();

  private final BundleContext bundleContext =
      FrameworkUtil.getBundle(QuerySchedulingPostIngestPlugin.class).getBundleContext();

  private final CatalogFramework catalogFramework;

  private final PersistentStore persistentStore;

  private final WorkspaceTransformer workspaceTransformer;

  public QuerySchedulingPostIngestPlugin(
      CatalogFramework catalogFramework,
      PersistentStore persistentStore,
      WorkspaceTransformer workspaceTransformer) {
    this.catalogFramework = catalogFramework;
    this.persistentStore = persistentStore;
    this.workspaceTransformer = workspaceTransformer;

    // TODO TEMP
    LOGGER.warn("Query scheduling plugin created!");
  }

  private static Fallible<Ignite> getIgnite() {
    if (Ignition.state() != IgniteState.STARTED) {
      return error(
          "An Ignite instance for scheduling and storing jobs is not currently available!");
    }

    return of(Ignition.ignite());
  }

  private static Fallible<IgniteCache<String, String>> getRunningQueriesCache(boolean create) {
    return getIgnite()
        .tryMap(
            ignite -> {
              try {
                if (create) {
                  return of(ignite.getOrCreateCache(QUERIES_CACHE_NAME));
                } else {
                  return ofNullable(
                      ignite.cache(QUERIES_CACHE_NAME),
                      "A cache does not currently exist for scheduled query data!");
                }
              } catch (CacheException exception) {
                return error(
                    "There was a problem attempting to retrieve a cache for running query data: %s",
                    exception.getMessage());
              }
            });
  }

  private static Fallible<IgniteScheduler> getScheduler() {
    return getIgnite().map(Ignite::scheduler);
  }

  private static final class QueryDeliveryExecutor implements Runnable {
    private final QuerySchedulingPostIngestPlugin plugin;

    private final String queryMetacardID;

    private final String queryMetacardTitle;

    private final String cqlQuery;

    private final Collection<String> scheduleDeliveryIDs;

    private final String scheduleUserID;

    private final int scheduleInterval;

    private final DateTime start;

    private final DateTime end;

    private int unitsPassedSinceStarted;

    private @Nullable SchedulerFuture<?> job = null;

    QueryDeliveryExecutor(
        QuerySchedulingPostIngestPlugin plugin,
        String queryMetacardID,
        String queryMetacardTitle,
        String cqlQuery,
        Collection<String> scheduleDeliveryIDs,
        String scheduleUserID,
        int scheduleInterval,
        DateTime start,
        DateTime end) {
      this.plugin = plugin;
      this.queryMetacardID = queryMetacardID;
      this.queryMetacardTitle = queryMetacardTitle;
      this.cqlQuery = cqlQuery;
      this.scheduleDeliveryIDs = scheduleDeliveryIDs;
      this.scheduleUserID = scheduleUserID;
      this.scheduleInterval = scheduleInterval;
      this.start = start;
      this.end = end;

      // Set this >= scheduleInterval - 1 so that a scheduled query executes the first
      // time it is able.
      unitsPassedSinceStarted = scheduleInterval;
    }

    private Fallible<QueryResponse> runQuery(final Subject subject, final String cqlQuery) {
      Filter filter;
      try {
        filter = ECQL.toFilter(cqlQuery);
      } catch (CQLException exception) {
        return error(
            "There was a problem reading the given query expression: " + exception.getMessage());
      }

      final Query query =
          new QueryImpl(
              filter, 1, Constants.DEFAULT_PAGE_SIZE, SortBy.NATURAL_ORDER, true, QUERY_TIMEOUT_MS);
      final QueryRequest queryRequest = new QueryRequestImpl(query, true);

      return subject.execute(
          () -> {
            try {
              return of(plugin.getCatalogFramework().query(queryRequest));
            } catch (UnsupportedQueryException exception) {
              return error(
                  "The query \"%s\" is not supported by the given catalog framework: %s",
                  cqlQuery, exception.getMessage());
            } catch (SourceUnavailableException exception) {
              return error(
                  "The catalog framework sources were unavailable: %s", exception.getMessage());
            } catch (FederationException exception) {
              return error(
                  "There was a problem with executing a federated search for the query \"%s\": %s",
                  cqlQuery, exception.getMessage());
            }
          });
    }

    private void deliver(
        final String deliveryType,
        final String queryMetacardTitle,
        final QueryResponse results,
        final String userID,
        final String deliveryID,
        final Map<String, Object> deliveryParameters,
        final Consumer<String> err) {
      final String filter = String.format("(objectClass=%s)", QueryCourier.class.getName());

      final Stream<QueryCourier> deliveryServices;
      try {
        deliveryServices =
            plugin
                .getBundleContext()
                .getServiceReferences(QueryCourier.class, filter)
                .stream()
                .map(plugin.getBundleContext()::getService)
                .filter(Objects::nonNull);
      } catch (InvalidSyntaxException exception) {
        err.accept(
            String.format(
                "The filter used to search for query delivery services, \"%s\", was invalid: %s",
                filter, exception.getMessage()));
        return;
      }

      final List<QueryCourier> selectedServices =
          deliveryServices
              .filter(deliveryService -> deliveryService.getDeliveryType().equals(deliveryType))
              .collect(Collectors.toList());

      if (selectedServices.isEmpty()) {
        err.accept(
            String.format(
                "The delivery method \"%s\" was not recognized; this query scheduling system found the following delivery methods: %s.",
                deliveryType,
                deliveryServices.map(QueryCourier::getDeliveryType).collect(Collectors.toList())));
        return;
      } else if (selectedServices.size() > 1) {
        final String selectedServicesString =
            selectedServices
                .stream()
                .map(selectedService -> selectedService.getClass().getCanonicalName())
                .collect(Collectors.joining(", "));
        err.accept(
            String.format(
                "%d delivery services were found to handle the delivery type %s: %s.",
                selectedServices.size(), deliveryType, selectedServicesString));
        return;
      }

      selectedServices
          .get(0)
          .deliver(queryMetacardTitle, results, userID, deliveryID, deliveryParameters, err);
    }

    private Fallible<Map<String, Object>> getUserPreferences(final String userID) {
      List<Map<String, Object>> preferencesList;
      try {
        preferencesList =
            plugin
                .getPersistentStore()
                .get(
                    PersistenceType.PREFERENCES_TYPE.toString(),
                    String.format("user = '%s'", userID));
      } catch (PersistenceException exception) {
        return error(
            "There was a problem attempting to retrieve the preferences for user '%s': %s",
            userID, exception.getMessage());
      }
      if (preferencesList.size() != 1) {
        return error(
            "There were %d preference entries found for user '%s'!",
            preferencesList.size(), userID);
      }
      final Map<String, Object> preferencesItem = preferencesList.get(0);

      return MapUtils.tryGet(preferencesItem, "preferences_json_bin", byte[].class)
          .tryMap(
              json -> {
                final Map<String, Object> preferences;
                try {
                  preferences =
                      JsonFactory.create()
                          .parser()
                          .parseMap(new String(json, Charset.defaultCharset()));
                } catch (JsonException exception) {
                  return error(
                      "There was an error parsing the preferences for user '%s': %s",
                      userID, exception.getMessage());
                }

                return of(preferences);
              });
    }

    private Fallible<Pair<String, Map<String, Object>>> getDeliveryInfo(
        final Map<String, Object> userPreferences, final String deliveryID) {
      return MapUtils.tryGet(userPreferences, DELIVERY_METHODS_KEY, List.class)
          .tryMap(
              userDeliveryMethods -> {
                final List<Map<String, Object>> matchingDestinations =
                    ((List<Map<String, Object>>) userDeliveryMethods)
                        .stream()
                        .filter(
                            destination ->
                                MapUtils.tryGet(destination, DELIVERY_METHOD_ID_KEY, String.class)
                                    .map(deliveryID::equals)
                                    .orDo(
                                        error -> {
                                          LOGGER.error(
                                              String.format(
                                                  "There was a problem attempting to retrieve the ID for a destination in the given preferences: %s",
                                                  error));
                                          return false;
                                        }))
                        .collect(Collectors.toList());
                if (matchingDestinations.size() != 1) {
                  return error(
                      "There were %d destinations matching the ID \"%s\" in the given preferences; 1 is expected!",
                      matchingDestinations.size(), deliveryID);
                }
                final Map<String, Object> destinationData = matchingDestinations.get(0);

                return MapUtils.tryGetAndRun(
                    destinationData,
                    QueryCourier.DELIVERY_TYPE_KEY,
                    String.class,
                    DELIVERY_PARAMETERS_KEY,
                    Map.class,
                    (deliveryType, deliveryOptions) ->
                        of(ImmutablePair.of(deliveryType, (Map<String, Object>) deliveryOptions)));
              });
    }

    private void deliverAll(
        final Collection<String> scheduleDeliveryIDs,
        final String scheduleUserID,
        final String queryMetacardTitle,
        final QueryResponse results,
        final Consumer<String> err) {
      getUserPreferences(scheduleUserID)
          .ifValue(
              userPreferences ->
                  forEach(
                      scheduleDeliveryIDs,
                      deliveryID ->
                          getDeliveryInfo(userPreferences, deliveryID)
                              .prependToError(
                                  "There was a problem retrieving the delivery information with ID \"%s\" for user '%s': ",
                                  deliveryID, scheduleUserID)
                              .ifValue(
                                  deliveryInfo ->
                                      deliver(
                                          deliveryInfo.getLeft(),
                                          queryMetacardTitle,
                                          results,
                                          scheduleUserID,
                                          deliveryID,
                                          deliveryInfo.getRight(),
                                          error ->
                                              err.accept(
                                                  String.format(
                                                      "There was a problem delivering query results to delivery info with ID \"%s\" for user '%s': %s",
                                                      deliveryID, scheduleUserID, error))))));
    }

    private void cancel() {
      getRunningQueriesCache(false)
          .ifValue(
              runningQueries -> {
                if (runningQueries.containsKey(queryMetacardID)
                    && (job == null || runningQueries.get(queryMetacardID).equals(job.id()))) {
                  runningQueries.remove(queryMetacardID);
                }
              })
          .elseDo(
              error ->
                  LOGGER.warn(
                      String.format(
                          "While cancelling a completed query scheduling job \"%s\" for query metacard \"%s\", the running queries cache could not be found to remove the job!",
                          job == null ? "null" : job.id(), queryMetacardID)));
      if (job != null) {
        job.cancel();
      }
    }

    @Override
    public void run() {
      // TODO TEMP
      LOGGER.warn(
          String.format("Acquiring and delivering metacard data for %s...", queryMetacardID));

      // Jobs can be cancelled before their end by removing their (queryMetacardId,
      // jobID) key-value pair from the cache.
      final boolean isCancelled =
          getRunningQueriesCache(false)
              .map(
                  runningQueries ->
                      !runningQueries.containsKey(queryMetacardID)
                          // If jobID is null, then the job is being run very immediately after
                          // creation, so assume that it is not cancelled.
                          || job != null && !runningQueries.get(queryMetacardID).equals(job.id()))
              .orDo(
                  error -> {
                    LOGGER.warn(
                        String.format(
                            "Running query data could not be found when the query via metacard \"%s\" ran: %s\nForcing assumption that this scheduled query was not cancelled by the user...",
                            queryMetacardID, error));
                    return false;
                  });
      final DateTime now = DateTime.now();
      if (job != null && (isCancelled || end.compareTo(now) < 0)) {
        cancel();
        return;
      }

      if (start.compareTo(now) <= 0) {
        if (unitsPassedSinceStarted < scheduleInterval - 1) {
          unitsPassedSinceStarted++;
          return;
        }

        unitsPassedSinceStarted = 0;

        // TODO TEMP
        LOGGER.warn(String.format("Running query for query metacard %s...", queryMetacardID));

        runQuery(SECURITY.getGuestSubject("0.0.0.0"), cqlQuery)
            .ifValue(
                results ->
                    deliverAll(
                        scheduleDeliveryIDs,
                        scheduleUserID,
                        queryMetacardTitle,
                        results,
                        LOGGER::error));

        if (job.nextExecutionTime() == 0
            || end.compareTo(new DateTime(job.nextExecutionTime(), DateTimeZone.UTC)) >= 0) {
          cancel();
        }
      }
    }

    // This method is intended to be called immediately after the creation of the job that will run
    // an instance of this class with the SchedulerFuture returned from the IgniteScheduler.
    // This setter must exist in order to allow the ID of the job running this Runnable to be
    // retrieved here for the future cannot be retrieved until the job is created and the job
    // cannot be created without first creating this, creating a circular data dependency.
    void setJob(@Nonnull SchedulerFuture<?> job) {
      this.job = job;
    }
  }

  private Fallible<?> schedule(
      final IgniteScheduler scheduler,
      final String queryMetacardTitle,
      final String queryMetacardID,
      final String cqlQuery,
      final String scheduleUserID,
      final int scheduleInterval,
      final String scheduleUnit,
      final String scheduleStartString,
      final String scheduleEndString,
      final List<String> scheduleDeliveryIDs) {
    if (scheduleInterval <= 0) {
      return error("A task cannot be executed every %d %s!", scheduleInterval, scheduleUnit);
    }

    final DateTime end;
    try {
      end = DateTime.parse(scheduleEndString, ISO_8601_DATE_FORMAT);
    } catch (DateTimeParseException exception) {
      return error(
          "The end date attribute of this metacard, \"%s\", could not be parsed: %s",
          scheduleStartString, exception.getMessage());
    }

    final DateTime start;
    try {
      start = DateTime.parse(scheduleStartString, ISO_8601_DATE_FORMAT);
    } catch (DateTimeParseException exception) {
      return error(
          "The start date attribute of this metacard, \"%s\", could not be parsed: %s",
          scheduleStartString, exception.getMessage());
    }

    final RepetitionTimeUnit unit;
    try {
      unit = RepetitionTimeUnit.valueOf(scheduleUnit.toUpperCase());
    } catch (IllegalArgumentException exception) {
      return error(
          "The unit of time \"%s\" for the scheduled query time interval is not recognized!",
          scheduleUnit);
    }

    final QueryDeliveryExecutor deliveryExecutor =
        new QueryDeliveryExecutor(
            this,
            queryMetacardID,
            queryMetacardTitle,
            cqlQuery,
            scheduleDeliveryIDs,
            scheduleUserID,
            scheduleInterval,
            start,
            end);
    final SchedulerFuture<?> job;
    synchronized (this) {
      try {
        job = scheduler.scheduleLocal(deliveryExecutor, unit.makeCronToRunEachUnit(start));
      } catch (IgniteException exception) {
        return error(
            "There was a problem attempting to schedule a job for a query metacard \"%s\": %s",
            queryMetacardID, exception.getMessage());
      }
      deliveryExecutor.setJob(job);
      getRunningQueriesCache(true)
          .ifValue(runningQueries -> runningQueries.put(queryMetacardID, job.id()));
    }

    return success();
  }

  private Fallible<?> readScheduleDataAndSchedule(
      final IgniteScheduler scheduler,
      final String queryMetacardTitle,
      final String queryMetacardId,
      final String cqlQuery,
      final Map<String, Object> scheduleData) {
    return MapUtils.tryGet(scheduleData, ScheduleMetacardTypeImpl.IS_SCHEDULED, Boolean.class)
        .tryMap(
            isScheduled -> {
              if (!isScheduled) {
                return success().mapValue(null);
              }

              return MapUtils.tryGetAndRun(
                  scheduleData,
                  ScheduleMetacardTypeImpl.SCHEDULE_USER_ID,
                  String.class,
                  ScheduleMetacardTypeImpl.SCHEDULE_AMOUNT,
                  Integer.class,
                  ScheduleMetacardTypeImpl.SCHEDULE_UNIT,
                  String.class,
                  ScheduleMetacardTypeImpl.SCHEDULE_START,
                  String.class,
                  ScheduleMetacardTypeImpl.SCHEDULE_END,
                  String.class,
                  ScheduleMetacardTypeImpl.SCHEDULE_DELIVERY_IDS,
                  List.class,
                  (scheduleUserID,
                      scheduleInterval,
                      scheduleUnit,
                      scheduleStartString,
                      scheduleEndString,
                      scheduleDeliveries) ->
                      schedule(
                          scheduler,
                          queryMetacardTitle,
                          queryMetacardId,
                          cqlQuery,
                          scheduleUserID,
                          scheduleInterval,
                          scheduleUnit,
                          scheduleStartString,
                          scheduleEndString,
                          (List<String>) scheduleDeliveries));
            });
  }

  private Fallible<?> readQueryMetacardAndSchedule(final Map<String, Object> queryMetacardData) {
    return getRunningQueriesCache(true)
        .tryMap(
            runningQueries ->
                getScheduler()
                    .tryMap(
                        scheduler ->
                            MapUtils.tryGetAndRun(
                                queryMetacardData,
                                Metacard.ID,
                                String.class,
                                Metacard.TITLE,
                                String.class,
                                (queryMetacardID, queryMetacardTitle) -> {
                                  if (runningQueries.containsKey(queryMetacardID)) {
                                    return error(
                                        "This query cannot be scheduled because a job is already scheduled for it!");
                                  }

                                  return MapUtils.tryGetAndRun(
                                      queryMetacardData,
                                      QueryMetacardTypeImpl.QUERY_CQL,
                                      String.class,
                                      QueryMetacardTypeImpl.QUERY_SCHEDULES,
                                      List.class,
                                      (cqlQuery, schedulesData) ->
                                          forEach(
                                              (List<Map<String, Object>>) schedulesData,
                                              scheduleData ->
                                                  readScheduleDataAndSchedule(
                                                      scheduler,
                                                      queryMetacardTitle,
                                                      queryMetacardID,
                                                      cqlQuery,
                                                      scheduleData)));
                                })));
  }

  private static Fallible<?> cancelSchedule(final String queryMetacardId) {
    return getRunningQueriesCache(false)
        .tryMap(
            runningQueries -> {
              try {
                runningQueries.remove(queryMetacardId);
              } catch (TransactionException exception) {
                return error(
                    "There was a problem attempting to cancel a job for the query metacard \"%s\": %s",
                    queryMetacardId, exception.getMessage());
              }
              return success();
            });
  }

  private Fallible<?> readQueryMetacardAndCancelSchedule(
      final Map<String, Object> queryMetacardData) {
    return MapUtils.tryGet(queryMetacardData, Metacard.ID, String.class)
        .tryMap(QuerySchedulingPostIngestPlugin::cancelSchedule);
  }

  private Fallible<?> processMetacard(
      Metacard workspaceMetacard, Function<Map<String, Object>, Fallible<?>> metacardAction) {
    if (!WorkspaceMetacardImpl.isWorkspaceMetacard(workspaceMetacard)) {
      return success();
    }

    final Map<String, Object> workspaceMetacardData =
        workspaceTransformer.transform(workspaceMetacard);

    if (!workspaceMetacardData.containsKey(WorkspaceAttributes.WORKSPACE_QUERIES)) {
      return success();
    }

    return MapUtils.tryGet(workspaceMetacardData, WorkspaceAttributes.WORKSPACE_QUERIES, List.class)
        .tryMap(
            queryMetacardsData ->
                forEach(
                    (List<Map<String, Object>>) queryMetacardsData,
                    queryMetacardData -> {
                      if (!queryMetacardData.containsKey(QueryMetacardTypeImpl.QUERY_SCHEDULES)) {
                        return success();
                      }

                      return metacardAction.apply(queryMetacardData);
                    }));
  }

  @Override
  public CreateResponse process(CreateResponse creation) throws PluginExecutionException {
    // TODO TEMP
    LOGGER.warn("Processing creation...");

    forEach(
            creation.getCreatedMetacards(),
            newMetacard ->
                processMetacard(newMetacard, this::readQueryMetacardAndSchedule)
                    .prependToError(
                        "There was an error attempting to schedule delivery job(s) for the new workspace metacard \"%s\": ",
                        newMetacard.getId()))
        .elseThrow(PluginExecutionException::new);

    return creation;
  }

  @Override
  public UpdateResponse process(UpdateResponse updates) throws PluginExecutionException {
    // TODO TEMP
    LOGGER.warn("Processing update...");

    forEach(
            updates.getUpdatedMetacards(),
            update ->
                processMetacard(update.getOldMetacard(), this::readQueryMetacardAndCancelSchedule)
                    .prependToError(
                        "There was an error attempting to cancel the scheduled delivery job(s) for the pre-update version of workspace metacard \"%s\": ",
                        update.getOldMetacard().getId()),
            update ->
                processMetacard(update.getNewMetacard(), this::readQueryMetacardAndSchedule)
                    .prependToError(
                        "There was an error attempting to schedule delivery job(s) for the post-update version of workspace metacard \"%s\": ",
                        update.getNewMetacard().getId()))
        .elseThrow(PluginExecutionException::new);

    return updates;
  }

  @Override
  public DeleteResponse process(DeleteResponse deletion) throws PluginExecutionException {
    // TODO TEMP
    LOGGER.warn("Processing deletion...");

    forEach(
            deletion.getDeletedMetacards(),
            deletedMetacard ->
                processMetacard(deletedMetacard, this::readQueryMetacardAndCancelSchedule)
                    .prependToError(
                        "There was an error attempting to cancel the scheduled delivery job(s) for the deleted workspace metacard \"%s\": ",
                        deletedMetacard.getId()))
        .elseThrow(PluginExecutionException::new);

    return deletion;
  }

  public BundleContext getBundleContext() {
    return bundleContext;
  }

  public CatalogFramework getCatalogFramework() {
    return catalogFramework;
  }

  public PersistentStore getPersistentStore() {
    return persistentStore;
  }
}
