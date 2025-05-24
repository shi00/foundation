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

package com.silong.foundation.dj.scrapper;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.ClosureSerializer;
import java.util.concurrent.Callable;
import org.objenesis.strategy.StdInstantiatorStrategy;

/**
 * kryo序列化器
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2024-01-12 9:47
 * @param <T> 序列化类型
 */
public class KryoSerializer<T> {

  private static final ThreadLocal<Kryo> KRYO_THREAD_LOCAL =
      ThreadLocal.withInitial(
          () -> {
            Kryo kryo = new Kryo();
            kryo.setReferences(true);
            kryo.setRegistrationRequired(false);
            kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
            kryo.register(Object[].class);
            kryo.register(Class.class);
            kryo.register(ClosureSerializer.Closure.class, new ClosureSerializer());
            kryo.register(Callable.class);
            kryo.register(Runnable.class);
            return kryo;
          });

  public byte[] toBytes(T t, Class<T> tClass) {
    try (Output output = new Output(1024, -1)) {
      KRYO_THREAD_LOCAL.get().writeObjectOrNull(output, t, tClass);
      return output.toBytes();
    }
  }

  public T fromBytes(byte[] bytes, Class<T> tClass) {
    return null;
  }
}
