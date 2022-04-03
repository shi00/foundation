CREATE TABLE IF NOT EXISTS `hazelcast_cluster_nodes`
(
    `host_name`     VARCHAR(32) NOT NULL, /* 节点名 */
    `cluster_name`  VARCHAR(32) NOT NULL, /* 节点所属的集群名 */
    `instance_name` VARCHAR(64), /* 节点所属的实例名 */
    `ip_address`    VARCHAR(50) NOT NULL, /* 节点IP地址，支持ipv4和ipv6 */
    `port`          INT         NOT NULL, /* 节点监听端口 */
    `ip_type`       INT         NOT NULL, /* 节点ip类型，0表示ipv4，1表示ipv6 */
    `created_time`  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP, /* 节点记录创建时间 */
    `updated_time`  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, /* 节点记录更新时间 */
    PRIMARY KEY (`host_name`),
    INDEX HAZELCAST_CLUSTER_NODES_UPDATEDTIME_IDX (`updated_time`)
) ENGINE = INNODB
  DEFAULT CHARSET = UTF8MB4;