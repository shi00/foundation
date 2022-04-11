package com.silong.foundation.devastator.utils;

import lombok.NonNull;
import org.jgroups.conf.ClassConfigurator;
import org.jgroups.util.ExtendedUUID;

import static com.silong.foundation.devastator.utils.TypeConverter.STRING_TO_BYTES;

/**
 * 扩展ExtendedUUID，使用zstd压缩算法对key，value进行压缩
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-04-11 22:49
 */
public class ClusterNodeUUID extends ExtendedUUID {
  static {
    // it will need to get registered with the ClassConfigurator in order to marshal it correctly
    // Note that the ID should be chosen such that it doesn’t collide with any IDs defined in
    // jg-magic-map.xml
    ClassConfigurator.add((short) 5674, ClusterNodeUUID.class);
  }

  /**
   * 默认构造方法
   *
   * @param uuid uuid
   */
  private ClusterNodeUUID(byte[] uuid) {
    super(uuid);
  }

  /**
   * 随机uuid生成对象
   *
   * @return @{@code ClusterNodeUUID}
   */
  public static ClusterNodeUUID randomUuid() {
    return new ClusterNodeUUID(generateRandomBytes());
  }

  /**
   *
   * @param key
   * @param keyConverter
   * @param value
   * @param valueConverter
   * @param <K>
   * @param <V>
   * @return
   */
  public <K, V> ClusterNodeUUID put(
      @NonNull K key,
      @NonNull TypeConverter<K, byte[]> keyConverter,
      @NonNull V value,
      @NonNull TypeConverter<V, byte[]> valueConverter) {
    put(keyConverter.to(key), valueConverter.to(value));
    return this;
  }

  /**
   * 根据key获取value
   *
   * @param key key
   * @param keyConverter key转换器
   * @param valueConverter value转换器
   * @param <T> value类型
   * @param <R> key类型
   * @return value
   */
  public <T, R> T get(
      @NonNull R key,
      @NonNull TypeConverter<R, byte[]> keyConverter,
      @NonNull TypeConverter<byte[], T> valueConverter) {
    return valueConverter.to(get(keyConverter.to(key)));
  }

  /**
   * 根据key获取值
   *
   * @param key key
   * @param valueConverter value类型转换器
   * @param <T> 值类型
   * @return 值
   */
  public <T> T get(byte[] key, @NonNull TypeConverter<byte[], T> valueConverter) {
    return valueConverter.to(get(key));
  }

  /**
   * 根据key获取值
   *
   * @param key key
   * @param valueConverter value类型转换器
   * @param <T> 值类型
   * @return 值
   */
  public <T> T get(String key, TypeConverter<byte[], T> valueConverter) {
    return get(key, STRING_TO_BYTES, valueConverter);
  }
}
