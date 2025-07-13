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

package com.silong.foundation.springboot.starter.minio;

import static com.google.common.io.Files.toByteArray;
import static java.nio.file.Files.*;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.springframework.util.DigestUtils.md5DigestAsHex;

import com.silong.foundation.springboot.starter.minio.configure.MinioClientAutoConfiguration;
import com.silong.foundation.springboot.starter.minio.configure.properties.MinioClientProperties;
import com.silong.foundation.springboot.starter.minio.exceptions.UploadObjectException;
import com.silong.foundation.springboot.starter.minio.handler.AsyncMinioHandler;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.unit.DataSize;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;

/**
 * 测试类
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2025-03-27 21:33
 */
@SpringBootTest(classes = {MinioClientAutoConfiguration.class})
@TestPropertySource("classpath:application-test.properties")
@Testcontainers
@Slf4j
public class MinioTests {

  static final String SAVE_DIR = System.getProperty("java.io.tmpdir");

  static final Faker FAKER = new Faker();

  static final String ADMIN = "admin";

  static final String PASSWORD = "Test@123";

  // 启动 MinIO 容器
  @Container
  private static final GenericContainer<?> MINIO_CONTAINER =
      new GenericContainer<>("minio/minio:RELEASE.2025-06-13T11-33-47Z")
          .withExposedPorts(9000, 9001) // MinIO API 和 Console 端口
          .withEnv("MINIO_ROOT_USER", ADMIN)
          .withEnv("MINIO_ROOT_PASSWORD", PASSWORD)
          .withCommand("server", "/data", "--console-address", ":9001")
          .waitingFor(
              Wait.forHttp("/minio/health/ready")
                  .forPort(9000)
                  .allowInsecure()
                  .withStartupTimeout(Duration.ofMinutes(1)));

  private static MinioClient obsClient;

  @Autowired private AsyncMinioHandler handler;

  @Autowired private MinioClientProperties props;

  @DynamicPropertySource
  static void registerProperties(DynamicPropertyRegistry registry) {
    // 获取动态端口并设置数据源 URL
    registry.add(
        "minio.client.endpoint",
        () ->
            String.format(
                "http://%s:%d", MINIO_CONTAINER.getHost(), MINIO_CONTAINER.getMappedPort(9000)));

    registry.add("minio.client.access-key", () -> ADMIN);

    registry.add("minio.client.secret-key", () -> PASSWORD);
  }

  @BeforeAll
  public static void init() {
    obsClient =
        MinioClient.builder()
            .endpoint(
                String.format(
                    "http://%s:%d", MINIO_CONTAINER.getHost(), MINIO_CONTAINER.getMappedPort(9000)))
            .credentials(ADMIN, PASSWORD)
            .build();
  }

  @BeforeEach
  public void cleanup() throws Exception {
    obsClient
        .listBuckets()
        .forEach(
            bucket -> {
              String bucketName = bucket.name();
              log.info("Deleting bucket: {}", bucketName);
              deleteAllObjectsInBucket(bucketName);
              try {
                obsClient.removeBucket(RemoveBucketArgs.builder().bucket(bucketName).build());
              } catch (Exception e) {
                throw new RuntimeException(e);
              }
            });
  }

  @Test
  public void test1() {
    StepVerifier.create(handler.listBuckets()).expectNextCount(0).verifyComplete();
  }

  @Test
  public void test2() {
    String bucketName = OSSBucketGenerator.generate(10);
    StepVerifier.create(handler.makeBucket(bucketName)).expectNext(Boolean.TRUE).verifyComplete();
    StepVerifier.create(handler.listBuckets())
        .expectNextMatches(bucket -> bucket.name().equals(bucketName))
        .verifyComplete();
  }

  @Test
  public void test3() {
    String bucketName = OSSBucketGenerator.generate(10);
    StepVerifier.create(handler.makeBucket(bucketName)).expectNext(Boolean.TRUE).verifyComplete();
    StepVerifier.create(handler.removeBucket(bucketName)).expectNext(Boolean.TRUE).verifyComplete();
    StepVerifier.create(handler.listBuckets()).expectNextCount(0).verifyComplete();
  }

  @Test
  public void test4() throws IOException {
    String objectName = "test4.docx";
    var f = new File("target/test-data/" + objectName);
    RandomFileGenerator.createRandomTempFile(f, DataSize.ofMegabytes(1));
    log.info("file: {}, size: {}", f.getAbsolutePath(), f.length());
    String bucketName = OSSBucketGenerator.generate(10);
    StepVerifier.create(handler.upload(bucketName, objectName, f))
        .expectNextMatches(resp -> resp.object().equals(objectName))
        .verifyComplete();
  }

  @Test
  public void test5() throws IOException {
    String objectName = "test5.docx";
    var f = new File("target/test-data/" + objectName);
    RandomFileGenerator.createRandomTempFile(f, DataSize.ofMegabytes(1));
    String bucketName = OSSBucketGenerator.generate(10);
    StepVerifier.create(handler.upload(bucketName, objectName, f))
        .expectNextMatches(resp -> resp.object().equals(objectName))
        .verifyComplete();

    StepVerifier.create(handler.download(bucketName, objectName, SAVE_DIR))
        .expectNextMatches(
            file -> {
              try {
                return md5DigestAsHex(toByteArray(f)).equals(md5DigestAsHex(toByteArray(file)));
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            })
        .verifyComplete();
  }

  @Test
  public void test6() throws IOException {
    String objectName = "test6.docx";
    var f = new File("target/test-data/" + objectName);
    RandomFileGenerator.createRandomTempFile(f, DataSize.ofMegabytes(10));
    String bucketName = OSSBucketGenerator.generate(10);
    StepVerifier.create(handler.upload(bucketName, objectName, f))
        .expectNextMatches(resp -> resp.object().equals(objectName))
        .verifyComplete();
    StepVerifier.create(handler.removeObject(bucketName, objectName))
        .expectNext(true)
        .verifyComplete();

    StepVerifier.create(handler.getObjectStat(bucketName, objectName))
        .verifyErrorMatches(
            t ->
                t instanceof CompletionException e
                    && e.getCause() instanceof ErrorResponseException ee
                    && ee.errorResponse().message().equals("Object does not exist"));
  }

  @Test
  public void test7() throws IOException {
    String objectName = "test7.docx";
    var f = new File("target/test-data/" + objectName);
    RandomFileGenerator.createRandomTempFile(f, DataSize.ofMegabytes(100));
    String bucketName = OSSBucketGenerator.generate(10);
    StepVerifier.create(handler.upload(bucketName, objectName, f))
        .expectNextMatches(resp -> resp.object().equals(objectName))
        .verifyComplete();
    StepVerifier.create(handler.getObjectStat(bucketName, objectName))
        .expectNextMatches(resp -> resp.object().equals(objectName) && resp.size() == f.length())
        .verifyComplete();
  }

  //
  //  @Test
  //  @SneakyThrows
  //  public void test8() {
  //    var bucketName = OSSBucketGenerator.generate(10);
  //    var objectNames = new ArrayList<String>();
  //    for (int i = 0; i < 10; i++) {
  //      var objectName = String.format("test8%d.docx", i);
  //      var f = new File("target/test-data/" + objectName);
  //      createDirectories(f.toPath().getParent());
  //      RandomDocxGenerator.generateDocx(f, 1024 * (i + 1));
  //      StepVerifier.create(handler.upload(bucketName, objectName, f))
  //          .expectNextMatches(resp -> resp.object().equals(objectName))
  //          .verifyComplete();
  //      objectNames.add(objectName);
  //    }
  //
  //    StepVerifier.create(handler.deleteObjects(bucketName, objectNames))
  //        .expectNext(true)
  //        .verifyComplete();
  //
  //    StepVerifier.create(handler.listBucketObjects(bucketName, true))
  //        .expectNextMatches(List::isEmpty)
  //        .verifyComplete();
  //  }

  @Test
  public void test9() {
    String bucketName = OSSBucketGenerator.generate(10);
    StepVerifier.create(handler.removeBucket(bucketName))
        .verifyErrorMatches(
            t ->
                t instanceof ErrorResponseException e
                    && e.errorResponse().message().equals("The specified bucket does not exist"));
  }

  @Test
  public void test10() {
    var bucketName = OSSBucketGenerator.generate(10);
    StepVerifier.create(handler.makeBucket(bucketName)).expectNext(Boolean.TRUE).verifyComplete();
    StepVerifier.create(handler.removeObject(bucketName, "non-existing-object"))
        .expectNext(true)
        .verifyComplete();
  }

  @Test
  public void test11() {
    var bucketName = "ABC";
    assertThrowsExactly(
        IllegalArgumentException.class,
        () -> handler.makeBucket(bucketName).subscribe(),
        "bucket name 'ABC' does not follow Amazon S3 standards. For more information refer https://docs.aws.amazon.com/AmazonS3/latest/userguide/bucketnamingrules.html");
  }

  @Test
  public void test12() throws IOException {
    var fp = Paths.get(SAVE_DIR).resolve("test12.docx");
    File file = fp.toFile();
    RandomFileGenerator.createRandomTempFile(file, DataSize.ofMegabytes(100));
    var bucketName = OSSBucketGenerator.generate(63);
    StepVerifier.create(handler.makeBucket(bucketName)).expectNext(Boolean.TRUE).verifyComplete();
    StepVerifier.create(handler.upload(bucketName, "test12.docx", file))
        .verifyErrorMatches(
            t ->
                t instanceof UploadObjectException e
                    && e.getCause() instanceof FileNotFoundException);
  }

  @Test
  public void test13() throws IOException {
    var bucketName = OSSBucketGenerator.generate(3);
    StepVerifier.create(handler.makeBucket(bucketName)).expectNext(Boolean.TRUE).verifyComplete();

    var path = Paths.get(props.getSavingDir());
    createDirectories(path);
    for (int i = 0; i < 10; i++) {
      var name = String.format("test13%d.docx", i);
      var fp = path.resolve(name);
      File file = fp.toFile();
      RandomFileGenerator.createRandomTempFile(file, DataSize.ofMegabytes(10));
      StepVerifier.create(handler.upload(bucketName, name, file))
          .expectNextMatches(resp -> resp.object().equals(name))
          .verifyComplete();
    }

    StepVerifier.create(handler.listBucketObjects(bucketName, true))
        .expectNextMatches(items -> items.size() == 10)
        .verifyComplete();
  }

  //
  //  @Test
  //  public void test14() throws Exception {
  //    String readonly = "readonly";
  //    StepVerifier.create(handler.makeBucket(readonly)).expectNext(readonly).verifyComplete();
  //    final String policy =
  //        """
  //                    {
  //                        "Version": "2012-10-17",\s
  //                        "Statement": [
  //                            {
  //                                "Effect": "Allow",\s
  //                                "Principal": {
  //                                    "AWS": [
  //                                        "*"
  //                                    ]
  //                                },\s
  //                                "Action": [
  //                                    "s3:GetBucketLocation",\s
  //                                    "s3:GetObject",\s
  //                                    "s3:ListBucket"
  //                                ],\s
  //                                "Resource": [
  //                                    "arn:aws:s3:::readonly",\s
  //                                    "arn:aws:s3:::readonly/*"
  //                                ]
  //                            },\s
  //                            {
  //                                "Effect": "Deny",\s
  //                                "Principal": {
  //                                    "AWS": [
  //                                        "*"
  //                                    ]
  //                                },\s
  //                                "Action": [
  //                                    "s3:AbortMultipartUpload",\s
  //                                    "s3:PutObject"
  //                                ],\s
  //                                "Resource": [
  //                                    "arn:aws:s3:::readonly/*"
  //                                ]
  //                            }
  //                        ]
  //                    }""";
  //
  //    // 创建 ObjectMapper 并启用缩进
  //    ObjectMapper mapper = new ObjectMapper();
  //    mapper.disable(INDENT_OUTPUT);
  //
  //    // 格式化输出
  //    JsonNode jsonNode = mapper.readTree(policy);
  //    String ppolicy = mapper.writeValueAsString(jsonNode);
  //
  //    StepVerifier.create(handler.setBucketPolicy(readonly, policy))
  //        .expectNext(Boolean.TRUE)
  //        .verifyComplete();
  //    StepVerifier.create(handler.getBucketPolicy(readonly)).expectNext(ppolicy).verifyComplete();
  //  }

  //  @Test
  //  public void test15() throws Exception {
  //    String readonly = "readonly1";
  //    StepVerifier.create(handler.makeBucket(readonly)).expectNext(readonly).verifyComplete();
  //    String policy =
  //        """
  //                              {
  //                                  "Version": "2012-10-17",\s
  //                                  "Statement": [
  //                                      {
  //                                          "Effect": "Allow",\s
  //                                          "Principal": {
  //                                              "AWS": [
  //                                                  "*"
  //                                              ]
  //                                          },\s
  //                                          "Action": [
  //                                              "s3:GetObject",\s
  //                                              "s3:ListBucket"
  //                                          ],\s
  //                                          "Resource": [
  //                                              "arn:aws:s3:::readonly1",\s
  //                                              "arn:aws:s3:::readonly1/*"
  //                                          ]
  //                                      },\s
  //                                      {
  //                                          "Effect": "Deny",\s
  //                                          "Principal": {
  //                                              "AWS": [
  //                                                  "*"
  //                                              ]
  //                                          },\s
  //                                          "Action": [
  //                                              "s3:AbortMultipartUpload",\s
  //                                              "s3:PutObject",\s
  //                                              "s3:DeleteObject"
  //                                          ],\s
  //                                          "Resource": [
  //                                              "arn:aws:s3:::readonly1/*"
  //                                          ]
  //                                      }
  //                                  ]
  //                              }""";
  //
  //    StepVerifier.create(handler.setBucketPolicy(readonly, policy))
  //        .expectNext(Boolean.TRUE)
  //        .verifyComplete();
  //
  //    var path = Paths.get(props.getSavingDir());
  //    createDirectories(path);
  //    var name = "test15.docx";
  //    var fp = path.resolve(name);
  //    File file = fp.toFile();
  //    RandomDocxGenerator.generateDocx(file, 1024 * 30);
  //    StepVerifier.create(handler.upload(readonly, name, file))
  //        .expectNextMatches(resp -> resp.object().equals(name))
  //        .verifyComplete();
  //    StepVerifier.create(handler.deleteObject(readonly, name))
  //        .expectNext(Boolean.TRUE)
  //        .verifyComplete();
  //  }

  @SneakyThrows
  private static void deleteAllObjectsInBucket(String bucketName) {
    // 列出所有对象（递归处理子目录）
    Iterable<Result<Item>> objects =
        obsClient.listObjects(
            ListObjectsArgs.builder()
                .bucket(bucketName)
                .recursive(true) // 递归获取所有层级对象
                .build());

    List<DeleteObject> deleteList = new ArrayList<>();
    for (Result<Item> result : objects) {
      Item item = result.get();
      deleteList.add(new DeleteObject(item.objectName())); // 添加对象名到删除列表
    }

    Iterable<Result<DeleteError>> results =
        obsClient.removeObjects(
            RemoveObjectsArgs.builder()
                .bucket(bucketName)
                .objects(deleteList) // 传入待删除对象列表
                .build());

    for (Result<DeleteError> result : results) {
      DeleteError error = result.get();
      log.info(error.toString());
    }
  }
}
