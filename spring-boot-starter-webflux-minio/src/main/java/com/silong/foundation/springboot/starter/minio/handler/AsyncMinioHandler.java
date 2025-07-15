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

import static com.silong.foundation.springboot.starter.minio.handler.FileUtils.deleteRecursively;
import static com.silong.foundation.springboot.starter.minio.handler.MethodWrapper.closeQuietly;
import static com.silong.foundation.springboot.starter.minio.handler.MethodWrapper.doOnTerminated;
import static org.springframework.util.StringUtils.hasLength;

import com.silong.foundation.springboot.starter.minio.configure.properties.MinioClientProperties;
import com.silong.foundation.springboot.starter.minio.exceptions.*;
import io.minio.*;
import io.minio.messages.Bucket;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import jakarta.annotation.Nullable;
import java.io.*;
import java.nio.file.*;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.UUID;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuple4;
import reactor.util.function.Tuples;

/**
 * minio文件上传下载文件处理器
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2025-03-27 21:33
 */
@Slf4j
public class AsyncMinioHandler {

  private static final DateTimeFormatter FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private final MinioClientProperties minioClientProperties;

  private final MethodWrapper wrapper;

  /**
   * 构造方法
   *
   * @param minioAsyncClient 客户端
   * @param minioClientProperties 配置
   */
  public AsyncMinioHandler(
      @NonNull MinioAsyncClient minioAsyncClient,
      @NonNull MinioClientProperties minioClientProperties) {
    this.minioClientProperties = minioClientProperties;
    this.wrapper = new MethodWrapper(minioAsyncClient);
  }

  /**
   * 设置桶策略
   *
   * @param bucket 桶名
   */
  public Mono<String> getBucketPolicy(String bucket) {
    return Mono.just(GetBucketPolicyArgs.builder().bucket(bucket).build())
        .flatMap(wrapper::getBucketPolicy)
        .doOnSuccess(
            policyJson ->
                log.info("Successfully get policy:{} from bucket:{}.", policyJson, bucket))
        .doOnError(t -> log.error("Failed to get policy from bucket:{}.", bucket));
  }

  /**
   * 设置桶策略
   *
   * @param bucket 桶名
   * @param policyJson 策略json
   */
  public Mono<Boolean> setBucketPolicy(String bucket, String policyJson) {
    return Mono.just(SetBucketPolicyArgs.builder().bucket(bucket).config(policyJson).build())
        .flatMap(wrapper::setBucketPolicy)
        .doOnSuccess(v -> log.info("Successfully set policy:{} for bucket:{}.", policyJson, bucket))
        .doOnError(t -> log.error("Failed to set policy:{} for bucket:{}.", policyJson, bucket));
  }

  /**
   * 上传文件到指定桶
   *
   * @param bucket 桶名
   * @param object 对象名
   * @param file 文件
   * @return ObjectWriteResponse
   */
  public Mono<ObjectWriteResponse> upload(String bucket, String object, File file) {
    return makeBucket(bucket)
        .flatMap(v -> uploadObjet(bucket, object, file))
        .flatMap(
            resp -> {
              Path path = file.toPath();
              return wrapper
                  .checkIntegrity(
                      bucket,
                      object,
                      path,
                      minioClientProperties.getPartThreshold(),
                      resp.etag(),
                      false)
                  .zipWith(Mono.just(resp));
            })
        .doOnSuccess(
            resp ->
                log.info(
                    "Successfully uploaded {} to bucket {} with name of {}.",
                    file.getAbsolutePath(),
                    bucket,
                    object))
        .map(Tuple2::getT2)
        .doOnError(
            t ->
                log.error(
                    "Failed to upload {} to bucket {} with name of {}.",
                    file.getAbsolutePath(),
                    object,
                    bucket,
                    t));
  }

  private Mono<ObjectWriteResponse> uploadObjet(String bucket, String object, File file) {
    log.info(
        "Start uploading file:{} to bucket:{}, object:{}.", file.getAbsolutePath(), bucket, object);
    try {
      InputStream inputStream =
          new ProgressStream(
              "Uploading " + object,
              file.length(),
              log,
              new BufferedInputStream(new FileInputStream(file)));
      return wrapper
          .uploadObjet(
              PutObjectArgs.builder().bucket(bucket).object(object).stream(
                      inputStream, file.length(), minioClientProperties.getPartThreshold())
                  .build(),
              file)
          .doOnSuccess(
              resp ->
                  log.info(
                      "Successfully updated {} to {} with name {}.",
                      file.getAbsolutePath(),
                      bucket,
                      object))
          .doOnError(
              t ->
                  log.error(
                      "Failed to upload {} from local to {} with name {}.",
                      file.getAbsolutePath(),
                      bucket,
                      object,
                      t))
          .doFinally(
              signalType ->
                  doOnTerminated(
                      signalType,
                      () -> {
                        closeQuietly(
                            inputStream,
                            tt ->
                                log.error(
                                    "Failed to close input stream of {}.",
                                    file.getAbsolutePath(),
                                    tt));
                        log.info("Closed input stream of {}.", file.getAbsolutePath());
                      }));

    } catch (FileNotFoundException e) {
      throw new UploadObjectException(bucket, object, file, e);
    }
  }

  /**
   * 从指定桶内下载对象
   *
   * @param bucket 桶名
   * @param object 对象名
   * @param saveDir 下载对象保存目录，不指定则使用默认配置的位置
   * @return 下载文件
   */
  public Mono<File> download(String bucket, String object, @Nullable String saveDir) {
    return getObjectStat(bucket, object) // 读取对象信息
        .map(
            resp ->
                Tuples.of(
                    resp.etag(),
                    resp.size(),
                    Paths.get(minioClientProperties.getTempDir())
                        .resolve(UUID.randomUUID().toString())))
        .doOnNext(
            t3 ->
                log.info(
                    "Start downloading from [bucket:{}, object:{}, eTag:{}, objectSize:{}] to tempDir:{}.",
                    bucket,
                    object,
                    t3.getT1(),
                    t3.getT2(),
                    t3.getT3()))
        .flatMap(t3 -> downloadObject(bucket, object).zipWith(Mono.just(t3)))
        .flatMap(t2 -> writeToFile(t2, object))
        .flatMap(
            t4 ->
                wrapper
                    .checkIntegrity(
                        bucket,
                        object,
                        t4.getT4(),
                        minioClientProperties.getPartThreshold(),
                        t4.getT1(),
                        true)
                    .zipWith(Mono.just(t4)))
        .flatMap(t2 -> move2Target(t2.getT2().getT4(), Path.of(getSavingDir(saveDir))))
        .doOnError(t -> log.error("Failed to download {} from {} to local.", object, bucket, t))
        .doOnSuccess(
            file ->
                log.info("Successfully downloaded {} from {} to local {}.", object, bucket, file));
  }

  private Mono<File> move2Target(Path tmpFile, Path target) {
    return wrapper
        .move2Target(tmpFile, target)
        .doOnSuccess(
            file ->
                log.info(
                    "Successfully moved {} to {}",
                    tmpFile.toFile().getAbsolutePath(),
                    file.getAbsolutePath()))
        .doOnError(
            t ->
                log.error(
                    "Failed to move {} to {}",
                    tmpFile.toFile().getAbsolutePath(),
                    target.toFile().getAbsolutePath(),
                    t));
  }

  /**
   * 数据写入文件
   *
   * @param tuple2 参数
   * @param object 对象名
   * @return 出参
   */
  private Mono<Tuple4<String, Long, Path, Path>> writeToFile(
      Tuple2<GetObjectResponse, Tuple3<String, Long, Path>> tuple2, String object) {
    Path parent = tuple2.getT2().getT3();
    Path targetFile = parent.resolve(object);
    return wrapper
        .write2File(tuple2, object, parent, targetFile)
        .doOnSuccess(
            t4 ->
                log.info(
                    "Successfully write {} to {}.", object, targetFile.toFile().getAbsolutePath()))
        .doOnError(
            t -> {
              deleteRecursively(parent);
              log.error(
                  "Failed to write {} to {}.", object, targetFile.toFile().getAbsolutePath(), t);
            });
  }

  /**
   * 下载对象
   *
   * @param bucket 桶名
   * @param object 对象名
   * @return 对象流
   */
  private Mono<GetObjectResponse> downloadObject(String bucket, String object) {
    return Mono.just(GetObjectArgs.builder().bucket(bucket).object(object).build())
        .doOnNext(
            args -> log.info("GetObjectArgs: [bucket:{}, object:{}]", args.bucket(), args.object()))
        .flatMap(wrapper::getObject)
        .doOnSuccess(
            resp -> log.info("Successfully downloaded {} from {}.", resp.object(), resp.bucket()))
        .doOnError(t -> log.error("Failed to download {} from {}.", object, bucket, t));
  }

  /**
   * 获取对象的元数据
   *
   * @param bucket 桶名
   * @param object 对象名
   * @return 对象信息
   */
  public Mono<StatObjectResponse> getObjectStat(String bucket, String object) {
    return Mono.just(StatObjectArgs.builder().bucket(bucket).object(object).build())
        .doOnNext(
            arg -> log.info("StatObjectArgs: [bucket:{}, object:{}]", arg.bucket(), arg.object()))
        .flatMap(wrapper::statObject)
        .doOnSuccess(resp -> log.info("Successfully obtained statObject: {}.", resp))
        .doOnError(
            t ->
                log.error(
                    "Failed to obtain statObject of bucket:{}, object:{}.", bucket, object, t));
  }

  /**
   * 删除指定桶内的对象
   *
   * @param bucket 桶名
   * @param object 对象名
   * @return 删除结果
   */
  public Mono<Boolean> removeObject(String bucket, String object) {
    return Mono.just(RemoveObjectArgs.builder().bucket(bucket).object(object).build())
        .doOnNext(
            arg -> log.info("RemoveObjectArgs: [bucket:{}, object:{}]", arg.bucket(), arg.object()))
        .flatMap(wrapper::removeObject)
        .doOnSuccess(v -> log.info("Successfully removed {} from {}.", object, bucket))
        .doOnError(t -> log.error("Failed to remove {} from {}.", object, bucket, t));
  }

  /**
   * 批量删除桶内对象
   *
   * @param bucket 桶名
   * @param objects 待删除对象列表
   * @return true or false
   */
  public Flux<DeleteError> removeObjects(String bucket, String... objects) {
    return Mono.just(
            RemoveObjectsArgs.builder()
                .bucket(bucket)
                .objects(Arrays.stream(objects).map(DeleteObject::new).toList())
                .build())
        .flatMapMany(wrapper::removeObjects)
        .doOnNext(e -> log.info("removeObjects: {}", e))
        .doOnError(t -> log.error("Failed to remove objects:{}.", objects, t));
  }

  /**
   * 列出指定桶内的所有对象
   *
   * @param bucket 桶名
   * @param recursive 是否递归查询
   * @return 对象列表
   */
  public Flux<Item> listBucketObjects(String bucket, boolean recursive) {
    return Mono.just(ListObjectsArgs.builder().bucket(bucket).recursive(recursive).build())
        .doOnNext(
            arg ->
                log.info(
                    "ListObjectsArgs: [bucket:{}, recursive:{}]", arg.bucket(), arg.recursive()))
        .flatMapMany(wrapper::listObjects)
        .doOnNext(
            item ->
                log.info(
                    "item[etag:{}, object:{}, lastModified:{}, owner:[{}, {}], size:{}, storageClass:{}, isLatest:{}, versionId:{}, userMetadata:{}, userTags:{}, isDir:{}]",
                    item.etag(),
                    item.objectName(),
                    item.lastModified().format(FORMATTER),
                    item.owner() == null ? null : item.owner().id(),
                    item.owner() == null ? null : item.owner().displayName(),
                    item.size(),
                    item.storageClass(),
                    item.isLatest(),
                    item.versionId(),
                    item.userMetadata(),
                    item.userTags(),
                    item.isDir()));
  }

  /**
   * 列出所有桶
   *
   * @return 桶列表
   * @throws ListBucketsException 异常
   */
  public Flux<Bucket> listBuckets() {
    return Mono.just(ListBucketsArgs.builder().build())
        .flatMapMany(wrapper::listBuckets)
        .sort(Comparator.comparing(Bucket::name, String::compareTo))
        .doOnNext(
            bucket ->
                log.info(
                    "bucket: {}, creationDate: {}",
                    bucket.name(),
                    bucket.creationDate().format(FORMATTER)))
        .doOnError(t -> log.error("Failed to list buckets.", t));
  }

  /**
   * 创建桶，如果桶存在则不创建否则创建
   *
   * @param bucket 桶名
   * @return true or false
   */
  public Mono<Boolean> makeBucket(String bucket) {
    return bucketExists(bucket)
        .flatMap(
            exist ->
                exist
                    ? Mono.just(Boolean.TRUE)
                    : Mono.just(MakeBucketArgs.builder().bucket(bucket).build())
                        .doOnNext(args -> log.info("MakeBucketArgs: bucket:{}", args.bucket()))
                        .flatMap(wrapper::makeBucket)
                        .doOnSuccess(bn -> log.info("Bucket {} created successfully.", bucket))
                        .doOnError(t -> log.error("Failed to create bucket: {}", bucket, t)));
  }

  /**
   * 检查桶是否存在
   *
   * @param bucket 桶名
   * @return true or false
   */
  public Mono<Boolean> bucketExists(String bucket) {
    return Mono.just(BucketExistsArgs.builder().bucket(bucket).build())
        .doOnNext(args -> log.info("BucketExistsArgs: bucket:{}.", args.bucket()))
        .flatMap(wrapper::bucketExists)
        .doOnSuccess(exist -> log.info("Bucket {} exists: {}", bucket, exist))
        .doOnError(t -> log.error("Failed to check bucket {} exists.", bucket, t));
  }

  /**
   * 删除桶
   *
   * @param bucket 桶名
   * @return true or false
   */
  public Mono<Boolean> removeBucket(String bucket) {
    return Mono.just(RemoveBucketArgs.builder().bucket(bucket).build())
        .doOnNext(args -> log.info("RemoveBucketArgs: bucket:{}", args.bucket()))
        .flatMap(wrapper::removeBucket)
        .doOnSuccess(v -> log.info("Successfully removed bucket {}.", bucket))
        .doOnError(t -> log.error("Failed to remove bucket {}.", bucket, t));
  }

  private String getSavingDir(String savingDir) {
    return hasLength(savingDir) ? savingDir : minioClientProperties.getSavingDir();
  }
}
