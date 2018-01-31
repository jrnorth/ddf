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
import static ddf.util.Fallible.success;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import ddf.catalog.CatalogFramework;
import ddf.catalog.Constants;
import ddf.catalog.data.Metacard;
import ddf.catalog.federation.FederationException;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.Response;
import ddf.catalog.operation.Update;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PostIngestPlugin;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.util.Fallible;
import ddf.util.MapUtils;
import java.nio.charset.Charset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
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
import org.boon.json.JsonFactory;
import org.codice.ddf.catalog.ui.metacard.workspace.QueryMetacardTypeImpl;
import org.codice.ddf.catalog.ui.metacard.workspace.WorkspaceAttributes;
import org.codice.ddf.catalog.ui.metacard.workspace.WorkspaceMetacardImpl;
import org.codice.ddf.catalog.ui.metacard.workspace.WorkspaceTransformer;
import org.codice.ddf.catalog.ui.scheduling.subscribers.EmailDeliveryService;
import org.codice.ddf.catalog.ui.scheduling.subscribers.QueryDeliveryService;
import org.codice.ddf.persistence.PersistenceException;
import org.codice.ddf.persistence.PersistentStore;
import org.codice.ddf.persistence.PersistentStore.PersistenceType;
import org.codice.ddf.security.common.Security;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuerySchedulingPostIngestPlugin implements PostIngestPlugin {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(QuerySchedulingPostIngestPlugin.class);

  public static final String DELIVERY_METHODS_KEY = "deliveryMethods";

  public static final String DELIVERY_METHOD_ID_KEY = "deliveryId";

  public static final String DELIVERY_OPTIONS_KEY = "deliveryOptions";

  public static final String QUERIES_CACHE_NAME = "scheduled queries";

  public static final long QUERY_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(10);

  private static final DateTimeFormatter ISO_8601_DATE_FORMAT =
      DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ").withZoneUTC();

  private static final Security SECURITY = Security.getInstance();

  private static Fallible<IgniteScheduler> scheduler =
      error(
          "An Ignite scheduler has not been obtained for this query! Have any queries been started yet?");

  /**
   * This {@link IgniteCache} relates metacards to running {@link Ignite} scheduled jobs. Keys are
   * metacard IDs (unique identifiers of metacards) while values are running {@link Ignite} jobs.
   * This {@link IgniteCache} will become available as soon as a job is scheduled if a running
   * {@link Ignite} instance is available.
   */
  @VisibleForTesting
  static Fallible<IgniteCache<String, Integer>> runningQueries =
      error(
          "An Ignite cache has not been obtained for this query! Have any queries been started yet?");

  private final CatalogFramework catalogFramework;

  private final PersistentStore persistentStore;

  private final WorkspaceTransformer workspaceTransformer;

  private final ImmutableMap<String, QueryDeliveryService> deliveryServicesByName;

  public QuerySchedulingPostIngestPlugin(
      CatalogFramework catalogFramework,
      EmailDeliveryService emailNotifierService,
      PersistentStore persistentStore,
      WorkspaceTransformer workspaceTransformer) {
    this.catalogFramework = catalogFramework;
    this.persistentStore = persistentStore;
    this.workspaceTransformer = workspaceTransformer;

    // TODO: replace this with a dynamic OSGi solution that Josh sent to grab all running
    // QueryDeliveryServices
    deliveryServicesByName = ImmutableMap.of("email", emailNotifierService);

    // TODO TEMP
    LOGGER.warn("Query scheduling plugin created!");
  }

  // TODO: refactor this method so that we can get user preferences once, not for every delivery ID
  private Fallible<Pair<String, Map<String, Object>>> getDeliveryInfo(
      final String username, final String deliveryID) {
    List<Map<String, Object>> preferencesList;
    try {
      preferencesList =
          persistentStore.get(
              PersistenceType.PREFERENCES_TYPE.toString(), String.format("user = '%s'", username));
    } catch (PersistenceException exception) {
      return error(
          "There was a problem attempting to retrieve the preferences for user '%s': %s",
          username, exception.getMessage());
    }
    if (preferencesList.size() != 1) {
      return error(
          "There were %d preference entries found for user '%s'!",
          preferencesList.size(), username);
    }
    final Map<String, Object> preferencesItem = preferencesList.get(0);

    return MapUtils.tryGet(preferencesItem, "preferences_json_bin", byte[].class)
        .tryMap(
            json -> {
              final Map<String, Object> preferences =
                  JsonFactory.create()
                      .parser()
                      .parseMap(new String(json, Charset.defaultCharset()));

              return MapUtils.tryGet(preferences, DELIVERY_METHODS_KEY, List.class)
                  .tryMap(
                      usersDestinations -> {
                        final List<Map<String, Object>> matchingDestinations =
                            ((List<Map<String, Object>>) usersDestinations)
                                .stream()
                                .filter(
                                    destination ->
                                        MapUtils.tryGet(
                                                destination, DELIVERY_METHOD_ID_KEY, String.class)
                                            .map(deliveryID::equals)
                                            .orDo(
                                                error -> {
                                                  LOGGER.error(
                                                      "There was a problem attempting to retrieve the ID for a destination in the preferences for user '%s': %s",
                                                      username, error);
                                                  return false;
                                                }))
                                .collect(Collectors.toList());
                        if (matchingDestinations.size() != 1) {
                          return error(
                              "There were %d destinations matching the ID \"%s\" for user '%s'; only one is suspected!",
                              matchingDestinations.size(), deliveryID, username);
                        }
                        final Map<String, Object> destinationData = matchingDestinations.get(0);

                        return MapUtils.tryGetAndRun(
                            destinationData,
                            QueryDeliveryService.DELIVERY_TYPE_KEY,
                            String.class,
                            DELIVERY_OPTIONS_KEY,
                            Map.class,
                            (deliveryType, deliveryOptions) ->
                                of(
                                    new ImmutablePair<>(
                                        deliveryType, (Map<String, Object>) deliveryOptions)));
                      });
            });
  }

  private Fallible<QueryResponse> runQuery(final String cqlQuery) {
    // TODO TEMP
    LOGGER.warn("Emailing metacard owner...");

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

    return SECURITY
        .runAsAdmin(SECURITY::getSystemSubject)
        .execute(
            () -> {
              try {
                return of(catalogFramework.query(queryRequest));
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

  private Fallible<?> deliver(
      final Map<String, Object> queryMetacardData,
      final QueryResponse results,
      final String deliveryType,
      final Map<String, Object> deliveryParameters) {
    if (!deliveryServicesByName.containsKey(deliveryType)) {
      return error(
          "The delivery method \"%s\" was not recognized; this query scheduling system recognized the following delivery methods: %s.",
          deliveryType, deliveryServicesByName.keySet());
    }

    return deliveryServicesByName
        .get(deliveryType)
        .deliver(queryMetacardData, results, deliveryParameters);
  }

  private Fallible<?> deliverAll(
      final Collection<String> scheduleDeliveryIDs,
      final String scheduleUsername,
      final Map<String, Object> queryMetacardData,
      final QueryResponse results) {
    return forEach(
        scheduleDeliveryIDs,
        deliveryID ->
            getDeliveryInfo(scheduleUsername, deliveryID)
                .prependToError(
                    "There was a problem retrieving the delivery information with ID \"%s\" for user '%s': ",
                    deliveryID, scheduleUsername)
                .tryMap(
                    deliveryInfo ->
                        deliver(
                                queryMetacardData,
                                results,
                                deliveryInfo.getLeft(),
                                deliveryInfo.getRight())
                            .prependToError(
                                "There was a problem delivering query results to delivery info with ID \"%s\" for user '%s': ",
                                deliveryID, scheduleUsername)));
  }

  private Fallible<SchedulerFuture<?>> scheduleJob(
      final IgniteScheduler scheduler,
      final Map<String, Object> queryMetacardData,
      final String queryMetacardId,
      final String cqlQuery,
      final String scheduleUsername,
      final int scheduleInterval,
      final String scheduleUnit,
      final String scheduleStartString,
      final String scheduleEndString,
      final List<String> scheduleDeliveryIDs) {
    if (scheduleInterval <= 0) {
      return error("A task cannot be executed every %d %s!", scheduleInterval, scheduleUnit);
    }

    DateTime start;
    DateTime end;
    try {
      start = DateTime.parse(scheduleStartString, ISO_8601_DATE_FORMAT);
    } catch (DateTimeParseException exception) {
      return error(
          "The start date attribute of this metacard, \"%s\", could not be parsed: %s",
          scheduleStartString, exception.getMessage());
    }
    try {
      end = DateTime.parse(scheduleEndString, ISO_8601_DATE_FORMAT);
    } catch (DateTimeParseException exception) {
      return error(
          "The end date attribute of this metacard, \"%s\", could not be parsed: %s",
          scheduleStartString, exception.getMessage());
    }

    RepetitionTimeUnit unit;
    try {
      unit = RepetitionTimeUnit.valueOf(scheduleUnit.toUpperCase());
    } catch (IllegalArgumentException exception) {
      return error(
          "The unit of time \"%s\" for the scheduled query time interval is not recognized!",
          scheduleUnit);
    }

    final SchedulerFuture<?> job;
    try {
      job =
          scheduler.scheduleLocal(
              new Runnable() {
                // Set this >= scheduleInterval - 1 so that a scheduled query executes the first
                // time it is able
                private int unitsPassedSinceStarted = scheduleInterval;

                @Override
                public void run() {
                  final boolean isRunning =
                      runningQueries
                          .map(runningQueries -> runningQueries.containsKey(queryMetacardId))
                          .or(true);
                  if (!isRunning) {
                    return;
                  }

                  final DateTime now = DateTime.now();
                  if (start.compareTo(now) <= 0) {
                    if (unitsPassedSinceStarted < scheduleInterval - 1) {
                      unitsPassedSinceStarted++;
                      return;
                    }

                    unitsPassedSinceStarted = 0;

                    runQuery(cqlQuery)
                        .tryMap(
                            results ->
                                deliverAll(
                                    scheduleDeliveryIDs,
                                    scheduleUsername,
                                    queryMetacardData,
                                    results))
                        .elseDo(LOGGER::error);
                  }
                }
              },
              unit.makeCronToRunEachUnit(start));
    } catch (IgniteException exception) {
      return error(
          "There was a problem attempting to schedule a job for a query metacard \"%s\": %s",
          queryMetacardId, exception.getMessage());
    }

    runningQueries.ifValue(runningQueries -> runningQueries.put(queryMetacardId, 0));

    job.listen(
        future ->
            runningQueries.ifValue(
                runningQueries -> {
                  if (future instanceof SchedulerFuture) {
                    final SchedulerFuture<?> jobFuture = (SchedulerFuture<?>) future;
                    if (jobFuture.nextExecutionTime() == 0
                        || jobFuture.nextExecutionTime() > end.getMillis()
                        || !runningQueries.containsKey(queryMetacardId)) {
                      runningQueries.remove(queryMetacardId);
                      jobFuture.cancel();
                    }
                  }
                }));

    return of(job);
  }

  private Fallible<?> readScheduleDataAndSchedule(
      final IgniteScheduler scheduler,
      final Map<String, Object> queryMetacardData,
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
                  ScheduleMetacardTypeImpl.SCHEDULE_USERNAME,
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
                  (scheduleUsername,
                      scheduleInterval,
                      scheduleUnit,
                      scheduleStartString,
                      scheduleEndString,
                      scheduleDeliveries) ->
                      scheduleJob(
                          scheduler,
                          queryMetacardData,
                          queryMetacardId,
                          cqlQuery,
                          scheduleUsername,
                          scheduleInterval,
                          scheduleUnit,
                          scheduleStartString,
                          scheduleEndString,
                          (List<String>) scheduleDeliveries));
            });
  }

  private Fallible<?> readQueryMetacardAndSchedule(final Map<String, Object> queryMetacardData) {
    if (Ignition.state() != IgniteState.STARTED) {
      return error("Cron queries cannot be scheduled without a running Ignite instance!");
    }

    final Ignite ignite = Ignition.ignite();

    final IgniteScheduler scheduler =
        QuerySchedulingPostIngestPlugin.scheduler.orDo(
            error -> {
              final IgniteScheduler newScheduler = ignite.scheduler();
              QuerySchedulingPostIngestPlugin.scheduler = of(newScheduler);
              return newScheduler;
            });

    final IgniteCache<String, ?> runningQueries =
        QuerySchedulingPostIngestPlugin.runningQueries.orDo(
            error -> {
              final IgniteCache<String, Integer> newCache =
                  ignite.getOrCreateCache(QUERIES_CACHE_NAME);
              QuerySchedulingPostIngestPlugin.runningQueries = of(newCache);
              return newCache;
            });

    return MapUtils.tryGetAndRun(
        queryMetacardData,
        Metacard.ID,
        String.class,
        queryMetacardId -> {
          if (runningQueries.containsKey(queryMetacardId)) {
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
                              queryMetacardData,
                              queryMetacardId,
                              cqlQuery,
                              scheduleData)));
        });
  }

  private static Fallible<?> cancelSchedule(final String queryMetacardId) {
    return runningQueries.tryMap(
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

  private static void throwErrorsIfAny(List<ImmutablePair<Metacard, String>> errors)
      throws PluginExecutionException {
    if (!errors.isEmpty()) {
      throw new PluginExecutionException(
          errors
              .stream()
              .map(
                  metacardAndError ->
                      String.format(
                          "There was an error attempting to modify schedule execution of workspace metacard \"%s\": %s",
                          metacardAndError.getLeft().getId(), metacardAndError.getRight()))
              .collect(Collectors.joining("\n")));
    }
  }

  private <T extends Response> T processSingularResponse(
      T response,
      List<Metacard> metacards,
      Function<Map<String, Object>, Fallible<?>> metacardAction)
      throws PluginExecutionException {
    List<ImmutablePair<Metacard, String>> errors = new ArrayList<>();

    for (Metacard metacard : metacards) {
      // TODO TEMP
      LOGGER.debug(
          String.format("Processing metacard of type %s...", metacard.getMetacardType().getName()));
      processMetacard(metacard, metacardAction)
          .elseDo(error -> errors.add(ImmutablePair.of(metacard, error)));
    }

    throwErrorsIfAny(errors);

    return response;
  }

  @Override
  public CreateResponse process(CreateResponse creation) throws PluginExecutionException {
    // TODO TEMP
    LOGGER.warn("Processing creation...");
    return processSingularResponse(
        creation, creation.getCreatedMetacards(), this::readQueryMetacardAndSchedule);
  }

  @Override
  public UpdateResponse process(UpdateResponse updates) throws PluginExecutionException {
    // TODO TEMP
    LOGGER.warn("Processing update...");
    List<ImmutablePair<Metacard, String>> errors = new ArrayList<>();

    for (Update update : updates.getUpdatedMetacards()) {
      // TODO TEMP
      LOGGER.warn(
          String.format(
              "Processing old metacard of type %s...",
              update.getOldMetacard().getMetacardType().getName()));
      processMetacard(update.getOldMetacard(), this::readQueryMetacardAndCancelSchedule)
          .elseDo(error -> errors.add(ImmutablePair.of(update.getOldMetacard(), error)));
      // TODO TEMP
      LOGGER.warn(
          String.format(
              "Processing new metacard of type %s...",
              update.getNewMetacard().getMetacardType().getName()));
      processMetacard(update.getNewMetacard(), this::readQueryMetacardAndSchedule)
          .elseDo(error -> errors.add(ImmutablePair.of(update.getNewMetacard(), error)));
    }

    throwErrorsIfAny(errors);

    return updates;
  }

  @Override
  public DeleteResponse process(DeleteResponse deletion) throws PluginExecutionException {
    // TODO TEMP
    LOGGER.warn("Processing deletion...");
    return processSingularResponse(
        deletion, deletion.getDeletedMetacards(), this::readQueryMetacardAndCancelSchedule);
  }
}
