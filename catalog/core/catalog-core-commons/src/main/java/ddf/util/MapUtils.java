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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class MapUtils {

  /**
   * Applies two given functions to each of the keys and values of a given map, respectively.
   *
   * @param oldMap the {@link Map} to which the two functions will be applied.
   * @param keyTransformer the function to be applied to each key in the {@link Map}.
   * @param valueTransformer the function to be applied to each value in the {@link Map}.
   * @param <OldKey> the type of the key in the given old {@link Map}.
   * @param <OldValue> the type of the value in the given old {@link Map}.
   * @param <NewKey> the type of the key that the old {@link Map}'s keys will be transformed into.
   * @param <NewValue> the type of the value that the old {@link Map}'s values will be transformed
   *     into.
   * @return a new {@link Map} made by applying the two given transformer functions to the given
   *     {@link Map}.
   */
  public static <OldKey, OldValue, NewKey, NewValue> Map<NewKey, NewValue> map(
      Map<OldKey, OldValue> oldMap,
      Function<OldKey, NewKey> keyTransformer,
      Function<OldValue, NewValue> valueTransformer) {
    return oldMap
        .entrySet()
        .stream()
        .collect(
            Collectors.toMap(
                entry -> keyTransformer.apply(entry.getKey()),
                entry -> valueTransformer.apply(entry.getValue())));
  }

  /**
   * Applies the given function to the keys in the given {@link Map}.
   *
   * @param oldMap the {@link Map} to be transformed.
   * @param keyTransformer the function to be applied to each key in the {@link Map}.
   * @param <OldKey> the type of the key in the given old {@link Map}.
   * @param <Value> the type of the values in the {@link Map} which will not be modified.
   * @param <NewKey> the type of the key that the old {@link Map}'s keys will be transformed into.
   * @return a new {@link Map} with keys resulting from transforming the keys in the given {@link
   *     Map} and the same values as given in the original {@link Map}.
   */
  public static <OldKey, Value, NewKey> Map<NewKey, Value> mapKeys(
      Map<OldKey, Value> oldMap, Function<OldKey, NewKey> keyTransformer) {
    return map(oldMap, keyTransformer, Function.identity());
  }

  /**
   * Applies the given function to the values in the given {@link Map}.
   *
   * @param oldMap the {@link Map} to be transformed.
   * @param valueTransformer the function to be applied to each value in the {@link Map}.
   * @param <Key> the type of the keys in the {@link Map} which will not be modified.
   * @param <OldValue> the type of the value in the given old {@link Map}.
   * @param <NewValue> the type of the value that the old {@link Map}'s values will be transformed
   *     into.
   * @return a new {@link Map} with values resulting from transforming the values in the given
   *     {@link Map} and the same keys as given in the original {@link Map}.
   */
  public static <Key, OldValue, NewValue> Map<Key, NewValue> mapValues(
      Map<Key, OldValue> oldMap, Function<OldValue, NewValue> valueTransformer) {
    return map(oldMap, Function.identity(), valueTransformer);
  }

  /**
   * Creates a {@link Map} from a {@link java.util.List} using a function given to generate the map
   * keys from each element. If keys generated from two different elements in the list result in the
   * same key, an {@link IllegalStateException} is thrown as per the implementation of {@link
   * Collectors#toMap(Function, Function)}.
   *
   * @param values the {@link java.util.List} of values which will become the resulting map's
   *     values.
   * @param keyMaker the function that can generate a key from each element in the given list of
   *     values.
   * @param <Key> the type of the keys to be generated from the given values.
   * @param <Value> the type of the values given
   * @return a new {@link Map} where the keys are generated from the given function applied to
   *     corresponding values and the values are taken from the given list of values.
   */
  public static <Key, Value> Map<Key, Value> key(
      Collection<Value> values, Function<Value, Key> keyMaker) {
    return values.stream().collect(Collectors.toMap(keyMaker, Function.identity()));
  }

  /**
   * A {@link Collector} to transform a {@link java.util.stream.Stream} of {@link Map.Entry
   * Map.Entries} into a {@link Map}. If two keys in the given {@link java.util.List} have the same
   * key, an {@link IllegalStateException} is thrown as per the implementation of {@link
   * Collectors#toMap(Function, Function)}.
   *
   * @param <Key> the type of the keys in the given {@link Map.Entry Map.Entries} and the resulting
   *     {@link Map}.
   * @param <Value> the type of the values in the given {@link Map.Entry Map.Entries} and the
   *     resulting {@link Map}.
   * @return a new {@link Map} where the keys and values are taken from the {@link Map.Entry
   *     Map.Entries} given.
   */
  public static <Key, Value>
      Collector<? extends Map.Entry<Key, Value>, ?, ? extends Map<Key, Value>> collectEntries() {
    return Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue);
  }

  public static <Element, Key, Value> Map<Key, Value> fromList(
      List<Element> list, Function<Element, Key> keyMaker, Function<Element, Value> valueMaker) {
    return list.stream().collect(Collectors.toMap(keyMaker, valueMaker));
  }
}
