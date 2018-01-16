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
package org.codice.ddf.catalog.ui.security.cron;

import static ddf.util.Fallible.*;

import com.google.common.annotations.VisibleForTesting;
import ddf.catalog.Constants;
import ddf.catalog.federation.FederationException;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.security.Subject;
import ddf.security.SubjectUtils;
import ddf.util.Fallible;
import ddf.util.MapUtils;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteScheduler;
import org.apache.ignite.IgniteState;
import org.apache.ignite.Ignition;
import org.apache.ignite.scheduler.SchedulerFuture;
import org.apache.shiro.SecurityUtils;
import org.boon.json.JsonFactory;
import org.boon.json.ObjectMapper;
import org.boon.json.annotations.JsonIgnore;
import org.codice.ddf.catalog.ui.metacard.workspace.WorkspaceMetacardImpl;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CronQuery extends CronQueryJson implements Runnable {
  private static final Logger LOGGER = LoggerFactory.getLogger(CronQuery.class);

  public static final ObjectMapper JSON_MAPPER = JsonFactory.create();

  public static final String QUERIES_CACHE_NAME = "scheduled queries";

  public static final long QUERY_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(10);

  private static Fallible<IgniteScheduler> scheduler =
      error(
          "An Ignite scheduler has not been obtained for this query! Have any queries been started yet?");

  @VisibleForTesting
  static Fallible<IgniteCache<String, Map<String, CronQuery>>> runningQueries =
      error(
          "An Ignite cache has not been obtained for this query! Have any queries been started yet?");

  @JsonIgnore private final String username;

  @JsonIgnore private final CronApplication application;

  @JsonIgnore
  private Fallible<SchedulerFuture<?>> job = error("This query has not been scheduled yet!");

  public CronQuery(CronQueryJson cronQueryJson, CronApplication application) {
    this(
        cronQueryJson.getQuery(),
        cronQueryJson.getCronSchedule(),
        cronQueryJson.getJobID(),
        application);
  }

  public CronQuery(String query, String cronSchedule, String jobID, CronApplication application) {
    this((Subject) SecurityUtils.getSubject(), query, cronSchedule, jobID, application);
  }

  public CronQuery(
      Subject subject,
      String query,
      String cronSchedule,
      String jobID,
      CronApplication application) {
    this(SubjectUtils.getName(subject), query, cronSchedule, jobID, application);
  }

  CronQuery(
      String username,
      String query,
      String cronSchedule,
      String jobID,
      CronApplication application) {
    super(query, cronSchedule, jobID);

    this.application = application;
    this.username = username;
  }

  public Fallible<?> start() {
    return start(null);
  }

  public Fallible<?> start(@Nullable String igniteInstanceName) {
    if (isRunning()) {
      return error("This job cannot be started because it is already running!");
    }

    if (Ignition.state(igniteInstanceName) != IgniteState.STARTED) {
      return error("Cron queries cannot be scheduled without a running Ignite instance!");
    }

    if (scheduler.hasError()) {
      final Ignite ignite = Ignition.ignite(igniteInstanceName);
      scheduler = of(ignite.scheduler());
      runningQueries = of(ignite.getOrCreateCache(QUERIES_CACHE_NAME));
    }
    job = scheduler.map(scheduler -> scheduler.scheduleLocal(this, getCronSchedule()));

    return runningQueries.tryMap(
        queries -> {
          if (queries.containsKey(getUsername())) {
            if (queries.get(getUsername()).containsKey(getJobID())) {
              return error(
                  "There is already a cron query running for user '%s' with the ID \"%s\"!",
                  getUsername(), getJobID());
            } else {
              queries.get(getUsername()).put(getJobID(), this);
            }
          } else {
            Map<String, CronQuery> queriesForUser = new HashMap<>();
            queriesForUser.put(getJobID(), this);
            queries.put(getUsername(), queriesForUser);
          }

          return success();
        });
  }

  public boolean isRunning() {
    return job.map(job -> !job.isCancelled()).or(false);
  }

  public String getUsername() {
    return username;
  }

  protected CronApplication getApplication() {
    return application;
  }

  @VisibleForTesting
  Fallible<SchedulerFuture<?>> getJob() {
    return job;
  }

  public static Fallible<CronQuery> findCronQuery(Subject subject, String jobID) {
    return findCronQuery(SubjectUtils.getName(subject), jobID);
  }

  @VisibleForTesting
  static Fallible<CronQuery> findCronQuery(String username, String jobID) {
    return findCronQueries(username)
        .tryMap(
            queries ->
                ofNullable(
                    queries.get(jobID),
                    "No queries were found for job \"%s\" for user '%s'!",
                    jobID,
                    username));
  }

  private static Fallible<Map<String, CronQuery>> findCronQueries(final String username) {
    return runningQueries.tryMap(
        queries ->
            ofNullable(queries.get(username), "No queries were found for user '%s'!", username));
  }

  public static Collection<CronQuery> getRunningQueriesFor(Subject subject) {
    return findCronQueries(SubjectUtils.getName(subject))
        .map(Map::values)
        .or(Collections.emptySet());
  }

  @Override
  public void run() {
    execute().elseDo(LOGGER::error);
  }

  public Fallible<?> execute() {
    Filter filter;
    try {
      filter = ECQL.toFilter(getQuery());
    } catch (CQLException exception) {
      return error(
          "There was a problem reading the given query expression: " + exception.getMessage());
    }

    final Query query =
        new QueryImpl(
            filter, 1, Constants.DEFAULT_PAGE_SIZE, SortBy.NATURAL_ORDER, true, QUERY_TIMEOUT_MS);
    final QueryRequest queryRequest = new QueryRequestImpl(query, true);

    QueryResponse queryResults;
    try {
      queryResults = getApplication().getCatalogFramework().query(queryRequest);
    } catch (UnsupportedQueryException exception) {
      return error(
          "The query \"%s\" is not supported by the given catalog framework: %s",
          getQuery(), exception.getMessage());
    } catch (SourceUnavailableException exception) {
      return error("The catalog framework sources were unavailable: " + exception.getMessage());
    } catch (FederationException exception) {
      return error(
          "There was a problem with executing a federated search for the query \"%s\": %s",
          getQuery(), exception.getMessage());
    }

    final Map<String, Pair<WorkspaceMetacardImpl, Long>> notifiableQueryResults =
        MapUtils.fromList(
            queryResults.getResults(),
            result -> result.getMetacard().getId(),
            result ->
                Pair.of(WorkspaceMetacardImpl.from(result.getMetacard()), queryResults.getHits()));

    getApplication().getEmailNotifierService().notify(notifiableQueryResults);

    return success();
  }

  public Fallible<?> stop() {
    return job.tryMap(
        job -> {
          if (!job.isCancelled()) {
            job.cancel();
            return runningQueries.tryMap(
                runningQueries -> {
                  final @Nullable Map<String, CronQuery> usersQueries =
                      runningQueries.get(getUsername());
                  if (usersQueries == null || !usersQueries.containsKey(getJobID())) {
                    return error("This %s was not found in the scheduled queries cache!", this);
                  }

                  usersQueries.remove(getJobID());
                  return success();
                });
          } else {
            return error("This %s cannot be cancelled because it is not running!", this);
          }
        });
  }

  @Override
  public String toString() {
    return String.format(
        "job \"%s\" for user \"%s\" running \"%s\" at %s",
        getJobID(), username, getQuery(), getCronSchedule());
  }
}
