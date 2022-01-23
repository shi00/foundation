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
package com.silong.foundation.duuid.generator;

import com.silong.foundation.duuid.generator.impl.CircularQueueDuuidGenerator;
import com.silong.foundation.duuid.generator.impl.TwitterSnowFlakeIdGenerator;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

/**
 * JHM对比推特雪花id实现与duuid实现性能
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-03 08:48
 */
@BenchmarkMode(Mode.All)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 10)
@Measurement(iterations = 10)
@Threads(1)
@Fork(
    value = 1,
    jvmArgs = {"-Xms64M", "-Xmx64M"})
public class BenchmarkTwitterSnowflakeIdAndDuuidTests {

  @Param({"10000"})
  private int loop;

  @Param({"CircularQueueDuuidGenerator", "TwitterSnowFlakeIdGenerator"})
  private String type;

  @Param({"true", "false"})
  private boolean randomIncrement;

  private DuuidGenerator generator;

  private long workerId;

  public static void main(String[] args) throws RunnerException {
    Options opt =
        new OptionsBuilder()
            .include(BenchmarkTwitterSnowflakeIdAndDuuidTests.class.getSimpleName())
            .build();
    new Runner(opt).run();
  }

  @Setup
  public void setup() {
    generator =
        type.equals("TwitterSnowFlakeIdGenerator")
            ? new TwitterSnowFlakeIdGenerator(1, 1)
            : new CircularQueueDuuidGenerator(() -> ++workerId, randomIncrement);
  }

  @Benchmark
  @Threads(5)
  public void spsc(Blackhole bh) {
    for (int i = 0; i < loop; i++) {
      bh.consume(generator.nextId());
    }
  }
}
