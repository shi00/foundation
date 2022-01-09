package com.silong.fundation.duuid.generator;

import com.silong.fundation.duuid.generator.impl.CircularQueueDuuidGenerator;
import com.silong.fundation.duuid.generator.impl.TwitterSnowFlakeIdGenerator;
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
