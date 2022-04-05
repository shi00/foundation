/**
  创建集群节点记录信息表
 */
CREATE TABLE IF NOT EXISTS `hazelcast_cluster_nodes`
(
    `host_name`     VARCHAR(32) NOT NULL COMMENT '节点名',
    `cluster_name`  VARCHAR(32) NOT NULL COMMENT '节点所属的集群名',
    `instance_name` VARCHAR(64) comment '节点所属的实例名',
    `ip_address`    VARCHAR(50) NOT NULL COMMENT '节点IP地址，支持ipv4和ipv6',
    `port`          INT         NOT NULL COMMENT '节点监听端口',
    `created_time`  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '节点记录创建时间',
    `updated_time`  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '节点记录更新时间',
    PRIMARY KEY (`host_name`, `ip_address`, `port`),
    INDEX HAZELCAST_CLUSTER_NODES_IDX (`cluster_name`, `instance_name`)
) ENGINE = INNODB
  DEFAULT CHARSET = UTF8MB4;

CREATE EVENT IF NOT EXISTS CLEANUP_INACTIVE_HAZELCAST_CLUSTER_NODES_EVENT
    ON SCHEDULE EVERY 1 MONTH
        STARTS DATE_ADD(DATE_ADD(DATE_SUB(LAST_DAY(NOW()), INTERVAL DAY(LAST_DAY(NOW())) - 1 DAY), INTERVAL 1 MONTH),
                        INTERVAL 4 HOUR)
    ON COMPLETION PRESERVE ENABLE
    COMMENT '每月第一天凌晨4时清理集群节点表中的去激活节点记录，所有清理执行时间点前7天的去激活节点记录都会被删除'
    DO
    DELETE
    FROM `hazelcast_cluster_nodes`
    WHERE DATE_ADD(CURRENT_TIMESTAMP(), INTERVAL -(7) DAY) > `hazelcast_cluster_nodes`.`updated_time`;
