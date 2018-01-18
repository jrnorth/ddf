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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.federation.FederationException;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.util.MapUtils;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteScheduler;
import org.apache.ignite.IgniteState;
import org.apache.ignite.Ignition;
import org.apache.ignite.scheduler.SchedulerFuture;
import org.boon.Exceptions.SoftenedException;
import org.boon.json.JsonException;
import org.boon.json.JsonFactory;
import org.boon.json.ObjectMapper;
import org.codice.ddf.catalog.ui.metacard.workspace.WorkspaceMetacardImpl;
import org.codice.ddf.catalog.ui.query.monitor.email.EmailNotifier;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Ignition.class})
public class CronQueryTest {
  private static final String MOCK_QUERY = "1 = 0";

  private static final String MOCK_USERNAME = "tester";

  private static final LocalDateTime IN_ONE_MONTH = LocalDateTime.now().plusMonths(1);

  private static final ObjectMapper JSON_CREATOR = JsonFactory.create();

  private CronApplication mockApplication;

  private Ignite mockIgnite;

  private IgniteCache mockRunningQueriesCache;

  private IgniteScheduler mockScheduler;

  private Collection<SchedulerFuture<?>> cancelledMockJobs;

  private Map<String, Map<String, CronQuery>> mockRunningQueriesCacheContents;

  @Before
  public void setUp() {
    final CatalogFramework mockCatalogFramework = mock(CatalogFramework.class);

    final EmailNotifier mockEmailNotifier = mock(EmailNotifier.class);

    mockApplication = mock(CronApplication.class);
    when(mockApplication.getCatalogFramework()).thenReturn(mockCatalogFramework);
    when(mockApplication.getEmailNotifierService()).thenReturn(mockEmailNotifier);

    mockRunningQueriesCache = mock(IgniteCache.class);
    mockRunningQueriesCacheContents = new HashMap<>();
    when(mockRunningQueriesCache.containsKey(any(String.class)))
        .then(
            invocation ->
                mockRunningQueriesCacheContents.containsKey(
                    invocation.getArgumentAt(0, String.class)));
    doAnswer(
            invocation ->
                mockRunningQueriesCacheContents.put(
                    invocation.getArgumentAt(0, String.class),
                    invocation.getArgumentAt(1, Map.class)))
        .when(mockRunningQueriesCache)
        .put(any(String.class), any(Map.class));
    when(mockRunningQueriesCache.get(any(String.class)))
        .then(
            invocation ->
                mockRunningQueriesCacheContents.get(invocation.getArgumentAt(0, String.class)));

    cancelledMockJobs = new HashSet<>();

    mockScheduler = mock(IgniteScheduler.class);
    when(mockScheduler.scheduleLocal(any(CronQuery.class), any(String.class)))
        .then(
            scheduleInvocation -> {
              final SchedulerFuture<?> mockJob = mock(SchedulerFuture.class);
              when(mockJob.cancel()).then(b -> cancelledMockJobs.add(mockJob));
              when(mockJob.isCancelled()).then(b -> cancelledMockJobs.contains(mockJob));
              return mockJob;
            });

    mockIgnite = mock(Ignite.class);
    when(mockIgnite.scheduler()).thenReturn(mockScheduler);
    when(mockIgnite.getOrCreateCache(CronQuery.QUERIES_CACHE_NAME))
        .thenReturn(mockRunningQueriesCache);

    PowerMockito.mockStatic(Ignition.class);
    BDDMockito.given(Ignition.state(any(String.class))).willReturn(IgniteState.STARTED);
    BDDMockito.given(Ignition.ignite(any(String.class))).willReturn(mockIgnite);
  }

  @After
  public void tearDown() {
    final ImmutableMap<String, ImmutableMap<String, CronQuery>> runningQueriesCopy =
        new ImmutableMap.Builder<String, ImmutableMap<String, CronQuery>>()
            .putAll(MapUtils.mapValues(mockRunningQueriesCacheContents, ImmutableMap::copyOf))
            .build();

    for (Map<String, CronQuery> queriesForUser : runningQueriesCopy.values()) {
      for (CronQuery query : queriesForUser.values()) {
        query
            .stop()
            .elseDo(
                error ->
                    fail(
                        "There was a problem stopping a running cron query during a test: "
                            + error));
      }
    }

    mockRunningQueriesCacheContents.clear();
    cancelledMockJobs.clear();
  }

  private static String toCron(LocalDateTime instant) {
    return Stream.of(
            instant.getMinute(),
            instant.getHour(),
            instant.getDayOfMonth(),
            instant.getMonthValue(),
            "*")
        .map(String::valueOf)
        .collect(Collectors.joining(" "));
  }

  private CronQuery newCronQuery(LocalDateTime querySchedule, String jobID) {
    return new CronQuery(MOCK_USERNAME, MOCK_QUERY, toCron(querySchedule), jobID, mockApplication);
  }

  private CronQuery tryStart(LocalDateTime querySchedule, String jobID) {
    final CronQuery cronQuery = newCronQuery(querySchedule, jobID);

    cronQuery
        .start()
        .elseDo(
            error -> fail("An error occurred while trying to start a test cron query: " + error));

    assertThat(cronQuery.getJob().hasError(), is(false));

    return cronQuery;
  }

  private void tryStop(CronQuery cronQuery) {
    cronQuery
        .stop()
        .elseDo(
            error -> fail("An error occurred while trying to stop a test cron query: " + error));

    assertThat(cronQuery.getJob().hasError(), is(false));
    cronQuery.getJob().ifValue(job -> assertThat(job.isCancelled(), is(true)));
  }

  @Test
  public void testStart() {
    tryStart(IN_ONE_MONTH, "testing start()");
  }

  @Test
  public void testStop() {
    final CronQuery cronQuery = tryStart(IN_ONE_MONTH, "testing stop()");

    tryStop(cronQuery);
  }

  @Test
  public void testStopWithoutStarting() {
    final CronQuery unscheduledQuery = newCronQuery(IN_ONE_MONTH, "testing stop without start");

    unscheduledQuery
        .stop()
        .ifValue(
            value ->
                fail("Stopping a cron query that was never started should have thrown an error!"));
  }

  @Test
  public void testStartingAlreadyStarted() {
    final CronQuery startedQuery = tryStart(IN_ONE_MONTH, "testing starting started");

    startedQuery
        .start()
        .ifValue(
            value ->
                fail(
                    "Starting a cron query that was already started should have thrown an error!"));
  }

  @Test
  public void testFindCronQuery() {
    final CronQuery cronQuery = tryStart(IN_ONE_MONTH, "testing finding running job");

    CronQuery.findCronQuery(cronQuery.getUsername(), cronQuery.getJobID())
        .ifValue(foundCronQuery -> assertThat(cronQuery, equalTo(foundCronQuery)))
        .elseDo(Assert::fail);
  }

  @Test
  public void testJsonSerialization() {
    final String MOCK_JOB_ID = "testing JSON serialization";

    final CronQuery cronQuery = newCronQuery(IN_ONE_MONTH, MOCK_JOB_ID);

    String cronQueryAsJson;
    try {
      cronQueryAsJson = JSON_CREATOR.toJson(cronQuery);
    } catch (SoftenedException exception) {
      fail(
          "There was a problem attempting to serialize a mock cron query: "
              + exception.getMessage());
      return;
    }

    final Map<String, Object> cronQueryJsonAsMap;
    try {
      cronQueryJsonAsMap = JSON_CREATOR.parser().parseMap(cronQueryAsJson);
    } catch (JsonException exception) {
      fail(
          "I expected the JSON form of a CronQuery to be a JSON map, but it could not be parsed as one: "
              + exception.getMessage());
      return;
    }

    assertThat(cronQueryJsonAsMap.get(CronQueryJson.QUERY_JSON_KEY), equalTo(MOCK_QUERY));
    assertThat(
        cronQueryJsonAsMap.get(CronQueryJson.CRON_SCHEDULE_JSON_KEY),
        equalTo(toCron(IN_ONE_MONTH)));
    assertThat(cronQueryJsonAsMap.get(CronQueryJson.JOB_ID_JSON_KEY), equalTo(MOCK_JOB_ID));
  }

  @Test
  public void testJsonDeserialization() {
    final String MOCK_JOB_ID = "testing JSON deserialization";

    final String inOneMonthCron = toCron(IN_ONE_MONTH);

    final String cronQueryJson =
        "{\""
            + CronQueryJson.JOB_ID_JSON_KEY
            + "\": \""
            + MOCK_JOB_ID
            + "\", \""
            + CronQueryJson.CRON_SCHEDULE_JSON_KEY
            + "\": \""
            + inOneMonthCron
            + "\", \""
            + CronQueryJson.QUERY_JSON_KEY
            + "\": \""
            + MOCK_QUERY
            + "\"}";

    CronQueryJson cronQueryFromJson;
    try {
      cronQueryFromJson = JSON_CREATOR.fromJson(cronQueryJson, CronQueryJson.class);
    } catch (JsonException exception) {
      fail(
          "There was an error while trying to deserialize a test cron query from JSON: "
              + exception.getMessage());
      return;
    }

    assertThat(cronQueryFromJson.getQuery(), equalTo(MOCK_QUERY));
    assertThat(cronQueryFromJson.getCronSchedule(), equalTo(inOneMonthCron));
    assertThat(cronQueryFromJson.getJobID(), equalTo(MOCK_JOB_ID));
  }

  @Test
  public void testExecute()
      throws UnsupportedQueryException, SourceUnavailableException, FederationException,
          InterruptedException {
    final String MOCK_METACARD_ID = "MOCK METACARD";
    final String MOCK_SOURCE_ID = "MOCK SOURCE";

    final List<QueryRequest> queryRequestsMade = new ArrayList<>();
    final List<Map<String, Pair<WorkspaceMetacardImpl, Long>>> emailNotificationInfo =
        new ArrayList<>();

    final MetacardImpl metacard = new MetacardImpl(BasicTypes.BASIC_METACARD);
    metacard.setId(MOCK_METACARD_ID);
    metacard.setSourceId(MOCK_SOURCE_ID);
    final Result queryResult = new ResultImpl(metacard);

    final QueryResponse mockQueryResponse = mock(QueryResponse.class);
    when(mockQueryResponse.getResults()).thenReturn(Collections.singletonList(queryResult));
    when(mockQueryResponse.getHits()).thenReturn((long) 1);

    final EmailNotifier mockEmailNotifier = mock(EmailNotifier.class);
    doAnswer(invocation -> emailNotificationInfo.add(invocation.getArgumentAt(0, Map.class)))
        .when(mockEmailNotifier)
        .notify(any(Map.class));

    final CatalogFramework mockCatalogFramework = mock(CatalogFramework.class);
    doAnswer(
            invocation -> {
              queryRequestsMade.add(invocation.getArgumentAt(0, QueryRequest.class));
              return mockQueryResponse;
            })
        .when(mockCatalogFramework)
        .query(any(QueryRequest.class));
    when(mockApplication.getCatalogFramework()).thenReturn(mockCatalogFramework);
    when(mockApplication.getEmailNotifierService()).thenReturn(mockEmailNotifier);

    final CronQuery cronQuery =
        newCronQuery(LocalDateTime.now().plusSeconds(1), "testing query run");
    cronQuery.execute().elseDo(Assert::fail);

    assertThat(queryRequestsMade.size(), is(1));
    assertThat(
        queryRequestsMade.get(0).getQuery().getTimeoutMillis(), is(CronQuery.QUERY_TIMEOUT_MS));
    assertThat(emailNotificationInfo.size(), is(1));
    final Map<String, Pair<WorkspaceMetacardImpl, Long>> emailNotification =
        emailNotificationInfo.get(0);
    assertThat(emailNotification.size(), is(1));
    for (Map.Entry<String, Pair<WorkspaceMetacardImpl, Long>> entry :
        emailNotification.entrySet()) {
      assertThat(entry.getKey(), equalTo(MOCK_METACARD_ID));
      assertThat(entry.getValue().getLeft().getSourceId(), equalTo(MOCK_SOURCE_ID));
      assertThat(entry.getValue().getRight(), is((long) 1));
    }
  }
}
