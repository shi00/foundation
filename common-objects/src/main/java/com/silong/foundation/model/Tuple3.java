package com.silong.foundation.model;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 三元组
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-03 20:20
 * @param <T1> 三元组类型
 * @param <T2> 三元组类型
 * @param <T3> 三元组类型
 */
@Data
@Builder
@Accessors(fluent = true)
public class Tuple3<T1, T2, T3> {
  private final T1 t1;
  private final T2 t2;
  private final T3 t3;
}
