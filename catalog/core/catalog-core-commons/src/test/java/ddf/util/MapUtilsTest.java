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
package ddf.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableMap;
import java.text.DecimalFormat;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.Test;

public class MapUtilsTest {
  private static final ImmutableMap<String, Integer> HARRY_POTTER_WORD_COUNTS =
      ImmutableMap.<String, Integer>builder()
          .put("Sorcerer's Stone", 76_944)
          .put("Chamber of Secrets", 85_141)
          .put("Prisoner of Azkaban", 107_253)
          .put("Goblet of Fire", 190_637)
          .put("Order of the Phoenix", 257_045)
          .put("Half-Blood Prince", 168_923)
          .put("Deathly Hallows", 198_227)
          .build();

  @Test
  public void testMapKeys() {
    final Map<Integer, Integer> titleLengthKeys =
        MapUtils.mapKeys(HARRY_POTTER_WORD_COUNTS, String::length);
    assertThat(
        titleLengthKeys,
        equalTo(
            ImmutableMap.builder()
                .put(16, 76_944)
                .put(18, 85_141)
                .put(19, 107_253)
                .put(14, 190_637)
                .put(20, 257_045)
                .put(17, 168_923)
                .put(15, 198_227)
                .build()));
  }

  @Test
  public void testMapValues() {
    final Map<String, Double> approximateLetterCounts =
        MapUtils.mapValues(HARRY_POTTER_WORD_COUNTS, wordCount -> wordCount * 5.1);
    assertThat(
        approximateLetterCounts,
        equalTo(
            ImmutableMap.builder()
                .put("Sorcerer's Stone", 392_414.4)
                .put("Chamber of Secrets", 434_219.1)
                .put("Prisoner of Azkaban", 546_990.3)
                .put("Goblet of Fire", 972_248.7)
                .put("Order of the Phoenix", 1_310_929.5)
                .put("Half-Blood Prince", 861_507.3)
                .put("Deathly Hallows", 1_010_957.7)
                .build()));
  }

  @Test
  public void testMap() {
    final Map<String, String> asIdentifiersAndWithUnits =
        MapUtils.map(
            HARRY_POTTER_WORD_COUNTS,
            title -> title.replaceAll("\\b[a-z]{2,}\\b", "").replaceAll("\\W", ""),
            wordCount -> new DecimalFormat("#,###").format(wordCount) + " words");
    assertThat(
        asIdentifiersAndWithUnits,
        equalTo(
            ImmutableMap.builder()
                .put("SorcerersStone", "76,944 words")
                .put("ChamberSecrets", "85,141 words")
                .put("PrisonerAzkaban", "107,253 words")
                .put("GobletFire", "190,637 words")
                .put("OrderPhoenix", "257,045 words")
                .put("HalfBloodPrince", "168,923 words")
                .put("DeathlyHallows", "198,227 words")
                .build()));
  }

  @Test
  public void testKey() {
    final Map<Integer, String> titlesByTheirLengths =
        MapUtils.key(HARRY_POTTER_WORD_COUNTS.keySet(), String::length);
    assertThat(
        titlesByTheirLengths,
        equalTo(
            ImmutableMap.builder()
                .put(16, "Sorcerer's Stone")
                .put(18, "Chamber of Secrets")
                .put(19, "Prisoner of Azkaban")
                .put(14, "Goblet of Fire")
                .put(20, "Order of the Phoenix")
                .put(17, "Half-Blood Prince")
                .put(15, "Deathly Hallows")
                .build()));
  }

  @Test
  public void testCollectEntries() {
    final Map<String, Boolean> titlesWithOfByWhetherThereAreMoreLettersAfterOf =
        HARRY_POTTER_WORD_COUNTS
            .keySet()
            .stream()
            .filter(title -> title.contains(" of "))
            .map(
                title -> {
                  final String[] sidesOfOf = title.split(" of ");
                  return new SimpleImmutableEntry<>(
                      title, sidesOfOf[0].length() < sidesOfOf[1].length());
                })
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    assertThat(
        titlesWithOfByWhetherThereAreMoreLettersAfterOf,
        equalTo(
            ImmutableMap.of(
                // @formatter:off
                "Chamber of Secrets", false,
                "Prisoner of Azkaban", false,
                "Goblet of Fire", false,
                "Order of the Phoenix", true)));
    // @formatter:on
  }
}
