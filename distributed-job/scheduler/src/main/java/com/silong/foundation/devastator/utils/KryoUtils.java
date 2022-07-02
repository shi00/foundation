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
import com.esotericsoftware.kryo.util.Pool;
import org.jgroups.util.Util;
import org.objenesis.strategy.StdInstantiatorStrategy;

import java.io.*;

import static org.rocksdb.util.SizeUnit.KB;

/**
 * kryo序列化工具
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-04-22 22:44
 */
public final class KryoUtils implements Serializable {

  @Serial private static final long serialVersionUID = 4808808739609644424L;

  /** kryo对象池容量，默认：8 */
  public static final int KRYO_POOL_CAPACITY =
      Integer.parseInt(System.getProperty("kryo.pool.capacity", "8"));

  /** kryo Input对象池容量，默认：16 */
  public static final int KRYO_INPUT_POOL_CAPACITY =
      Integer.parseInt(System.getProperty("kryo.input.pool.capacity", "16"));

  /** kryo Output对象池容量，默认：16 */
  public static final int KRYO_OUTPUT_POOL_CAPACITY =
      Integer.parseInt(System.getProperty("kryo.output.pool.capacity", "16"));

  private static final int DEFAULT_BUFFER_SIZE = (int) (4 * KB);

  private static final Pool<Kryo> KRYO_POOL =
      new Pool<>(true, false, KRYO_POOL_CAPACITY) {
        @Override
        protected Kryo create() {
          Kryo kryo = new Kryo();
          kryo.setReferences(true);
          kryo.setRegistrationRequired(false);
          kryo.setClassLoader(Thread.currentThread().getContextClassLoader());
          kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
          kryo.setDefaultSerializer(CompatibleFieldSerializer.class);
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
    Kryo kryo = KRYO_POOL.obtain();
    Output output = OUTPUT_POOL.obtain();
    try {
      output.reset();
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
    assert bytes != null && bytes.length != 0;
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
    assert dataOutput != null;
    Kryo kryo = KRYO_POOL.obtain();
    Output output = OUTPUT_POOL.obtain();
    try {
      output.reset();
      kryo.writeClassAndObject(output, object);
      Util.writeByteBuffer(output.getBuffer(), dataOutput);
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
    assert dataInput != null;
    Kryo kryo = KRYO_POOL.obtain();
    Input input = INPUT_POOL.obtain();
    try {
      byte[] bytes = Util.readByteBuffer(dataInput);
      assert bytes != null && bytes.length != 0;
      input.setBuffer(bytes);
      return (T) kryo.readClassAndObject(input);
    } finally {
      KRYO_POOL.free(kryo);
      INPUT_POOL.free(input);
    }
  }
}
