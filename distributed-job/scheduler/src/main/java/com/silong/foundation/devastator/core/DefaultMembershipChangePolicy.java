package com.silong.foundation.devastator.core;

import org.jgroups.Address;
import org.jgroups.Membership;
import org.jgroups.stack.MembershipChangePolicy;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * 集群成员变更策略
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-04-25 23:30
 */
public class DefaultMembershipChangePolicy implements MembershipChangePolicy, Serializable {

  @Serial private static final long serialVersionUID = 0L;

  /** 节点处理能力权重属性key */
  public static final String NODE_POWER_WEIGHT_ATTRIBUTE_KEY = "node-power-weight";

  /** 实例 */
  public static final DefaultMembershipChangePolicy INSTANCE = new DefaultMembershipChangePolicy();

  /** 禁止实例化 */
  private DefaultMembershipChangePolicy() {}

  private double getNodePowerWeight(Address address, String key) {
    ClusterNodeUUID uuid = (ClusterNodeUUID) address;
    String weight = uuid.clusterNodeInfo().getAttributesMap().get(key);
    return weight != null ? Double.parseDouble(weight) : 0;
  }

  private int getRole(ClusterNodeUUID uuid) {
    return uuid.clusterNodeInfo().getRole();
  }

  @Override
  public List<Address> getNewMembership(
      Collection<Address> currentMembers,
      Collection<Address> joiners,
      Collection<Address> leavers,
      Collection<Address> suspects) {
    List<Address> members =
        currentMembers == null || currentMembers.isEmpty()
            ? new LinkedList<>()
            : new LinkedList<>(currentMembers);

    if (leavers != null && !leavers.isEmpty()) {
      members.removeAll(leavers);
    }

    if (suspects != null && !suspects.isEmpty()) {
      members.removeAll(suspects);
    }

    if (joiners != null && !joiners.isEmpty()) {
      members.addAll(joiners);
    }

    members.sort(this::compare);
    return members;
  }

  private int compare(Address m1, Address m2) {
    int compare =
        Double.compare(
            getNodePowerWeight(m2, NODE_POWER_WEIGHT_ATTRIBUTE_KEY),
            getNodePowerWeight(m1, NODE_POWER_WEIGHT_ATTRIBUTE_KEY));
    if (compare != 0) {
      return compare;
    }

    // 客户端节点有限作为coordinator
    int role1 = getRole((ClusterNodeUUID) m1);
    int role2 = getRole((ClusterNodeUUID) m2);
    compare = Integer.compare(role2, role1);
    if (compare != 0) {
      return compare;
    }
    return m1.compareTo(m2);
  }

  @Override
  public List<Address> getNewMembership(Collection<Collection<Address>> subviews) {
    // add the coord of each subview
    Membership coords = new Membership();
    subviews.stream()
        .filter(subview -> !subview.isEmpty())
        .forEach(subview -> coords.add(subview.iterator().next()));

    // pick the first coord of the sorted list as the new coord
    coords.sort();
    Membership new_mbrs = new Membership().add(coords.elementAt(0));

    // add all other members in the order in which they occurred in their subviews - dupes are not
    // added
    subviews.forEach(new_mbrs::add);
    return new_mbrs.getMembers();
  }
}
