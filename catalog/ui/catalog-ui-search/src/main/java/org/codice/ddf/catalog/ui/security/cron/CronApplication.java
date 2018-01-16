package org.codice.ddf.catalog.ui.security.cron;

import static ddf.util.Fallible.*;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static org.boon.HTTP.APPLICATION_JSON;
import static spark.Spark.delete;
import static spark.Spark.exception;
import static spark.Spark.get;
import static spark.Spark.patch;
import static spark.Spark.post;

import com.google.common.collect.ImmutableMap;
import ddf.catalog.CatalogFramework;
import ddf.security.Subject;
import ddf.util.Fallible;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.shiro.SecurityUtils;
import org.boon.json.JsonException;
import org.boon.json.JsonFactory;
import org.codice.ddf.catalog.ui.query.monitor.email.EmailNotifier;
import org.codice.ddf.catalog.ui.util.EndpointUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;
import spark.servlet.SparkApplication;

public class CronApplication implements SparkApplication {
  private static final Logger LOGGER = LoggerFactory.getLogger(CronApplication.class);

  private final CatalogFramework catalogFramework;

  private final EndpointUtil endpointUtil;

  private final EmailNotifier emailNotifierService;

  public CronApplication(
      CatalogFramework catalogFramework, EndpointUtil endpointUtil, EmailNotifier emailNotifier) {
    this.catalogFramework = catalogFramework;
    this.endpointUtil = endpointUtil;
    this.emailNotifierService = emailNotifier;
  }

  @Override
  public void init() {
    get(
        "/cron/list",
        APPLICATION_JSON,
        (request, response) -> endpointUtil.getJson(getCronQueries(response)));

    post(
        "/cron/new",
        APPLICATION_JSON,
        (request, response) -> statusAndFallibleToJson(newCronQuery(request), response));

    patch(
        "/cron/edit",
        APPLICATION_JSON,
        (request, response) -> statusAndFallibleToJson(changeCronQuery(request), response));

    delete(
        "/cron/remove",
        APPLICATION_JSON,
        (request, response) -> statusAndFallibleToJson(deleteCronQuery(request), response));

    exception(IOException.class, this::processException);

    exception(RuntimeException.class, this::processException);
  }

  private String processException(Exception exception, Request request, Response response) {
    LOGGER.error(
        "An exception occurred while processing a request to the cron query endpoint!", exception);
    response.status(500);
    response.header(CONTENT_TYPE, APPLICATION_JSON);
    return errorToResponseJson(exception.getMessage());
  }

  private String errorToResponseJson(String error) {
    return endpointUtil.getJson(ImmutableMap.of("message", error));
  }

  private String statusAndFallibleToJson(
      Pair<Integer, Fallible<Object>> statusAndFallible, Response response) {
    response.status(statusAndFallible.getLeft());
    return statusAndFallible.getRight().map(endpointUtil::getJson).orDo(this::errorToResponseJson);
  }

  private Collection<CronQuery> getCronQueries(final Response response) {
    response.status(200);
    return CronQuery.getRunningQueriesFor((Subject) SecurityUtils.getSubject());
  }

  private static Pair<Integer, Fallible<Object>> internalError(String message, Object... params) {
    return ImmutablePair.of(500, error(message, params));
  }

  private static Pair<Integer, Fallible<Object>> badRequest(String message, Object... params) {
    return ImmutablePair.of(400, error(message, params));
  }

  private static Pair<Integer, Fallible<Object>> ok(Object value) {
    return ImmutablePair.of(200, of(value));
  }

  private Pair<Integer, Fallible<Object>> newCronQuery(final Request request) {
    // TODO TEMP
    LOGGER.warn("Trying to start new cron query...");

    String jsonString;
    try {
      jsonString = endpointUtil.safeGetBody(request);
    } catch (IOException exception) {
      return internalError(
          "An IOException occurred when attempting to get the body of the request!");
    }

    CronQueryJson json;
    try {
      json = CronQuery.JSON_MAPPER.fromJson(jsonString, CronQueryJson.class);
    } catch (JsonException exception) {
      return badRequest(
          "The body of this request could not be parsed as an object: " + exception.getMessage());
    }

    return new CronQuery(json, this)
        .start()
        .mapValue(ok(json))
        .orDo(CronApplication::internalError);
  }

  private Pair<Integer, Fallible<Object>> changeCronQuery(final Request request) {
    final Subject subject = (Subject) SecurityUtils.getSubject();

    String requestBody;
    try {
      requestBody = endpointUtil.safeGetBody(request);
    } catch (IOException exception) {
      return internalError(
          "An IOException occurred when attempting to get the body of the request!");
    }

    Map<String, Object> requestJson;
    try {
      requestJson = JsonFactory.create().parser().parseMap(requestBody);
    } catch (JsonException exception) {
      return badRequest(
          "The body of this request could not be parsed as an object: " + exception.getMessage());
    }

    if (!(requestJson.get("oldJobID") instanceof String)) {
      return badRequest("This request is missing the job ID for the job to modify!");
    }
    final String oldJobID = (String) requestJson.get("oldJobID");

    final Object newJobInfo = requestJson.get("newJobInfo");
    if (!(newJobInfo instanceof Map)) {
      return badRequest(
          "This request is missing data pertaining to the changes to make to the given job!");
    }

    String newQuery = null, newCronSchedule = null, newJobID = null;
    for (Map.Entry<String, Object> newJobDatum : ((Map<String, Object>) newJobInfo).entrySet()) {
      switch (newJobDatum.getKey()) {
        case CronQueryJson.QUERY_JSON_KEY:
          if (!(newJobDatum.getValue() instanceof String)) {
            return badRequest(
                "This request expects a string value for \"%s\"!", newJobDatum.getKey());
          }
          newQuery = (String) newJobDatum.getValue();
          break;
        case CronQueryJson.CRON_SCHEDULE_JSON_KEY:
          if (!(newJobDatum.getValue() instanceof String)) {
            return badRequest(
                "This request expects a string value for \"%s\"!", newJobDatum.getKey());
          }
          newCronSchedule = (String) newJobDatum.getValue();
          break;
        case CronQueryJson.JOB_ID_JSON_KEY:
          if (!(newJobDatum.getValue() instanceof String)) {
            return badRequest(
                "This request expects a string value for \"%s\"!", newJobDatum.getKey());
          }
          newJobID = (String) newJobDatum.getValue();
          break;
        default:
          return badRequest(
              "The key \"%s\" in the new job data was not recognized!", newJobDatum.getKey());
      }
    }

    if (newQuery == null && newCronSchedule == null && newJobID == null) {
      return badRequest("No job info was changed; the new job info was empty!");
    }

    final String newQueryF = newQuery, newCronScheduleF = newCronSchedule, newJobIDF = newJobID;
    return CronQuery.findCronQuery(subject, oldJobID)
        .map(
            oldCronQuery -> {
              final String updatedQuery = newQueryF == null ? oldCronQuery.getQuery() : newQueryF;
              final String updatedCronSchedule =
                  newCronScheduleF == null ? oldCronQuery.getCronSchedule() : newCronScheduleF;
              final String updatedJobID = newJobIDF == null ? oldCronQuery.getJobID() : newJobIDF;

              final CronQuery newCronQuery =
                  new CronQuery(updatedQuery, updatedCronSchedule, updatedJobID, this);

              final Fallible<?> stopResult =
                  oldCronQuery.isRunning() ? oldCronQuery.stop() : success();

              return stopResult
                  .mapValue(ok(newCronQuery.start().mapValue(newCronQuery)))
                  .orDo(CronApplication::internalError);
            })
        .orDo(CronApplication::internalError);
  }

  private Pair<Integer, Fallible<Object>> deleteCronQuery(final Request request) {
    final Subject subject = (Subject) SecurityUtils.getSubject();

    String requestJsonString;
    try {
      requestJsonString = endpointUtil.safeGetBody(request);
    } catch (IOException exception) {
      return internalError(
          "An IOException occurred when attempting to get the body of the request: "
              + exception.getMessage());
    }

    Map<String, Object> requestJson;
    try {
      requestJson = JsonFactory.create().parser().parseMap(requestJsonString);
    } catch (JsonException exception) {
      return badRequest(
          "The body of this request could not be parsed as an object: " + exception.getMessage());
    }

    if (!requestJson.containsKey(CronQueryJson.JOB_ID_JSON_KEY)) {
      return badRequest("No job ID was given to identify the job to delete!");
    } else if (requestJson.get(CronQueryJson.JOB_ID_JSON_KEY) instanceof String) {
      return badRequest("I expected a string value for the ID of the job to delete!");
    } else if (requestJson.size() > 1) {
      return badRequest("Only the key \"jobID\" is expected for deletion of jobs!");
    }

    final String jobID = (String) requestJson.get(CronQueryJson.JOB_ID_JSON_KEY);
    return CronQuery.findCronQuery(subject, jobID)
        .tryMap(cronQuery -> cronQuery.stop().mapValue(ok(cronQuery)))
        .orDo(CronApplication::internalError);
  }

  CatalogFramework getCatalogFramework() {
    return catalogFramework;
  }

  EmailNotifier getEmailNotifierService() {
    return emailNotifierService;
  }
}
