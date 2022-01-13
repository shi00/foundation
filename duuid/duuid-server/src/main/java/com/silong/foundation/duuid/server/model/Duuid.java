package com.silong.foundation.duuid.server.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * duuid结果
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-13 13:17
 */
@Data
@NoArgsConstructor
@Accessors(fluent = true)
public class Duuid {
  /** 生成的duuid */
  @JsonProperty private long id;
}
