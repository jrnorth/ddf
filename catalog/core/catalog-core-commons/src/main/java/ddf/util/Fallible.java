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

import com.sun.istack.internal.Nullable;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Fallible<Value> {
  private String error;
  private Value value;

  private Fallible(String error, Value value) {
    this.error = error;
    this.value = value;
  }

  /**
   * Creates a {@link Fallible} containing the given value rather than an error.
   *
   * @param value the value to be contained.
   * @param <Value> the type of the value to be contained.
   * @return a new {@link Fallible} containing the given value.
   */
  public static <Value> Fallible<Value> of(Value value) {
    return new Fallible<>(null, value);
  }

  /**
   * Creates a {@link Fallible} containing the given value if the value is not <b><tt>null</tt></b>
   * or a {@link Fallible} containing the given error message otherwise.
   *
   * @param value the value to be contained if not <b><tt>null</tt></b>.
   * @param errorIfNull the error to be contained if the given value is <b><tt>null</tt></b>.
   * @param formatParams the parameters used to construct the message via {@link
   *     String#format(String, Object...)}.
   * @param <Value> the type of the value to be contained if not <b><tt>null</tt></b>.
   * @return a {@link Fallible} containing the given value if the value is not <b><tt>null</tt></b>;
   *     a {@link Fallible} containing the given error message otherwise.
   */
  public static <Value> Fallible<Value> ofNullable(
      @Nullable Value value, String errorIfNull, Object... formatParams) {
    return value == null ? error(errorIfNull, formatParams) : of(value);
  }

  /**
   * Creates a {@link Fallible} containing a meaningless value of type <tt>?</tt>. This is a more
   * readable, convenient way to return success in methods that are error-prone, but have no need to
   * return a value.
   *
   * @return a {@link Fallible} containing a value of <b><tt>null</tt></b>.
   */
  public static Fallible<?> success() {
    return of(null);
  }

  /**
   * Creates a {@link Fallible} containing the given error message.
   *
   * @param errorFormat the format of the error message to be contained. This will be used as the
   *     format string in a call to {@link String#format(String, Object...)}.
   * @param formatParams the parameters used to construct the error message via {@link
   *     String#format(String, Object...)}.
   * @param <Any> whatever type matches in context.
   * @return a new {@link Fallible} containing the given error message.
   */
  public static <Any> Fallible<Any> error(String errorFormat, Object... formatParams) {
    return new Fallible<>(String.format(errorFormat, formatParams), null);
  }

  public static <OldElement> Fallible<?> forEach(Collection<OldElement> inputs, Function<OldElement, Fallible<?>> mapper) {
    final String errors =
            inputs
                    .stream()
                    .map(input -> mapper.apply(input))
                    .map(fallible -> fallible.mapValue((String) null).orDo(Function.identity()))
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining("\n"));
    if (errors.isEmpty()) {
      return success();
    }

    return error(errors);
  }

  /**
   * Determines whether or not this {@link Fallible} has an error as opposed to a value.
   *
   * @return <b><tt>true</tt></b> if this {@link Fallible} has an error and no value;
   *     <b><tt>false</tt></b> otherwise.
   */
  public boolean hasError() {
    return error != null;
  }

  /**
   * @param value the value to return if this {@link Fallible} has an error rather than a value.
   * @return this {@link Fallible}'s value if it has one or the given value if it has an error.
   */
  public final Value or(Value value) {
    return orDo(error -> value);
  }

  /**
   * Applies the given function to the error message if this {@link Fallible} has one and returns
   * the result; if it has a value, its value is returned.
   *
   * @param function the function to be applied to the error message to return an alternate value if
   *     needed.
   * @return either this {@link Fallible}'s value or the result of the given function's application
   *     to this {@link Fallible}'s error message.
   */
  public final Value orDo(Function<? super String, ? extends Value> function) {
    return hasError() ? function.apply(error) : value;
  }

  /**
   * Applies the given function to the error message if this {@link Fallible} has one and returns a
   * {@link Fallible} result; if it has a value, nothing changes.
   *
   * @param function the function to be applied to the error message to return an alternate {@link
   *     Fallible} value if needed.
   * @return either this {@link Fallible} or the result of the given function's application to this
   *     {@link Fallible}'s error message.
   */
  public final Fallible<Value> orTry(Function<? super String, Fallible<Value>> function) {
    return hasError() ? function.apply(error) : this;
  }

  /**
   * Applies the given function to the error message if this {@link Fallible} has one and throws the
   * resulting {@link Throwable}; if it has a value, its value is returned.
   *
   * @param function the function to be applied to the error message to return a {@link Throwable}
   *     if needed.
   * @param <E> the type of {@link Throwable} (likely {@link Exception}) to be thrown if this {@link
   *     Fallible} has no value.
   * @return this {@link Fallible}'s value if it has one.
   * @throws E the {@link Throwable} (likely {@link Exception}) constructed by applying the given
   *     function to the error message if this {@link Fallible} has an error.
   */
  public final <E extends Throwable> Value elseThrow(Function<? super String, E> function)
      throws E {
    if (hasError()) {
      throw function.apply(this.error);
    }
    return this.value;
  }

  /**
   * Applies the given function to the error message if this {@link Fallible} has one and throws the
   * resulting {@link RuntimeException}; if it has a value, its value is returned.
   *
   * <p>This function is very similar to {@link #elseThrow(Function) elseThrow} with the exceptions
   * that <b>1)</b> the type of the {@link RuntimeException} thrown will not be added to this
   * method's <b>throws</b> signature and <b>2)</b> the object thrown is restricted to {@link
   * RuntimeException} instead of any {@link Throwable} so that no <b>throws</b> declaration is
   * necessary.
   *
   * @param function the function to be applied to the error message to return a {@link
   *     RuntimeException} if needed.
   * @param <RTE> the type of {@link RuntimeException} to be thrown if this {@link Fallible} has no
   *     value.
   * @return this {@link Fallible}'s value if it has one.
   * @throws RTE the {@link RuntimeException} constructed by applying the given function to the
   *     error message if this {@link Fallible} has an error.
   */
  public final <RTE extends RuntimeException> Value elseThrowRTE(
      Function<? super String, RTE> function) {
    if (hasError()) {
      throw function.apply(this.error);
    }
    return this.value;
  }

  /**
   * Applies the given transformer function to this {@link Fallible}'s value if it has a value; if
   * it has an error, nothing happens.
   *
   * @param transformer the {@link Function} to be applied to this {@link Fallible}'s value.
   * @param <R> the return type of the transformer function.
   * @return a {@link Fallible} containing either the same error that it did before or a value
   *     returned from the given transformer function.
   */
  public final <R> Fallible<R> map(Function<? super Value, R> transformer) {
    // This is equivalent to mapValue(transformer.apply(value)) except that the short circuiting of
    // the ternary operator
    // can ensure that the transformer function is not executed unless the value is used.
    return hasError() ? error(error) : of(transformer.apply(value));
  }

  /**
   * Returns a new {@link Fallible} with the given new value if it has a value; if it has an error,
   * nothing happens. This is roughly equivalent to the following {@link #map(Function)} method call
   * where "<tt>newValue</tt>" is the given <b><tt>newValue</tt></b>, but more efficient and more
   * readable:
   *
   * <pre>
   *   map(oldValue -> newValue)
   * </pre>
   *
   * @param newValue the value to replace the existing value unless this has an error.
   * @param <R> the new value's type.
   * @return a new {@link Fallible} containing either the new value or an error.
   */
  public final <R> Fallible<R> mapValue(R newValue) {
    return hasError() ? error(error) : of(newValue);
  }

  /**
   * Applies the given transformer function to this {@link Fallible}'s value if it has a value; if
   * this {@link Fallible} has an error, nothing happens.
   *
   * <p>This function is very similar to {@link #map(Function) map} with the exception that if the
   * transformer function is used, the returned {@link Fallible} will be returned as it is rather
   * than packaging it into a {@link Fallible}\<{@link Fallible}\<{@link R}\>\>.
   *
   * @param transformer the {@link Function} to be applied to this {@link Fallible}'s value.
   * @param <R> the return type of the transformer function.
   * @return a {@link Fallible} containing either the same error that it did before or a value
   *     returned from the given transformer function.
   */
  public final <R> Fallible<R> tryMap(Function<? super Value, Fallible<R>> transformer) {
    // This is equivalent to tryMapValue(transformer.apply(value)) except that the short circuiting
    // of the ternary operator can ensure that the transformer function is not executed unless the
    // value is used.
    return hasError() ? error(error) : transformer.apply(value);
  }

  /**
   * Returns the given new {@link Fallible} if this {@link Fallible} has a value; if this {@link
   * Fallible} has an error, nothing happens.
   *
   * @param newValue is the new {@link Fallible} to return if this {@link Fallible} has a value.
   * @param <R> the inner type of the new {@link Fallible}.
   * @return this {@link Fallible} if it has an error; the given new {@link Fallible} otherwise.
   */
  public final <R> Fallible<R> tryMapValue(Fallible<R> newValue) {
    return hasError() ? error(error) : newValue;
  }

  /**
   * Applies the given transformer function to this {@link Fallible}'s error message if it has one;
   * if this {@link Fallible} has a value, nothing happens.
   *
   * @param errorTransformer is the {@link Function} to be applied to this {@link Fallible}'s error
   *     message.
   * @return a {@link Fallible} containing either the same value as before or a transformation of
   *     the error message previously contained.
   */
  public final Fallible<Value> mapError(Function<String, String> errorTransformer) {
    return hasError() ? error(errorTransformer.apply(error)) : this;
  }

  public final Fallible<Value> prependToError(String prefix, Object... formatParams) {
    return hasError() ? error(String.format(prefix, formatParams) + error) : this;
  }

  /**
   * Applies a given function to this {@link Fallible}'s value if it has a value; if it has an
   * error, nothing happens.
   *
   * <p>This function is very similar to {@link #map(Function) map} with the exception that the
   * value returned from the given function will be discarded, not used to replace the original
   * value contained inside this {@link Fallible}.
   *
   * @param function the {@link Function} to be applied to this {@link Fallible}'s value.
   * @return {@link this}.
   */
  public final Fallible<Value> ifValue(Consumer<? super Value> function) {
    if (!hasError()) {
      function.accept(value);
    }

    return this;
  }

  /**
   * Applies a given function to this {@link Fallible}'s error if it has an error; if it has a
   * value, nothing happens.
   *
   * <p>This function is very similar to {@link #orDo(Function) map} with the exception that the
   * value returned from the given function will be discarded, not used to replace the original
   * value contained inside this {@link Fallible}.
   *
   * @param function the {@link Function} to be applied to this {@link Fallible}'s error message.
   * @return {@link this}.
   */
  public final Fallible<Value> elseDo(Consumer<? super String> function) {
    if (hasError()) {
      function.accept(error);
    }

    return this;
  }
}
