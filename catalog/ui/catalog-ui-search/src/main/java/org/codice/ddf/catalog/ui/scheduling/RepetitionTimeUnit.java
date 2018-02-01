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

import java.util.stream.IntStream;
import org.joda.time.DateTime;

public enum RepetitionTimeUnit {
  // To repeat each unit of time in a cron expression, fields will either need to be starred ("*")
  // or filled with the value of that field in the start date-time.
  // where "S" is the value of that field in the start date:
  // every minute: * * * * *
  // every hour:   S * * * *
  // every day:    S S * * *
  // every week:   S S * * S
  // every month:  S S S * *
  // every year:   S S S S *

  MINUTES(),
  HOURS(0),
  DAYS(0, 1),
  WEEKS(0, 1, 4),
  MONTHS(0, 1, 2),
  YEARS(0, 1, 2, 3);

  private int[] cronFieldsThatShouldBeFilled;

  RepetitionTimeUnit(int... cronFieldsThatShouldBeFilled) {
    this.cronFieldsThatShouldBeFilled = cronFieldsThatShouldBeFilled;
  }

  public boolean cronFieldShouldBeFilled(int index) {
    return IntStream.of(cronFieldsThatShouldBeFilled)
        .anyMatch(cronFieldIndex -> cronFieldIndex == index);
  }

  public String makeCronToRunEachUnit(DateTime start) {
    final String[] cronFields = new String[5];
    final int[] startValues =
        new int[] {
          start.getMinuteOfHour(),
          start.getHourOfDay(),
          start.getDayOfMonth(),
          start.getMonthOfYear(),
          // Joda's day of the week value := 1-7 Monday-Sunday;
          // cron's day of the week value := 0-6 Sunday-Saturday
          start.getDayOfWeek() % 7
        };
    for (int i = 0; i < 5; i++) {
      if (cronFieldShouldBeFilled(i)) {
        cronFields[i] = String.valueOf(startValues[i]);
      } else {
        cronFields[i] = "*";
      }
    }

    return String.join(" ", cronFields);
  }
}
