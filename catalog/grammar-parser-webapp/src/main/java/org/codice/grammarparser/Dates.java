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
package org.codice.grammarparser;

import ddf.catalog.filter.FilterBuilder;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;
import org.opengis.filter.Filter;

public class Dates {
  private static FilterBuilder filterBuilder;

  public static void setFilterBuilder(final FilterBuilder filterBuilder) {
    Dates.filterBuilder = filterBuilder;
  }

  private static Clock clock = Clock.systemUTC();

  static void setClock(final Clock newClock) {
    clock = newClock;
  }

  private Dates() {}

  public static Filter relativeTimeFilter(final String timeUnit, final int multiplier) {
    return filterBuilder
        .attribute("created")
        .after()
        .date(relativeDateBeforeNow(timeUnit, multiplier));
  }

  public static Date relativeDateBeforeNow(final String timeUnit, final int multiplier) {
    final ZonedDateTime now = ZonedDateTime.now(clock);
    final ZonedDateTime relativeDateTime;
    switch (timeUnit.toLowerCase()) {
      case "month":
      case "months":
        relativeDateTime = now.minusMonths(multiplier);
        break;
      case "week":
      case "weeks":
        relativeDateTime = now.minusWeeks(multiplier);
        break;
      case "day":
      case "days":
        relativeDateTime = now.minusDays(multiplier);
        break;
      case "hour":
      case "hours":
        relativeDateTime = now.minusHours(multiplier);
        break;
      case "minute":
      case "minutes":
        relativeDateTime = now.minusMinutes(multiplier);
        break;
      default:
        throw new IllegalStateException(
            "Shouldn't happen because this is only called when a time unit token is present.");
    }
    return toDate(relativeDateTime);
  }

  public static Date parseMonthAsRangeStart(final String month) {
    final ZonedDateTime now = ZonedDateTime.now(clock);
    final int thisMonth = now.getMonthValue();
    int year = now.getYear();
    final int parsedMonth = Month.valueOf(month.toUpperCase()).getValue();
    if (parsedMonth > thisMonth) {
      year -= 1;
    }
    final ZonedDateTime parsedDateTime = atStartOfDay(LocalDate.of(year, parsedMonth, 1));
    return toDate(parsedDateTime);
  }

  public static Date parseMonthAsRangeEnd(final String month) {
    final ZonedDateTime now = ZonedDateTime.now(clock);
    final int thisMonth = now.getMonthValue();
    int year = now.getYear();
    final int parsedMonth = Month.valueOf(month.toUpperCase()).getValue();
    if (parsedMonth > thisMonth) {
      year -= 1;
    }
    final ZonedDateTime parsedDateTime =
        atEndOfDay(LocalDate.of(year, parsedMonth, 1)).with(TemporalAdjusters.lastDayOfMonth());
    return toDate(parsedDateTime);
  }

  public static Date parseMonthDateAsRangeStart(final String monthDate) {
    final LocalDate adjustedDate = adjustMonthDateRelativeToCurrentTime(monthDate);
    final ZonedDateTime parsedDateTime = atStartOfDay(adjustedDate);
    return toDate(parsedDateTime);
  }

  public static Date parseMonthDateAsRangeEnd(final String monthDate) {
    final LocalDate adjustedDate = adjustMonthDateRelativeToCurrentTime(monthDate);
    final ZonedDateTime parsedDateTime = atEndOfDay(adjustedDate);
    return toDate(parsedDateTime);
  }

  public static Filter during(final Date from, final Date to) {
    return from.before(to)
        ? filterBuilder.attribute("created").is().during().dates(from, to)
        : filterBuilder.attribute("created").is().during().dates(to, from);
  }

  private static ZonedDateTime atStartOfDay(final LocalDate date) {
    return ZonedDateTime.of(
        date.getYear(), date.getMonthValue(), date.getDayOfMonth(), 0, 0, 0, 0, ZoneId.of("UTC"));
  }

  private static ZonedDateTime atEndOfDay(final LocalDate date) {
    return ZonedDateTime.of(
        date.getYear(),
        date.getMonthValue(),
        date.getDayOfMonth(),
        23,
        59,
        59,
        999_999_999,
        ZoneId.of("UTC"));
  }

  private static Date toDate(final ZonedDateTime zonedDateTime) {
    final long millis = zonedDateTime.toInstant().toEpochMilli();
    return new Date(millis);
  }

  private static int[] parseMonthDate(final String monthDate) {
    final String monthDateSingleSpace = monthDate.replaceAll(" +", " ");
    final String[] monthDateSplit = monthDateSingleSpace.split(" ");
    final String month = monthDateSplit[0];
    final int parsedMonth = Month.valueOf(month.toUpperCase()).getValue();
    final String date = monthDateSplit[1];
    final String dateOnlyNumbers = date.replaceAll("[^0-9]", "");
    final int parsedDate = Integer.parseInt(dateOnlyNumbers);
    return new int[] {parsedMonth, parsedDate};
  }

  private static LocalDate adjustMonthDateRelativeToCurrentTime(final String monthDate) {
    final ZonedDateTime now = ZonedDateTime.now(clock);
    final int thisMonth = now.getMonthValue();
    final int dayOfMonth = now.getDayOfMonth();
    int year = now.getYear();

    final int[] parsedMonthDate = parseMonthDate(monthDate);
    final int parsedMonth = parsedMonthDate[0];
    final int parsedDate = parsedMonthDate[1];

    final int daysInMonth = YearMonth.of(year, parsedMonth).lengthOfMonth();
    int dateAdjustedToMonth = Math.max(Math.min(parsedDate, daysInMonth), 1);
    if (parsedMonth > thisMonth || (parsedMonth == thisMonth && dateAdjustedToMonth > dayOfMonth)) {
      year -= 1;
      // Length of February might be different between years
      dateAdjustedToMonth =
          Math.min(YearMonth.of(year, parsedMonth).lengthOfMonth(), dateAdjustedToMonth);
    }
    return LocalDate.of(year, parsedMonth, dateAdjustedToMonth);
  }
}
