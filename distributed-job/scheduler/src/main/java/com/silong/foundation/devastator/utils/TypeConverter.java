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
   */
  R to(T t);

  /**
   * 类型转换
   *
   * @param r 入参
   * @return 转换结果
   */
  T from(R r);

  /**
   * 转换反转
   *
   * @return 参数类型反转
   */
  default TypeConverter<R, T> reverse() {
    return new TypeConverter<>() {
      @Override
      public T to(R r) {
        return TypeConverter.this.from(r);
      }

      @Override
      public R from(T t) {
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

  /** 字符串和byte数组互转 */
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
