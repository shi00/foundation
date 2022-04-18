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

import com.google.protobuf.AbstractMessage;
import com.google.protobuf.Any;
import lombok.NonNull;

import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * 类型转换工具
 *
 * @param <T> 类型
 * @param <R> 类型
 * @author louis sin
 * @version 1.0.0
 * @since 2022-04-11 23:24
 */
public interface TypeConverter<T, R> {

  /**
   * 类型转换
   *
   * @param t 入参
   * @return 转换结果
   * @throws IOException 异常
   */
  R to(T t) throws IOException;

  /**
   * 类型转换
   *
   * @param r 入参
   * @return 转换结果
   * @throws IOException 异常
   */
  T from(R r) throws IOException;

  /**
   * 转换反转
   *
   * @return 参数类型反转
   */
  default TypeConverter<R, T> reverse() {
    return new TypeConverter<>() {
      @Override
      public T to(R r) throws IOException {
        return TypeConverter.this.from(r);
      }

      @Override
      public R from(T t) throws IOException {
        return TypeConverter.this.to(t);
      }
    };
  }

  /**
   * 输出即输入
   *
   * @param <S> 类型
   * @return identity
   */
  static <S> TypeConverter<S, S> identity() {
    return new TypeConverter<>() {
      @Override
      public S to(S s) {
        return s;
      }

      @Override
      public S from(S s) {
        return s;
      }

      @Override
      public TypeConverter<S, S> reverse() {
        return this;
      }
    };
  }

  /**
   * 获取protobuf消息与byte数组之间的类型转换器，使用Any类型实现，二进制信息中包含类型编码，效率较低，不推荐使用
   *
   * @param tClass protobuf消息类型
   * @param <T> 消息类型
   * @return 类型转换器
   */
  static <T extends AbstractMessage> TypeConverter<T, byte[]> getProtobufTypeConver(
      @NonNull Class<T> tClass) {
    return new TypeConverter<>() {
      @Override
      public byte[] to(T t) {
        if (t == null) {
          return null;
        }
        return Any.pack(t).toByteArray();
      }

      @Override
      public T from(byte[] bytes) throws IOException {
        if (bytes == null) {
          return null;
        }
        Any any = Any.parseFrom(bytes);
        if (any.is(tClass)) {
          return any.unpack(tClass);
        }
        throw new IllegalStateException(
            String.format("Failed to convert bytes to %s", tClass.getName()));
      }
    };
  }

  /** 字符串和byte数组互转，使用UTF8编码 */
  TypeConverter<Long, byte[]> LONG_TO_BYTES =
      new TypeConverter<>() {

        @Override
        public byte[] to(Long value) {
          if (value == null) {
            return null;
          }
          byte[] result = new byte[Long.BYTES];
          for (int i = Long.BYTES - 1; i >= 0; i--) {
            result[i] = (byte) (value & 0xffL);
            value >>= Long.BYTES;
          }
          return result;
        }

        @Override
        public Long from(byte[] bytes) {
          if (bytes == null || bytes.length != Long.BYTES) {
            return null;
          }
          return (bytes[0] & 0xFFL) << 56
              | (bytes[1] & 0xFFL) << 48
              | (bytes[2] & 0xFFL) << 40
              | (bytes[3] & 0xFFL) << 32
              | (bytes[4] & 0xFFL) << 24
              | (bytes[5] & 0xFFL) << 16
              | (bytes[6] & 0xFFL) << 8
              | (bytes[7] & 0xFFL);
        }
      };

  /** 字符串和byte数组互转，使用UTF8编码 */
  TypeConverter<String, byte[]> STRING_TO_BYTES =
      new TypeConverter<>() {
        @Override
        public byte[] to(String str) {
          if (str == null) {
            return null;
          }
          return str.getBytes(UTF_8);
        }

        @Override
        public String from(byte[] bytes) {
          if (bytes == null) {
            return null;
          }
          return new String(bytes, UTF_8);
        }
      };
}
