package com.silong.foundation.duuid.server.configure;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * prometheus 自动装配
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-15 14:52
 */
@Configuration
public class PrometheusAutoConfiguration {
  @Bean
  TimedAspect timedAspect(MeterRegistry registry) {
    return new TimedAspect(registry);
  }
}
