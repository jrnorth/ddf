package org.codice.ddf.catalog.ui.security.cron;

public class CronQueryJson {
  public static final String QUERY_JSON_KEY = "query";

  public static final String CRON_SCHEDULE_JSON_KEY = "cronSchedule";

  public static final String JOB_ID_JSON_KEY = "jobID";

  private final String query;

  private final String cronSchedule;

  private final String jobID;

  CronQueryJson(String query, String cronSchedule, String jobID) {
    this.query = query;
    this.cronSchedule = cronSchedule;
    this.jobID = jobID;
  }

  public String getQuery() {
    return query;
  }

  public String getCronSchedule() {
    return cronSchedule;
  }

  public String getJobID() {
    return jobID;
  }

  @Override
  public String toString() {
    return String.format("job \"%s\" running \"%s\" at %s", jobID, query, cronSchedule);
  }
}
