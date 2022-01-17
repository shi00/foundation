package com.silong.foundation.duuid.server.configure.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Positive;
import java.util.Map;
import java.util.Set;

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

  /** http请求头内签名字段，默认：Signature */
  @NotEmpty private String httpHeaderSignature = "Signature";

  /** 客户端和服务端可接受的时间差，默认：10000毫秒 */
  @Positive private int acceptableTimeDiffMills = 10000;

  /** http请求头内时间戳字段，默认：Timestamp */
  @NotEmpty private String httpHeaderTimestamp = "Timestamp";

  /** http请求头内身份标识字段，默认：Identifier */
  @NotEmpty private String httpHeaderIdentifier = "Identifier";

  /** http请求头内随机字符串字段，默认：Random */
  @NotEmpty private String httpHeaderRandom = "Random";

  /** 使用HmacSha256签名使用的密钥 */
  @NotEmpty private String workKey;

  /** 鉴权白名单 */
  @Valid @NotEmpty private Set<@NotEmpty String> authWhiteList;

  /** 鉴权名单 */
  @Valid @NotEmpty private Set<@NotEmpty String> authList;

  /** 用户到角色映射 */
  @Valid @NotEmpty
  private Map<@NotEmpty String, @Valid @NotEmpty Set<@NotEmpty String>> userRolesMappings;

  /** 角色到请求路径映射 */
  @Valid @NotEmpty
  private Map<@NotEmpty String, @NotEmpty @Valid Set<@NotEmpty String>> rolePathsMappings;
}
