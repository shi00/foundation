package com.silong.foundation.duuid.server.handlers;

import com.silong.foundation.duuid.generator.DuuidGenerator;
import com.silong.foundation.duuid.server.model.Duuid;
import io.micrometer.core.annotation.Timed;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import static com.silong.foundation.constants.CommonErrorCode.SERVICE_INTERNAL_ERROR;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.MediaType.APPLICATION_JSON;

/**
 * id生成器处理器，配合webflux使用
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-03 12:28
 */
@Slf4j
public class IdGeneratorHandler implements HandlerFunction<ServerResponse> {

  private static final ThreadLocal<Duuid> DUUID_THREAD_LOCAL = ThreadLocal.withInitial(Duuid::new);

  /** 服务名 */
  private final String serviceName;

  /** id生成器 */
  private final DuuidGenerator duuidGenerator;

  /**
   * 构造方法
   *
   * @param serviceName 服务名
   * @param duuidGenerator id生成器
   */
  public IdGeneratorHandler(@NonNull String serviceName, @NonNull DuuidGenerator duuidGenerator) {
    this.serviceName = serviceName;
    this.duuidGenerator = duuidGenerator;
  }

  /**
   * 根据请求生成uuid
   *
   * @param request 请求
   * @return 响应
   */
  @Override
  @Timed(value = "nextId.time", description = "Time taken to return duuid.")
  public Mono<ServerResponse> handle(ServerRequest request) {
    return ServerResponse.ok()
        .contentType(APPLICATION_JSON)
        .body(BodyInserters.fromValue(DUUID_THREAD_LOCAL.get().id(duuidGenerator.nextId())))
        .onErrorResume(
            t -> {
              log.error("Failed to generate id.", t);
              return ServerResponse.status(INTERNAL_SERVER_ERROR)
                  .contentType(APPLICATION_JSON)
                  .body(BodyInserters.fromValue(SERVICE_INTERNAL_ERROR.format(serviceName)));
            });
  }
}
