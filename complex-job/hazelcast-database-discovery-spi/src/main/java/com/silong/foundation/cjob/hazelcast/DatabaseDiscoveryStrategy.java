package com.silong.foundation.cjob.hazelcast;

import com.hazelcast.logging.ILogger;
import com.hazelcast.spi.discovery.AbstractDiscoveryStrategy;
import com.hazelcast.spi.discovery.DiscoveryNode;

import java.util.Map;

/**
 * 数据库节点发现策略
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-03-27 21:18
 */
public class DatabaseDiscoveryStrategy extends AbstractDiscoveryStrategy {
  public DatabaseDiscoveryStrategy(ILogger logger, Map<String, Comparable> properties) {
    super(logger, properties);
  }

  @Override
  public Iterable<DiscoveryNode> discoverNodes() {
    return null;
  }
}
