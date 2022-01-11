package com.silong.fundation.duuidserver.configure.properties;

import lombok.Builder;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;

import static com.silong.fundation.duuid.generator.impl.CircularQueueDuuidGenerator.Constants.*;

/**
 * 服务配置
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-03 11:31
 */
@Data
@Builder
@Validated
@ConfigurationProperties(prefix = "duuid.generator")
public class DuuidGeneratorProperties {
  /** workerId占用比特数量 */
  @Positive @Builder.Default private int workerIdBits = DEFAULT_WORK_ID_BITS;

  /** deltaDays占用比特数量 */
  @Positive @Builder.Default private int deltaDaysBits = DEFAULT_DELTA_DAYS_BITS;

  /** sequence占用比特数量 */
  @Positive @Builder.Default private int sequenceBits = DEFAULT_SEQUENCE_BITS;

  /** 序列号 */
  @PositiveOrZero @Builder.Default private long sequence = 0;

  /** 队列长度 */
  @Positive @Builder.Default private int queueCapacity = DEFAULT_QUEUE_CAPACITY;

  /** 是否开启生成id随机增长，避免出现连续id */
  @Builder.Default private boolean enableSequenceRandom = false;

  /** id增量随机数上边界 */
  @Positive @Builder.Default private int maxRandomIncrement = DEFAULT_MAX_RANDOM_INCREMENT;

  /** 启用的workerId分配器权限定名 */
  private String workerIdAllocatorFqdn;
}
