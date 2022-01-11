package com.silong.fundation.duuidserver.configure.properties;

import lombok.Builder;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;

/**
 * 服务配置
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-03 11:31
 */
@Data
@Builder
@Validated
@ConfigurationProperties(prefix = "duuid.server")
public class DuuidServerProperties {
  /** 服务访问路径，默认：/duuid */
  @Builder.Default @NotBlank private String path = "/duuid";
}
