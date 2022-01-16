package com.silong.foundation.duuid.server;

import com.silong.foundation.crypto.RootKey;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Duuid生成器服务
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-03 00:25
 */
@SpringBootApplication
@OpenAPIDefinition(
    info =
        @Info(
            title = "${spring.application.name}",
            version = "v1.0.0",
            contact =
                @Contact(email = "louis2sin@gmail.com", name = "Silong Technologies Co., Ltd."),
            license =
                @License(
                    name = "Apache License 2.0",
                    url = "https://www.apache.org/licenses/LICENSE-2.0"),
            description = "Distributed UUID Generation Service"))
public class DuuidServerApplication {

  static {
    RootKey.initialize();
  }

  /**
   * 服务启动入口
   *
   * @param args 参数服务
   */
  public static void main(String[] args) {
    SpringApplication.run(DuuidServerApplication.class, args);
  }
}
