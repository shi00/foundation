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
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3)
@Measurement(iterations = 8)
@Fork(
    value = 2,
    jvmArgs = {"-Xms1G", "-Xmx1G"})
public class BenchmarkTwitterSnowflakeIdAndDuuidTests {

  @Param({"10000000"})
  private int counter;

  private DuuidGenerator circular;

  private DuuidGenerator twitter;

  public static void main(String[] args) throws RunnerException {
    Options opt =
        new OptionsBuilder()
            .include(BenchmarkTwitterSnowflakeIdAndDuuidTests.class.getSimpleName())
            .forks(1)
            .build();
    new Runner(opt).run();
  }

  @Setup
  public void setup() {
    circular = new CircularQueueDuuidGenerator(0, false);
    twitter = new TwitterSnowFlakeIdGenerator(1, 1);
  }

  @Benchmark
  public void circular(Blackhole bh) {
    for (int i = 0; i < counter; i++) {
      long id = circular.nextId();
      bh.consume(id);
    }
  }

  @Benchmark
  public void twitter(Blackhole bh) {
    for (int i = 0; i < counter; i++) {
      long id = twitter.nextId();
      bh.consume(id);
    }
  }
}
