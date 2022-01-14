package com.silong.foundation.duuid.server;

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
public class DuuidServerApplication {

  /**
   * 服务启动入口
   *
   * @param args 参数服务
   */
  public static void main(String[] args) {
    SpringApplication.run(DuuidServerApplication.class, args);
  }
}
