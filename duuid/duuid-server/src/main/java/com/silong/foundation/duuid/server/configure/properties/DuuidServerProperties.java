package com.silong.foundation.duuid.server.configure.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotEmpty;

/**
 * 服务配置
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-03 11:31
 */
@Data
@Validated
@ConfigurationProperties(prefix = "duuid.server")
public class DuuidServerProperties {
  /** 服务端点路径，默认：/duuid */
  @NotEmpty private String servicePath = "/duuid";
}
