package com.silong.foundation.duuid.server.configure;

import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * jackson定制自动装配
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-03 10:48
 */
@Configuration
public class JacksonAutoConfiguration {

  /**
   * 加载性能优化模块<br>
   * <pre>
   * For serialization (POJOs to JSON):
   *    Accessors for "getting" values (field access, calling getter method) are inlined using generated code instead of reflection
   *    Serializers for small number of 'primitive' types (int, long, String) are replaced with direct calls, instead of getting delegated to JsonSerializers
   *
   * For deserialization (JSON to POJOs)
   *    Calls to default (no-argument) constructors are byte-generated instead of using reflection
   *    Mutators for "setting" values (field access, calling setter method) are inlined using generated code instead of reflection
   *    Deserializers for small number of 'primitive' types (int, long, String) are replaced with direct calls, instead of getting delegated to JsonDeserializers
   * <pre>
   * @return afterburner
   */
  @Bean
  com.fasterxml.jackson.databind.Module afterburnerModule() {
    return new AfterburnerModule();
  }
}
