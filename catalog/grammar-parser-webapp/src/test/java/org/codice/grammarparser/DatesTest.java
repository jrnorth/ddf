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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.filter.FilterDelegate;
import ddf.catalog.filter.delegate.FilterToTextDelegate;
import ddf.catalog.filter.proxy.adapter.GeotoolsFilterAdapterImpl;
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;
import ddf.catalog.source.UnsupportedQueryException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opengis.filter.Filter;
import org.opentest4j.AssertionFailedError;

public class DatesTest {
  private static final Clock FIXED_CLOCK =
      Clock.fixed(Instant.parse("2020-02-21T12:00:00Z"), ZoneId.of("Z"));

  @BeforeAll
  public static void setupClass() {
    Dates.setClock(FIXED_CLOCK);
    Dates.setFilterBuilder(new GeotoolsFilterBuilder());
  }

  @ParameterizedTest
  @MethodSource(value = "relativeTimeTestCases")
  public void relativeTimeFilter(
      final String timeUnit, final int multiplier, final String expectedIso8601) {
    final Filter relativeTimeFilter = Dates.relativeTimeFilter(timeUnit, multiplier);
    final String relativeTimeFilterStr = toText(relativeTimeFilter);
    assertEquals("after(created," + toDateString(expectedIso8601) + ")", relativeTimeFilterStr);
  }

  static Stream<Arguments> relativeTimeTestCases() {
    return Stream.of(
        arguments("month", 1, "2020-01-21T12:00:00Z"),
        arguments("months", 2, "2019-12-21T12:00:00Z"),
        arguments("week", 1, "2020-02-14T12:00:00Z"),
        arguments("weeks", 2, "2020-02-07T12:00:00Z"),
        arguments("day", 1, "2020-02-20T12:00:00Z"),
        arguments("days", 2, "2020-02-19T12:00:00Z"),
        arguments("hour", 1, "2020-02-21T11:00:00Z"),
        arguments("hours", 2, "2020-02-21T10:00:00Z"),
        arguments("minute", 1, "2020-02-21T11:59:00Z"),
        arguments("minutes", 2, "2020-02-21T11:58:00Z"));
  }

  @ParameterizedTest
  @MethodSource(value = "parseMonthAsRangeStartTestCases")
  public void parseMonthStartAsRangeStart(final String month, final String expectedIso8601) {
    final Date parsedMonthRangeStart = Dates.parseMonthAsRangeStart(month);
    assertEquals(toDate(expectedIso8601), parsedMonthRangeStart);
  }

  static Stream<Arguments> parseMonthAsRangeStartTestCases() {
    return Stream.of(
        arguments("january", "2020-01-01T00:00:00Z"),
        arguments("february", "2020-02-01T00:00:00Z"),
        arguments("march", "2019-03-01T00:00:00Z"));
  }

  @ParameterizedTest
  @MethodSource(value = "parseMonthAsRangeEndTestCases")
  public void parseMonthAsRangeEnd(final String month, final String expectedIso8601) {
    final Date parsedMonthRangeEnd = Dates.parseMonthAsRangeEnd(month);
    assertEquals(toDate(expectedIso8601), parsedMonthRangeEnd);
  }

  static Stream<Arguments> parseMonthAsRangeEndTestCases() {
    return Stream.of(
        arguments("january", "2020-01-31T23:59:59.999Z"),
        arguments("february", "2020-02-29T23:59:59.999Z"),
        arguments("march", "2019-03-31T23:59:59.999Z"));
  }

  @ParameterizedTest
  @MethodSource(value = "parseMonthDateAsRangeStartTestCases")
  public void parseMonthDateAsRangeStart(final String monthDate, final String expectedIso8601) {
    final Date parsedMonthDateRangeStart = Dates.parseMonthDateAsRangeStart(monthDate);
    assertEquals(toDate(expectedIso8601), parsedMonthDateRangeStart);
  }

  static Stream<Arguments> parseMonthDateAsRangeStartTestCases() {
    return Stream.of(
        arguments("january 31st", "2020-01-31T00:00:00Z"),
        arguments("january 31", "2020-01-31T00:00:00Z"),
        arguments("february 29th", "2019-02-28T00:00:00Z"),
        arguments("february 29", "2019-02-28T00:00:00Z"),
        arguments("march 2nd", "2019-03-02T00:00:00Z"),
        arguments("march 2", "2019-03-02T00:00:00Z"),
        arguments("february 3rd", "2020-02-03T00:00:00Z"),
        arguments("february 3", "2020-02-03T00:00:00Z"),
        arguments("january 32", "2020-01-31T00:00:00Z"),
        arguments("january 0", "2020-01-01T00:00:00Z"));
  }

  @ParameterizedTest
  @MethodSource(value = "parseMonthDateAsRangeEndTestCases")
  public void parseMonthDateAsRangeEnd(final String monthDate, final String expectedIso8601) {
    final Date parsedMonthDateRangeEnd = Dates.parseMonthDateAsRangeEnd(monthDate);
    assertEquals(toDate(expectedIso8601), parsedMonthDateRangeEnd);
  }

  static Stream<Arguments> parseMonthDateAsRangeEndTestCases() {
    return Stream.of(
        arguments("january 31st", "2020-01-31T23:59:59.999Z"),
        arguments("january 31", "2020-01-31T23:59:59.999Z"),
        arguments("february 29th", "2019-02-28T23:59:59.999Z"),
        arguments("february 29", "2019-02-28T23:59:59.999Z"),
        arguments("march 2nd", "2019-03-02T23:59:59.999Z"),
        arguments("march 2", "2019-03-02T23:59:59.999Z"),
        arguments("february 3rd", "2020-02-03T23:59:59.999Z"),
        arguments("february 3", "2020-02-03T23:59:59.999Z"),
        arguments("january 32", "2020-01-31T23:59:59.999Z"),
        arguments("january 0", "2020-01-01T23:59:59.999Z"));
  }

  @ParameterizedTest
  @MethodSource(value = "duringFilterTestCases")
  public void duringFilter(final Date from, final Date to, final String expectedFilter) {
    final Filter duringFilter = Dates.during(from, to);
    assertEquals(expectedFilter, toText(duringFilter));
  }

  static Stream<Arguments> duringFilterTestCases() {
    return Stream.of(
        arguments(
            toDate("2020-01-03T00:00:00Z"),
            toDate("2020-01-05T00:00:00Z"),
            "during(created,"
                + toDateString("2020-01-03T00:00:00Z")
                + ","
                + toDateString("2020-01-05T00:00:00Z")
                + ")"),
        arguments(
            toDate("2020-01-05T00:00:00Z"),
            toDate("2020-01-03T00:00:00Z"),
            "during(created,"
                + toDateString("2020-01-03T00:00:00Z")
                + ","
                + toDateString("2020-01-05T00:00:00Z")
                + ")"));
  }

  private static Date toDate(final String iso8601) {
    return new Date(Instant.parse(iso8601).toEpochMilli());
  }

  private static String toDateString(final String iso8601) {
    return toDate(iso8601).toString();
  }

  private static String toText(final Filter filter) {
    final FilterDelegate<String> delegate = new FilterToTextDelegate();
    final FilterAdapter adapter = new GeotoolsFilterAdapterImpl();

    try {
      return adapter.adapt(filter, delegate);
    } catch (UnsupportedQueryException e) {
      throw new AssertionFailedError("Could not convert filter to text.", e);
    }
  }
}
