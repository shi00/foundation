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
@Warmup(iterations = 3)
@Measurement(iterations = 5)
@Fork(
    value = 2,
    jvmArgs = {"-Xms1G", "-Xmx1G"})
public class BenchmarkTwitterSnowflakeIdAndDuuidTests {

  @Param({"10000000"})
  private int counter;

  @Param({"1", "2"})
  private int type;

  private DuuidGenerator generator;

  public static void main(String[] args) throws RunnerException {
    Options opt =
        new OptionsBuilder()
            .include(BenchmarkTwitterSnowflakeIdAndDuuidTests.class.getSimpleName())
            .forks(1)
            .build();
    new Runner(opt).run();
  }

  long workerId;

  @Setup
  public void setup() {
    generator =
        type == 1
            ? new TwitterSnowFlakeIdGenerator(1, 1)
            : new CircularQueueDuuidGenerator(() -> ++workerId, false);
  }

  @Benchmark
  public void spsc(Blackhole bh) {
    for (int i = 0; i < counter; i++) {
      bh.consume(generator.nextId());
    }
  }
}
