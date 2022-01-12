package com.silong.foundation.lambda;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 二元组
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-03 20:20
 * @param <T1> 二元组类型
 * @param <T2> 二元组类型
 */
@Data
@Builder
@Accessors(fluent = true)
public class Tuple2<T1, T2> {
  private final T1 t1;
  private final T2 t2;
}
