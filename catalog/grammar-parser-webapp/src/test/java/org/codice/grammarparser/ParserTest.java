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

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.filter.FilterDelegate;
import ddf.catalog.filter.delegate.FilterToTextDelegate;
import ddf.catalog.filter.proxy.adapter.GeotoolsFilterAdapterImpl;
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;
import ddf.catalog.source.UnsupportedQueryException;
import java.io.StringReader;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opengis.filter.Filter;
import org.opentest4j.AssertionFailedError;

public class ParserTest {
  private static final String WKT = "POLYGON ((30 10, 40 40, 20 40, 10 20, 30 10))";

  private static final Clock FIXED_CLOCK =
      Clock.fixed(Instant.parse("2020-01-21T12:00:00Z"), ZoneId.of("Z"));

  @BeforeAll
  public static void setupClass() {
    Dates.setClock(FIXED_CLOCK);
    Dates.setFilterBuilder(new GeotoolsFilterBuilder());
  }

  @ParameterizedTest
  @MethodSource(value = "testCases")
  void javacc(final String searchExpression, final List<String> filters) throws ParseException {
    final Parser parser = new Parser(new StringReader(searchExpression));
    parser.setFilterBuilder(new GeotoolsFilterBuilder());
    final List<String> actualFilters =
        parser.SearchExpression().stream().map(this::toText).collect(toList());
    assertEquals(filters, actualFilters, "'" + searchExpression + "' was not parsed correctly");
  }

  static Stream<Arguments> testCases() {
    return Stream.of(
        arguments("satellite imagery", singletonList("like(anyText,satellite imagery)")),
        arguments(
            "satellite imagery in Arizona",
            asList("like(anyText,satellite imagery)", "intersects(anyGeo,wkt(" + WKT + "))")),
        arguments(
            "satellite imagery in the last week",
            asList(
                "like(anyText,satellite imagery)",
                "after(created," + toDateString("2020-01-14T12:00:00Z") + ")")),
        arguments(
            "satellite imagery in the last 12 hours",
            asList(
                "like(anyText,satellite imagery)",
                "after(created," + toDateString("2020-01-21T00:00:00Z") + ")")),
        arguments(
            "satellite imagery in Arizona in the last 12 hours",
            asList(
                "like(anyText,satellite imagery)",
                "intersects(anyGeo,wkt(" + WKT + "))",
                "after(created," + toDateString("2020-01-21T00:00:00Z") + ")")),
        arguments(
            "satellite imagery between October and December",
            asList(
                "like(anyText,satellite imagery)",
                "during(created,"
                    + toDateString("2019-10-01T00:00:00Z")
                    + ","
                    + toDateString("2019-12-31T23:59:59Z")
                    + ")")),
        arguments(
            "satellite imagery in Arizona between November and January",
            asList(
                "like(anyText,satellite imagery)",
                "intersects(anyGeo,wkt(" + WKT + "))",
                "during(created,"
                    + toDateString("2019-11-01T00:00:00Z")
                    + ","
                    + toDateString("2020-01-31T23:59:59Z")
                    + ")")),
        arguments(
            "satellite imagery between November 11th and January 2nd",
            asList(
                "like(anyText,satellite imagery)",
                "during(created,"
                    + toDateString("2019-11-11T00:00:00Z")
                    + ","
                    + toDateString("2020-01-02T23:59:59Z")
                    + ")")),
        arguments(
            "satellite imagery between November 11 and January 2",
            asList(
                "like(anyText,satellite imagery)",
                "during(created,"
                    + toDateString("2019-11-11T00:00:00Z")
                    + ","
                    + toDateString("2020-01-02T23:59:59Z")
                    + ")")));
  }

  private String toText(final Filter filter) {
    final FilterDelegate<String> delegate = new FilterToTextDelegate();
    final FilterAdapter adapter = new GeotoolsFilterAdapterImpl();

    try {
      return adapter.adapt(filter, delegate);
    } catch (UnsupportedQueryException e) {
      throw new AssertionFailedError("Could not convert filter to text.", e);
    }
  }

  private static String toDateString(final String iso8601) {
    return new Date(Instant.parse(iso8601).toEpochMilli()).toString();
  }
}
