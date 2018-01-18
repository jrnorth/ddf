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
