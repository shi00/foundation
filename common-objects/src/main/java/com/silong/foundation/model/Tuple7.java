package com.silong.foundation.model;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 七元组
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-03 20:20
 * @param <T1> 七元组类型
 * @param <T2> 七元组类型
 * @param <T3> 七元组类型
 * @param <T4> 七元组类型
 * @param <T5> 七元组类型
 * @param <T6> 七元组类型
 * @param <T7> 七元组类型
 */
@Data
@Builder
@Accessors(fluent = true)
public class Tuple7<T1, T2, T3, T4, T5, T6, T7> {
  private T1 t1;
  private T2 t2;
  private T3 t3;
  private T4 t4;
  private T5 t5;
  private T6 t6;
  private T7 t7;
}
