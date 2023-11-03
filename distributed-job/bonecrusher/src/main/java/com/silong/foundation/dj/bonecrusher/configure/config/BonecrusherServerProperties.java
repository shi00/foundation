/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one
 *  * or more contributor license agreements.  See the NOTICE file
 *  * distributed with this work for additional information
 *  * regarding copyright ownership.  The ASF licenses this file
 *  * to you under the Apache License, Version 2.0 (the
 *  * "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  * KIND, either express or implied.  See the License for the
 *  * specific language governing permissions and limitations
 *  * under the License.
 *
 */

package com.silong.foundation.dj.bonecrusher.configure.config;

import static io.netty.handler.logging.LogLevel.INFO;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.springframework.util.unit.DataUnit.KILOBYTES;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.nio.file.Path;
import java.time.Duration;
import lombok.*;
import org.apache.commons.lang3.SystemUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.boot.convert.DataSizeUnit;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

/**
 * UDT 客户端配置
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2023-10-19 11:12
 */
@Data
@Validated
@ConfigurationProperties(prefix = "bonecrusher.server")
public class BonecrusherServerProperties {

  /** 读写空闲事件触发配置 */
  @Data
  public static class IdleStateProperties {
    /** 当指定时间内没有执行任何读取操作时，将触发状态为 IdleState.READER_IDLE 的 IdleStateEvent。指定 0 禁用。 */
    @NotNull
    @DurationUnit(SECONDS)
    private Duration readerIdleTime = Duration.ZERO;

    /** 当指定时间内没有执行任何写入操作时，将触发状态为 IdleState.WRITER_IDLE 的 IdleStateEvent。指定 0 禁用。 */
    @NotNull
    @DurationUnit(SECONDS)
    private Duration writerIdleTime = Duration.ZERO;

    /** 当指定时间段内没有进行读写操作时，会触发状态为 IdleState.ALL_IDLE 的 IdleStateEvent。指定 0 禁用。 */
    @NotNull
    @DurationUnit(SECONDS)
    private Duration allIdleTime = Duration.ZERO;

    /** 在评估写空闲时是否应考虑字节消耗。默认为 false。 */
    private boolean observeOutput;
  }

  /** netty相关调优配置 */
  @Data
  public static class NettyTuningProperties {
    /** SO_REUSEADDR，默认：true */
    private boolean SO_REUSEADDR = true;

    /** CONNECT_TIMEOUT_MILLIS，默认：5秒 */
    @NotNull
    @DurationUnit(SECONDS)
    private Duration CONNECT_TIMEOUT_MILLIS = Duration.of(5, SECONDS);
  }

  /**
   * UDT Server监听地址，目前支持ipv4<br>
   * 默认：127.0.0.1
   */
  @NotEmpty private String address = "127.0.0.1";

  // 服务监听端口，默认：6118
  @Max(65535)
  @Min(1025)
  private int port = 6118;

  /** Boss Group线程池线程命名前缀，默认：BS-Acceptor */
  @NotEmpty private String bossGroupPoolName = "BS-Acceptor";

  /** Connector Group线程池线程命名前缀，默认：BS-Connector */
  @NotEmpty private String connectorGroupPoolName = "BS-Connector";

  /** 文件传输时，数据分块大小，默认：8KB */
  @NotNull
  @DataSizeUnit(KILOBYTES)
  private DataSize dataBlockSize = DataSize.ofKilobytes(8);

  /** 数据存储目录，默认：java.io.tmpdir */
  @NotNull private Path dataStorePath = SystemUtils.getJavaIoTmpDir().toPath();

  /** Netty server bossGroup，默认：1 */
  @Positive private int bossGroupThreads = 1;

  /** Netty server workerGroup，默认：可用cpu核数 */
  @Positive private int workerGroupThreads = Runtime.getRuntime().availableProcessors();

  /** 日志级别，默认：INFO */
  @NotNull private io.netty.handler.logging.LogLevel LogLevel = INFO;

  /** netty调优配置 */
  @Valid @NestedConfigurationProperty
  private NettyTuningProperties netty = new NettyTuningProperties();

  /** 服务器空闲事件触发配置 */
  @Valid @NestedConfigurationProperty
  private IdleStateProperties idleState = new IdleStateProperties();
}
