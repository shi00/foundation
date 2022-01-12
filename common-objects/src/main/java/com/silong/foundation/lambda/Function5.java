package com.silong.foundation.lambda;

/**
 * Represents a function that accepts five arguments and produces a result. This is the five-arity
 * specialization of Function.
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-012 16:23
 * @param <T1> the type of the first argument to the function
 * @param <T2> the type of the second argument to the function
 * @param <T3> the type of the third argument to the function
 * @param <T4> the type of the fourth argument to the function
 * @param <T5> the type of the fifth argument to the function
 * @param <R> the type of the result of the function
 */
public interface Function5<T1, T2, T3, T4, T5, R> {
  /**
   * apply
   *
   * @param t1 param1
   * @param t2 param2
   * @param t3 param3
   * @param t4 param4
   * @param t5 param5
   * @return result
   */
  R apply(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5);
}
