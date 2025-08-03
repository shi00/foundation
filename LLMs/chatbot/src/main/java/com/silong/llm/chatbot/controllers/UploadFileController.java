/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one
 *  * or more contributor license agreements.  See the NOTICE file
 *  * distributed with this work for additional information
 *  * regarding copyright ownership.  The ASF licenses this file
 *  * to you under the Apache License, Version 2.0 (the
 *  * "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  * KIND, either express or implied.  See the License for the
 *  * specific language governing permissions and limitations
 *  * under the License.
 *
 */

package com.silong.llm.chatbot.controllers;

import static com.silong.foundation.springboot.starter.jwt.common.Constants.ACCESS_TOKEN;
import static java.util.Objects.requireNonNull;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

import com.silong.foundation.springboot.starter.jwt.common.ErrorDetail;
import com.silong.foundation.springboot.starter.minio.handler.AsyncMinioHandler;
import com.silong.llm.chatbot.configure.properties.UploadFileProperties;
import com.silong.llm.chatbot.pos.UploadResult;
import io.minio.ObjectWriteResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

/**
 * 文件上传控制器
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2025-05-19 20:50
 */
@Slf4j
@RestController
public class UploadFileController {

  private final AsyncMinioHandler minioHandler;

  private final UploadFileProperties properties;

  /**
   * 构造方法
   *
   * @param minioHandler 处理器
   */
  public UploadFileController(
      @NonNull AsyncMinioHandler minioHandler, @NonNull UploadFileProperties properties) {
    this.minioHandler = minioHandler;
    this.properties = properties;
  }

  /**
   * 文件上传
   *
   * @param files 上传文件列表
   * @return 上传结果
   */
  @Operation(
      operationId = "upload",
      summary = "Batch upload files.",
      tags = {"Chat"},
      security = {@SecurityRequirement(name = ACCESS_TOKEN)},
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = UploadResult.class))),
        @ApiResponse(
            responseCode = "401",
            description = "UNAUTHORIZED",
            content = @Content(schema = @Schema(implementation = ErrorDetail.class))),
        @ApiResponse(
            responseCode = "403",
            description = "FORBIDDEN",
            content = @Content(schema = @Schema(implementation = ErrorDetail.class))),
        @ApiResponse(
            responseCode = "500",
            description = "INTERNAL_SERVER_ERROR",
            content = @Content(schema = @Schema(implementation = ErrorDetail.class)))
      })
  @PostMapping(value = "/upload/files", consumes = MULTIPART_FORM_DATA_VALUE)
  public Mono<ResponseEntity<UploadResult>> uploadMultipleFiles(
      @RequestPart("files") MultipartFile[] files) {
    return Flux.fromArray(files)
        .map(
            file ->
                Tuples.of(
                    Path.of(properties.getFileStorageDirectory()),
                    requireNonNull(
                        file.getOriginalFilename(),
                        "The file name of the uploaded must not be null."),
                    getInputStream(file)))
        .flatMap(
            t3 ->
                minioHandler.upload(
                    properties.getObsBucket(), t3.getT2(), write2File(t3.getT1(), t3.getT3())))
        .collectList()
        .map(
            list -> {
              var result = new UploadResult();
              List<String> fs = new ArrayList<>(list.size());
              result.setFiles(fs);
              result.setBucket(list.getFirst().bucket());
              for (ObjectWriteResponse resp : list) {
                assert result.getBucket().equals(resp.bucket());
                result.setBucket(resp.bucket());
                fs.add(resp.object());
              }
              return ResponseEntity.ok(result);
            })
        .doOnSuccess(rs -> log.info("File list uploaded successfully. {}", rs.getBody()));
  }

  @SneakyThrows(IOException.class)
  private static File write2File(Path tempDir, InputStream inputStream) {
    var path = tempDir.resolve(UUID.randomUUID().toString());
    try (inputStream;
        OutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(path))) {
      inputStream.transferTo(outputStream);
      return path.toFile();
    }
  }

  @SneakyThrows(IOException.class)
  private static InputStream getInputStream(MultipartFile file) {
    return file.getInputStream();
  }
}
