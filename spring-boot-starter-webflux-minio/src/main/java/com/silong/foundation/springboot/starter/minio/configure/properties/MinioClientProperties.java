package com.silong.foundation.springboot.starter.minio.configure.properties;

import static org.springframework.util.unit.DataUnit.MEGABYTES;

import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

/**
 * minio客户端配置
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2025-03-27 20:29
 */
@Data
@Validated
@ConfigurationProperties("minio.client")
public class MinioClientProperties {

  /** 文件处理临时保存目录，默认: java.io.tmpdir */
  @NotEmpty private String tempDir = System.getProperty("java.io.tmpdir");

  /** 临时文件保存目录，默认：java.io.tmpdir */
  @NotEmpty private String savingDir = System.getProperty("user.home");

  /** 服务端点 */
  @NotEmpty private String endpoint;

  /** ak */
  @NotEmpty private String accessKey;

  /** sk */
  @NotEmpty private String secretKey;

  /** obs区域，可以为null */
  @Nullable private String region;

  /** 分段阈值有效值为5MB---5GB，默认：5MB */
  @NotNull private DataSize partThreshold = DataSize.of(5, MEGABYTES);

  /** 连接超时，默认：30秒 */
  @NotNull private Duration connectionTimeout = Duration.ofSeconds(30);

  /** 写超时，默认：60秒 */
  @NotNull private Duration writeTimeout = Duration.ofSeconds(60);

  /** 写超时，默认：60秒 */
  @NotNull private Duration readTimeout = Duration.ofSeconds(60);

  /** 连接池配置 */
  @NotNull @Valid @NestedConfigurationProperty private Pool connectionPool = new Pool();

  /** 连接池配置 */
  @Data
  public static class Pool {
    /** 最大空闲连接数，默认：10 */
    @Positive private int maxIdleConnections = 10;

    /** 连接保持活跃的时间，默认：5分钟 */
    @NotNull private Duration keepAliveDuration = Duration.ofSeconds(300);
  }

  /**
   * 是否开启的sll or tls 加密通讯
   *
   * @return true or false
   */
  public boolean isSecure() {
    return endpoint.startsWith("https://");
  }
}
