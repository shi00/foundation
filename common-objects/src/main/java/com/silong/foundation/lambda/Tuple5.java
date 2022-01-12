package com.silong.foundation.lambda;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 五元组
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-03 20:20
 * @param <T1> 五元组类型
 * @param <T2> 五元组类型
 * @param <T3> 五元组类型
 * @param <T4> 五元组类型
 * @param <T5> 五元组类型
 */
@Data
@Builder
@Accessors(fluent = true)
public class Tuple5<T1, T2, T3, T4, T5> {
  private final T1 t1;
  private final T2 t2;
  private final T3 t3;
  private final T4 t4;
  private final T5 t5;
}
