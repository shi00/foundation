package com.silong.foundation.model;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 四元组
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-03 20:20
 * @param <T1> 四元组类型
 * @param <T2> 四元组类型
 * @param <T3> 四元组类型
 * @param <T4> 四元组类型
 */
@Data
@Builder
@Accessors(fluent = true)
public class Tuple4<T1, T2, T3, T4> {
  private T1 t1;
  private T2 t2;
  private T3 t3;
  private T4 t4;
}
