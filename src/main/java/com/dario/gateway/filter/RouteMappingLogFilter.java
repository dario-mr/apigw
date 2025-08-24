package com.dario.gateway.filter;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ORIGINAL_REQUEST_URL_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;
import static org.springframework.core.Ordered.LOWEST_PRECEDENCE;

import java.net.URI;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@Order(LOWEST_PRECEDENCE - 1) // run late, after routing is decided
public class RouteMappingLogFilter implements GlobalFilter {

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    var original = resolveOriginalRequestUrl(exchange);

    return chain.filter(exchange).then(Mono.fromRunnable(() -> {
      var route = (Route) exchange.getAttribute(GATEWAY_ROUTE_ATTR);
      var target = (URI) exchange.getAttribute(GATEWAY_REQUEST_URL_ATTR);
      if (route == null || target == null) {
        return;
      }

      var status = exchange.getResponse().getStatusCode();
      var method = exchange.getRequest().getMethod();

      log.info("route={}, status={}, {} {} -> {}",
          route.getId(),
          status != null ? status.value() : "-",
          method,
          original != null ? pathAndQuery(original) : exchange.getRequest().getURI(),
          target);
    }));
  }

  private static URI resolveOriginalRequestUrl(ServerWebExchange exchange) {
    var originals = (Set<URI>) exchange.getAttribute(GATEWAY_ORIGINAL_REQUEST_URL_ATTR);
    if (originals == null || originals.isEmpty()) {
      return null;
    }
    return originals.iterator().next();
  }

  private static String pathAndQuery(URI uri) {
    var path = uri.getRawPath();
    var query = uri.getRawQuery();
    return query == null ? path : path + "?" + query;
  }

}
