package com.silong.foundation.duuid.server.security.authorization;

import lombok.NonNull;
import org.springframework.security.authorization.AuthorityAuthorizationDecision;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.ImmutableList.toImmutableList;

/**
 * 授权管理器
 *
 * @author louis sin
 * @version 1.0.0
 * @since 2022-01-16 17:12
 */
public class SimpleReactiveAuthorizationManager
    implements ReactiveAuthorizationManager<ServerWebExchange> {

  private final Map<String, Set<String>> user2RolesMappings;

  private final Map<String, Set<String>> role2PathsMappings;

  private final List<GrantedAuthority> authorities;

  /**
   * 构造方法
   *
   * @param user2RolesMappings rbac配置
   * @param role2PathsMappings rbac配置
   */
  public SimpleReactiveAuthorizationManager(
      @NonNull Map<String, Set<String>> user2RolesMappings,
      @NonNull Map<String, Set<String>> role2PathsMappings) {
    this.user2RolesMappings = user2RolesMappings;
    this.role2PathsMappings = role2PathsMappings;
    this.authorities =
        user2RolesMappings.values().stream()
            .flatMap(Set::stream)
            .distinct()
            .map(SimpleGrantedAuthority::new)
            .collect(toImmutableList());
  }

  @Override
  public Mono<AuthorizationDecision> check(
      Mono<Authentication> authentication, ServerWebExchange exchange) {
    String requestPath = exchange.getRequest().getPath().value();
    return authentication
        .filter(Authentication::isAuthenticated)
        .flatMapIterable(Authentication::getAuthorities)
        .map(GrantedAuthority::getAuthority)
        .map(user2RolesMappings::get)
        .filter(roles -> !CollectionUtils.isEmpty(roles))
        .flatMapIterable(roles -> roles)
        .map(role2PathsMappings::get)
        .filter(paths -> !CollectionUtils.isEmpty(paths))
        .any(paths -> paths.contains(requestPath))
        .map(
            (granted) ->
                ((AuthorizationDecision) new AuthorityAuthorizationDecision(granted, authorities)))
        .defaultIfEmpty(new AuthorityAuthorizationDecision(false, authorities));
  }
}
