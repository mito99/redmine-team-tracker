package jp.hitachi.mineo.tool.redmine_team_tracker.util;

import java.util.function.Consumer;

@FunctionalInterface
public interface ThrowingConsumer<T, E extends Exception> {
  void accept(T t) throws E;

  public static <T, E extends Exception> Consumer<T> tryConsumer(ThrowingConsumer<T, E> c) {
    return t -> {
      try {
        c.accept(t);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    };
  }
}
