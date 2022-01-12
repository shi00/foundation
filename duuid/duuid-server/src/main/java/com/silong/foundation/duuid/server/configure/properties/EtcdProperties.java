package com.silong.foundation.duuid.server.configure.properties;

import lombok.Builder;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.EMPTY;

/**
 * 基于etcdv3的workerId分配器配置
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-11 22:42
 */
@Data
@Builder
@Validated
@ConfigurationProperties(prefix = "duuid.worker-id-provider.etcdv3")
public class EtcdProperties {
  /** 是否启用 */
  private boolean enabled;

  /** 服务器地址 */
  @NotEmpty @Valid private List<@NotBlank String> serverAddresses;

  /**
   * Trusted certificates for verifying the remote endpoint's certificate. The file should contain
   * an X.509 certificate collection in PEM format.
   */
  @Builder.Default private String trustCertCollectionFile = EMPTY;

  /** a PKCS#8 private key file in PEM format */
  @Builder.Default private String keyFile = EMPTY;

  /** an X.509 certificate chain file in PEM format */
  @Builder.Default private String keyCertChainFile = EMPTY;

  /** 用户名 */
  @Builder.Default private String userName = EMPTY;

  /** 密码 */
  @Builder.Default private String password = EMPTY;
}
