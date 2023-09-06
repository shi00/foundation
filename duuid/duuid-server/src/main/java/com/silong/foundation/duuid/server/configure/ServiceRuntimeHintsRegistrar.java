package com.silong.foundation.duuid.server.configure;

import io.netty.channel.epoll.EpollChannelOption;
import lombok.SneakyThrows;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * 本地镜像构建提示信息
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-13 21:23
 */
class ServiceRuntimeHintsRegistrar implements RuntimeHintsRegistrar {
  @Override
  @SneakyThrows
  public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
    ReflectionHints reflection = hints.reflection();
    reflection.registerField(EpollChannelOption.class.getField("TCP_USER_TIMEOUT"));
    //    reflection.registerConstructor(DoubleHis);
  }
}
