/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.silong.foundation.devastator.utils;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.CompatibleFieldSerializer;
import com.esotericsoftware.kryo.serializers.JavaSerializer;
import com.esotericsoftware.kryo.util.Pool;
import org.jgroups.util.Util;
import org.objenesis.strategy.StdInstantiatorStrategy;

import java.io.*;
import java.net.URI;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.ArrayUtils.isEmpty;

/**
 * kryo序列化工具
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-04-22 22:44
 */
public final class KryoUtils implements Serializable {

  @Serial private static final long serialVersionUID = -1423099862137538111L;

  /** kryo对象池容量，默认：8 */
  public static final int KRYO_POOL_CAPACITY =
      Integer.parseInt(System.getProperty("devastator.kryo.pool.capacity", "8"));

  /** kryo Input对象池容量，默认：16 */
  public static final int KRYO_INPUT_POOL_CAPACITY =
      Integer.parseInt(System.getProperty("devastator.kryo.input.pool.capacity", "16"));

  /** kryo Output对象池容量，默认：16 */
  public static final int KRYO_OUTPUT_POOL_CAPACITY =
      Integer.parseInt(System.getProperty("devastator.kryo.output.pool.capacity", "16"));

  /** 缓存大小8192，单位：字节 */
  public static final int DEFAULT_BUFFER_SIZE =
      Integer.parseInt(System.getProperty("devastator.kryo.buffer.size", "8192"));

  private static final Pool<Kryo> KRYO_POOL =
      new Pool<>(true, false, KRYO_POOL_CAPACITY) {

        private static final JavaSerializer DEFAULT = new JavaSerializer();

        @Override
        protected Kryo create() {
          Kryo kryo = new Kryo();
          kryo.setReferences(true);
          kryo.setRegistrationRequired(false);
          kryo.setClassLoader(KryoUtils.class.getClassLoader());
          kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
          kryo.setDefaultSerializer(CompatibleFieldSerializer.class);
          // kryo5没有内置以下几种类型的默认序列化实现，因此使用java序列化
          kryo.register(AtomicInteger.class, DEFAULT);
          kryo.register(AtomicBoolean.class, DEFAULT);
          kryo.register(AtomicReference.class, DEFAULT);
          kryo.register(AtomicLong.class, DEFAULT);
          kryo.register(Pattern.class, DEFAULT);
          kryo.register(UUID.class, DEFAULT);
          kryo.register(URI.class, DEFAULT);
          return kryo;
        }
      };

  private static final Pool<Input> INPUT_POOL =
      new Pool<>(true, false, KRYO_INPUT_POOL_CAPACITY) {
        @Override
        protected Input create() {
          return new Input(DEFAULT_BUFFER_SIZE);
        }
      };

  private static final Pool<Output> OUTPUT_POOL =
      new Pool<>(true, false, KRYO_OUTPUT_POOL_CAPACITY) {
        @Override
        protected Output create() {
          return new Output(DEFAULT_BUFFER_SIZE, -1);
        }
      };

  /** 禁止实例化 */
  private KryoUtils() {}

  /**
   * 对象序列化
   *
   * @param object 对象
   * @return 二进制结果
   */
  public static byte[] serialize(Object object) {
    if (object == null) {
      throw new IllegalArgumentException("object must not be null.");
    }
    Kryo kryo = KRYO_POOL.obtain();
    Output output = OUTPUT_POOL.obtain();
    try {
      kryo.writeClassAndObject(output, object);
      return output.toBytes();
    } finally {
      KRYO_POOL.free(kryo);
      OUTPUT_POOL.free(output);
    }
  }

  /**
   * 反序列化
   *
   * @param bytes 序列化数据
   * @return 对象
   * @param <T> 对象类型
   */
  public static <T> T deserialize(byte[] bytes) {
    if (isEmpty(bytes)) {
      throw new IllegalArgumentException("bytes must not be null or empty.");
    }
    Kryo kryo = KRYO_POOL.obtain();
    Input input = INPUT_POOL.obtain();
    try {
      input.setBuffer(bytes);
      return (T) kryo.readClassAndObject(input);
    } finally {
      KRYO_POOL.free(kryo);
      INPUT_POOL.free(input);
    }
  }

  /**
   * 对象序列化到指定输出流
   *
   * @param object 对象
   * @param dataOutput 输出流
   * @throws IOException 异常
   */
  public static void serialize(Object object, DataOutput dataOutput) throws IOException {
    if (object == null) {
      throw new IllegalArgumentException("object must not be null.");
    }
    if (dataOutput == null) {
      throw new IllegalArgumentException("dataOutput must not be null.");
    }
    Kryo kryo = KRYO_POOL.obtain();
    Output output = OUTPUT_POOL.obtain();
    try {
      kryo.writeClassAndObject(output, object);
      Util.writeByteBuffer(output.getBuffer(), 0, output.position(), dataOutput);
    } finally {
      KRYO_POOL.free(kryo);
      OUTPUT_POOL.free(output);
    }
  }

  /**
   * 反序列化
   *
   * @param dataInput 二进制输入
   * @return 对象
   * @param <T> 对象类型
   */
  public static <T> T deserialize(DataInput dataInput) throws IOException {
    if (dataInput == null) {
      throw new IllegalArgumentException("dataInput must not be null.");
    }
    Kryo kryo = KRYO_POOL.obtain();
    Input input = INPUT_POOL.obtain();
    try {
      byte[] bytes = Util.readByteBuffer(dataInput);
      input.setBuffer(bytes);
      return (T) kryo.readClassAndObject(input);
    } finally {
      KRYO_POOL.free(kryo);
      INPUT_POOL.free(input);
    }
  }
}
