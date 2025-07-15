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

package com.silong.foundation.springboot.starter.minio.handler;

import static com.silong.foundation.springboot.starter.minio.handler.FileUtils.*;
import static java.nio.file.StandardCopyOption.*;
import static java.nio.file.StandardOpenOption.*;

import com.silong.foundation.springboot.starter.minio.exceptions.*;
import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.Bucket;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.function.Consumer;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuple4;
import reactor.util.function.Tuples;

/**
 * 封装工具
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2025-03-27 21:33
 */
@Slf4j
record MethodWrapper(MinioAsyncClient minioAsyncClient) {

  /**
   * 构造方法
   *
   * @param minioAsyncClient 客户端
   */
  MethodWrapper {}

  Mono<Boolean> bucketExists(BucketExistsArgs args) {
    return Mono.fromCallable(() -> minioAsyncClient.bucketExists(args))
        .flatMap(Mono::fromFuture)
        .subscribeOn(Schedulers.boundedElastic())
        .onErrorMap(
            MethodWrapper::isMinioException, t -> new CheckBucketExistException(args.bucket(), t));
  }

  Flux<Bucket> listBuckets(ListBucketsArgs args) {
    return Mono.fromCallable(() -> minioAsyncClient.listBuckets(args))
        .flatMap(Mono::fromFuture)
        .flatMapMany(Flux::fromIterable)
        .subscribeOn(Schedulers.boundedElastic())
        .onErrorMap(MethodWrapper::isMinioException, ListBucketsException::new);
  }

  Mono<Boolean> removeBucket(RemoveBucketArgs args) {
    return Mono.fromCallable(() -> minioAsyncClient.removeBucket(args).thenApply(v -> Boolean.TRUE))
        .flatMap(Mono::fromFuture)
        .subscribeOn(Schedulers.boundedElastic())
        .onErrorMap(
            MethodWrapper::isMinioException, t -> new RemoveBucketException(args.bucket(), t));
  }

  Flux<Item> listObjects(ListObjectsArgs args) {
    return Mono.fromCallable(() -> minioAsyncClient.listObjects(args))
        .flatMapMany(Flux::fromIterable)
        .map(MethodWrapper::getResult)
        .subscribeOn(Schedulers.boundedElastic());
  }

  Mono<Boolean> removeObject(RemoveObjectArgs args) {
    return Mono.fromCallable(() -> minioAsyncClient.removeObject(args).thenApply(v -> Boolean.TRUE))
        .flatMap(Mono::fromFuture)
        .subscribeOn(Schedulers.boundedElastic())
        .onErrorMap(
            MethodWrapper::isMinioException,
            t -> new RemoveObjectException(args.bucket(), args.object(), t));
  }

  Flux<DeleteError> removeObjects(RemoveObjectsArgs args) {
    return Flux.fromIterable(minioAsyncClient.removeObjects(args))
        .map(MethodWrapper::getResult)
        .onErrorMap(
            MethodWrapper::isMinioException,
            t ->
                new RemoveObjectsException(
                    args.bucket(),
                    Flux.fromIterable(args.objects())
                        .map(
                            e -> {
                              try {
                                Field nameField = DeleteObject.class.getDeclaredField("name");
                                nameField.setAccessible(true); // 允许访问私有字段
                                return (String) nameField.get(e);
                              } catch (NoSuchFieldException | IllegalAccessException ex) {
                                log.error(
                                    "Failed to get the field name from DeleteObject.class.", ex);
                                return "";
                              }
                            })
                        .toStream()
                        .toArray(String[]::new),
                    t))
        .subscribeOn(Schedulers.boundedElastic());
  }

  Mono<StatObjectResponse> statObject(StatObjectArgs args) {
    return Mono.fromCallable(() -> minioAsyncClient.statObject(args))
        .flatMap(Mono::fromFuture)
        .subscribeOn(Schedulers.boundedElastic())
        .onErrorMap(
            MethodWrapper::isMinioException,
            t -> new GetObjectStatException(args.bucket(), args.object(), t));
  }

  Mono<GetObjectResponse> getObject(GetObjectArgs args) {
    return Mono.fromCallable(() -> minioAsyncClient.getObject(args))
        .flatMap(Mono::fromFuture)
        .subscribeOn(Schedulers.boundedElastic())
        .onErrorMap(
            MethodWrapper::isMinioException,
            t -> new GetObjectException(args.bucket(), args.object(), t));
  }

  Mono<Boolean> makeBucket(MakeBucketArgs args) {
    return Mono.fromCallable(() -> minioAsyncClient.makeBucket(args).thenApply(v -> Boolean.TRUE))
        .flatMap(Mono::fromFuture)
        .subscribeOn(Schedulers.boundedElastic())
        .onErrorMap(
            MethodWrapper::isMinioException, t -> new MakeBucketException(args.bucket(), t));
  }

  Mono<Tuple4<String, Long, Path, Path>> write2File(
      Tuple2<GetObjectResponse, Tuple3<String, Long, Path>> tuple2,
      String object,
      Path parent,
      Path targetFile) {
    return Mono.fromCallable(
            () -> {
              createDirectories(parent);
              try (ProgressStream input =
                      new ProgressStream(
                          "Downloading " + object, tuple2.getT2().getT2(), log, tuple2.getT1());
                  OutputStream out =
                      new BufferedOutputStream(
                          Files.newOutputStream(targetFile, CREATE, WRITE, TRUNCATE_EXISTING))) {
                input.transferTo(out);
              }
              return Tuples.of(tuple2.getT2().getT1(), tuple2.getT2().getT2(), parent, targetFile);
            })
        .subscribeOn(Schedulers.boundedElastic())
        .onErrorMap(IOException.class, e -> new WriteObject2FileException(targetFile, object, e));
  }

  Mono<File> move2Target(Path tmpFile, Path target) {
    return Mono.fromCallable(
            () -> {
              createDirectories(target);
              Path targetFile = target.resolve(tmpFile.getFileName());
              Files.move(tmpFile, targetFile, ATOMIC_MOVE, REPLACE_EXISTING);
              return targetFile.toFile();
            })
        .subscribeOn(Schedulers.boundedElastic()) // 指定使用IO调度器
        .onErrorMap(
            IOException.class,
            e ->
                new MoveFileException(
                    String.format(
                        "Failed to move %s to %s.",
                        tmpFile.toFile().getAbsolutePath(), target.toFile().getAbsolutePath()),
                    e));
  }

  Mono<Boolean> checkIntegrity(
      String bucket, String object, Path path, long partSize, String eTag, boolean deleteFile) {
    return Mono.fromCallable(
            () -> {
              String md5 = calculateETag(path.toFile(), partSize);
              if (!md5.equals(eTag)) {
                if (deleteFile) {
                  deleteRecursively(path);
                }
                throw new CheckIntegrityException(
                    String.format(
                        "MD5 checksum mismatch: [%s --- MD5: %s] vs obs[bucket:%s / object:%s --- eTag:%s]",
                        path.toFile().getAbsolutePath(), md5, bucket, object, eTag));
              }
              return Boolean.TRUE;
            })
        .subscribeOn(Schedulers.boundedElastic())
        .onErrorMap(
            IOException.class,
            e ->
                new CheckIntegrityException(
                    String.format(
                        "Failed to calculate MD5 of %s.", path.toFile().getAbsolutePath()),
                    e))
        .doOnSuccess(
            e ->
                log.info(
                    "MD5 checksum matched: [{}] vs obs[bucket:{} / object:{} / eTag:{}]",
                    path.toFile().getAbsolutePath(),
                    bucket,
                    object,
                    eTag))
        .doOnError(
            t -> {
              if (deleteFile) {
                deleteRecursively(path);
              }
              log.error("Failed to pass the integrity check.", t);
            });
  }

  Mono<ObjectWriteResponse> uploadObjet(PutObjectArgs args, File file) {
    return Mono.fromCallable(() -> minioAsyncClient.putObject(args))
        .flatMap(Mono::fromFuture)
        .onErrorMap(
            MethodWrapper::isMinioException,
            t -> new UploadObjectException(args.bucket(), args.object(), file, t));
  }

  Mono<Boolean> setBucketPolicy(SetBucketPolicyArgs args) {
    return Mono.fromCallable(
            () -> minioAsyncClient.setBucketPolicy(args).thenApply(v -> Boolean.TRUE))
        .flatMap(Mono::fromFuture)
        .onErrorMap(
            MethodWrapper::isMinioException, t -> new SetBucketPolicyException(args.bucket(), t));
  }

  Mono<String> getBucketPolicy(GetBucketPolicyArgs args) {
    return Mono.fromCallable(() -> minioAsyncClient.getBucketPolicy(args))
        .flatMap(Mono::fromFuture)
        .onErrorMap(
            MethodWrapper::isMinioException, t -> new GetBucketPolicyException(args.bucket(), t));
  }

  private static boolean isMinioException(Throwable t) {
    return true;
  }

  static void closeQuietly(InputStream inputStream, Consumer<Throwable> logIOException) {
    if (inputStream != null) {
      try {
        inputStream.close();
      } catch (IOException e) {
        logIOException.accept(e);
      }
    }
  }

  @SneakyThrows({
    ErrorResponseException.class,
    IllegalArgumentException.class,
    InsufficientDataException.class,
    InternalException.class,
    InvalidKeyException.class,
    InvalidResponseException.class,
    IOException.class,
    NoSuchAlgorithmException.class,
    ServerException.class,
    XmlParserException.class
  })
  private static <T> T getResult(Result<T> result) {
    return result.get();
  }

  /**
   * 反应流执行结束时执行某个动作，结束有三种状态：ON_COMPLETE(正常结束)，ON_ERROR(异常结束)，CANCEL(取消执行)
   *
   * @param signalType 信号类型
   * @param action 动作
   */
  static void doOnTerminated(SignalType signalType, Runnable action) {
    if (SignalType.CANCEL == signalType
        || SignalType.ON_COMPLETE == signalType
        || SignalType.ON_ERROR == signalType) {
      action.run();
    }
  }
}
