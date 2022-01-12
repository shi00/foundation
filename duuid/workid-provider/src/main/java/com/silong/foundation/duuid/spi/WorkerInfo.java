package com.silong.foundation.duuid.spi;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * Id分配器信息
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2021-12-30 21:50
 */
@Data
@Builder
public class WorkerInfo {
  /** worker name */
  private final String name;

  /** 附加信息 */
  private final Map<String, String> extraInfo;
}
