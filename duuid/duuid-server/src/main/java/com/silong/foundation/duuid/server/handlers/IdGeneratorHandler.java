package com.silong.foundation.duuid.server.handlers;

import com.silong.foundation.duuid.generator.DuuidGenerator;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import static org.springframework.http.MediaType.APPLICATION_JSON;

/**
 * id生成器处理器，配合webflux使用
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-03 12:28
 */
public class IdGeneratorHandler implements HandlerFunction<ServerResponse> {

  /** id生成器 */
  private final DuuidGenerator duuidGenerator;

  /**
   * 构造方法
   *
   * @param duuidGenerator id生成器
   */
  public IdGeneratorHandler(DuuidGenerator duuidGenerator) {
    this.duuidGenerator = duuidGenerator;
  }

  @Override
  public Mono<ServerResponse> handle(ServerRequest request) {
    return ServerResponse.ok()
        .contentType(APPLICATION_JSON)
        .body(BodyInserters.fromValue(duuidGenerator.nextId()));
  }
}
