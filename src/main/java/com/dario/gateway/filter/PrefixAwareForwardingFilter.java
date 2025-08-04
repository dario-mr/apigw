package com.dario.gateway.filter;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;
import static org.springframework.util.StringUtils.hasText;

import com.dario.gateway.filter.PrefixAwareForwardingFilter.Config;
import java.net.URI;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Infers the first path segment as the external prefix, strips it, and injects correct forwarded headers.
 * <p>
 * Example: incoming {@code /api-stress-test/login} → downstream sees {@code /login}, and headers contain:
 * <ul>X-Forwarded-Prefix: /api-stress-test</ul>
 * <ul>X-Forwarded-Host: original host</ul>
 * <ul>X-Forwarded-Proto: original scheme</ul>
 */
@Slf4j
@Component
public class PrefixAwareForwardingFilter extends AbstractGatewayFilterFactory<Config> {

  public PrefixAwareForwardingFilter() {
    super(Config.class);
  }

  @Override
  public GatewayFilter apply(Config config) {
    return (exchange, chain) -> {
      var original = exchange.getRequest();

      var host = getOrDefault("", original.getHeaders().getFirst("Host"));
      var scheme = getOrDefault("http",
          original.getHeaders().getFirst("X-Forwarded-Proto"),
          original.getURI().getScheme());
      var prefix = getOrDefault("", config.getPrefix());
      var strippedPath = stripPrefix(original, prefix);

      var newUri = buildNewURI(original.getURI(), strippedPath);

      var internalTarget = getInternalUri(exchange, strippedPath, original);
      log.info("Redirecting {} to {}", original.getURI(), internalTarget);

      var mutated = original.mutate()
          .uri(newUri)
          .header("X-Forwarded-Host", host)
          .header("X-Forwarded-Proto", scheme)
          .header("X-Forwarded-Prefix", prefix)
          .build();

      return chain.filter(exchange.mutate().request(mutated).build());
    };
  }

  private static String getOrDefault(String defaultValue, String... values) {
    for (String value : values) {
      if (hasText(value)) {
        return value;
      }
    }

    return defaultValue;
  }

  private static String stripPrefix(ServerHttpRequest original, String prefix) {
    var rawPath = original.getURI().getRawPath(); // e.g. /api-stress-test/whatever
    var strippedPath = rawPath;

    if (!prefix.isEmpty() && rawPath.startsWith(prefix)) {
      strippedPath = rawPath.substring(prefix.length());
      if (strippedPath.isEmpty()) {
        strippedPath = "/"; // keep root
      }
    }

    return strippedPath;
  }

  private static URI buildNewURI(URI originalURI, String strippedPath) {
    try {
      return UriComponentsBuilder.fromUri(originalURI)
          .replacePath(strippedPath)
          .build(true) // strict, validates & encodes
          .toUri();
    } catch (IllegalArgumentException e) {
      return UriComponentsBuilder.fromUri(originalURI)
          .replacePath(strippedPath)
          .build(false) // lenient, skip the component validation/encoding to avoid the error
          .toUri();
    }
  }

  private static URI getInternalUri(ServerWebExchange exchange, String strippedPath, ServerHttpRequest original) {
    Route route = exchange.getRequiredAttribute(GATEWAY_ROUTE_ATTR);
    var backendBase = route.getUri();
    return UriComponentsBuilder.fromUri(backendBase)
        .replacePath(strippedPath)
        .query(original.getURI().getQuery())
        .build(false)
        .toUri();
  }

  @Setter
  @Getter
  public static class Config {

    // prefix to supply, e.g. /api-stress-test
    private String prefix;
  }

}
