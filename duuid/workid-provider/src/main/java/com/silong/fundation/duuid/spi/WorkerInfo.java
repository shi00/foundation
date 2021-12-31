package com.silong.fundation.duuid.spi;

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
public class WorkerInfo {
  /** worker name */
  private final String name;

  /** 附加信息 */
  private final Map<String, String> extraInfo;
}
