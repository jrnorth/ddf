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
package ddf.test.itests.catalog;

import static com.jayway.restassured.RestAssured.given;
import static java.time.LocalDateTime.now;
import static org.codice.ddf.itests.common.AbstractIntegrationTest.DynamicUrl.SECURE_ROOT;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.RequestSpecification;
import com.jayway.restassured.specification.ResponseSpecification;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.boon.json.JsonFactory;
import org.boon.json.ObjectMapper;
import org.codice.ddf.itests.common.AbstractIntegrationTest;
import org.codice.ddf.itests.common.annotations.BeforeExam;
import org.codice.ddf.itests.common.utils.LoggingUtils;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerSuite;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
public class TestCronQueryEndpoint extends AbstractIntegrationTest {
  private static final ObjectMapper JSON = JsonFactory.create();

  private static final String USER_CRON_PATH = "/search/catalog/internal/user/cron";

  private static final DynamicUrl USER_CRON_URL =
      new DynamicUrl(SECURE_ROOT, HTTPS_PORT, USER_CRON_PATH);

  private static final String CRON_SCHEDULE_JSON_QUERY_KEY = "cronSchedule";

  private static final String QUERY_JSON_QUERY_KEY = "query";

  private static final String JOB_ID_JSON_QUERY_KEY = "jobID";

  private static final String MOCK_QUERY = "*";

  private static String toCron(LocalDateTime instant) {
    return Stream.of(
            instant.getSecond(),
            instant.getMinute(),
            instant.getHour(),
            instant.getDayOfMonth(),
            instant.getMonth().getValue(),
            "?",
            instant.getYear())
        .map(String::valueOf)
        .collect(Collectors.joining(" "));
  }

  private static ImmutableMap<String, Object> newCronQueryJson(
      LocalDateTime instant, String jobID) {
    return ImmutableMap.of(
        // @formatter:off
        QUERY_JSON_QUERY_KEY, MOCK_QUERY,
        CRON_SCHEDULE_JSON_QUERY_KEY, toCron(instant),
        JOB_ID_JSON_QUERY_KEY, jobID);
    // @formatter:on
  }

  @BeforeExam
  public void beforeExam() throws Exception {
    try {
      waitForSystemReady();
      getServiceManager().waitForHttpEndpoint(USER_CRON_URL.getUrl());
      getServiceManager().waitForAllBundles();
    } catch (Exception e) {
      LoggingUtils.failWithThrowableStacktrace(e, "Failed in @BeforeExam: ");
    }
  }

  @After
  public void cleanUp() {
    clearCatalog();
  }

  private static RequestSpecification asGuest() {
    return given().log().all().header("Content-Type", "application/json");
  }

  private static RequestSpecification asUser(String username, String password) {
    return asGuest().auth().preemptive().basic(username, password);
  }

  private static RequestSpecification asAdmin() {
    return asUser("admin", "admin");
  }

  private static ResponseSpecification expect(RequestSpecification req, int status) {
    return req.expect().log().all().statusCode(status).when();
  }

  @Test
  public void testGuestCannotMakeCronQueries() {
    final String testCronQueriesJson =
        JSON.writeValueAsString(
            ImmutableList.of(newCronQueryJson(now().plusMonths(1), "guest cron query")));
    Response putResponse = asGuest().put(testCronQueriesJson);
    assertThat(putResponse.getStatusCode(), is(401));
  }

  @Test
  public void testUserCanMakeCronQueries() {
    final ImmutableList<ImmutableMap<String, Object>> cronQueries =
        ImmutableList.of(newCronQueryJson(now().plusMonths(1), "Variadicism cron query"));
    Response putResponse =
        asUser("Variadicism", "password").put(JSON.writeValueAsString(cronQueries));

    assertThat(putResponse.getStatusCode(), is(200));
    final Object bodyJson = JSON.fromJson(putResponse.getBody().print());
    assertThat(bodyJson, instanceOf(List.class));
    assertThat((List<Object>) bodyJson, hasItems(cronQueries));

    asUser("Variadicism", "password").put(JSON.writeValueAsString(Collections.emptyList()));
  }

  @Test
  public void testStartQueries() throws InterruptedException {
    final ImmutableList<ImmutableMap<String, Object>> cronQueries =
        ImmutableList.of(newCronQueryJson(now().plusSeconds(1), "cron query in 1 second"));
    Response putResponse =
        asUser("Variadicism", "password").put(JSON.writeValueAsString(cronQueries));

    assertThat(putResponse.getStatusCode(), is(200));
    final Object bodyJson = JSON.fromJson(putResponse.getBody().print());
    assertThat(bodyJson, instanceOf(List.class));
    assertThat((List<Object>) bodyJson, hasItems(cronQueries));

    Thread.sleep(1200);

    // TODO: check for email

    asUser("Variadicism", "password").put(JSON.writeValueAsString(Collections.emptyList()));
  }
}
