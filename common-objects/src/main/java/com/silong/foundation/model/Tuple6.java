package com.silong.foundation.model;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 六元组
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-03 20:20
 * @param <T1> 六元组类型
 * @param <T2> 六元组类型
 * @param <T3> 六元组类型
 * @param <T4> 六元组类型
 * @param <T5> 六元组类型
 * @param <T6> 六元组类型
 */
@Data
@Builder
@Accessors(fluent = true)
public class Tuple6<T1, T2, T3, T4, T5, T6> {
  private final T1 t1;
  private final T2 t2;
  private final T3 t3;
  private final T4 t4;
  private final T5 t5;
  private final T6 t6;
}
