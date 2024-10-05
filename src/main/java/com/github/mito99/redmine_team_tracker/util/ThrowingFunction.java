package com.github.mito99.redmine_team_tracker.util;

import java.util.function.Function;

@FunctionalInterface
public interface ThrowingFunction<T, R, E extends Exception> {
  R apply(T t) throws E;

  public static <T, R, E extends Exception> Function<T, R> tryFunction(
      ThrowingFunction<T, R, E> func) {
    return t -> {
      try {
        return func.apply(t);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    };
  }
}
