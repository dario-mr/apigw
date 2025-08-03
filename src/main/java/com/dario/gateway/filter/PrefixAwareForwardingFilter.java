package com.dario.gateway.filter;

import com.dario.gateway.filter.PrefixAwareForwardingFilter.Config;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
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

      var host = getOrDefault(original.getHeaders().getFirst("Host"), "");
      var scheme = getOrDefault(original.getURI().getScheme(), "http");
      var prefix = getOrDefault(config.getPrefix(), "");
      var strippedPath = stripPrefix(original, prefix);

      var newUri = UriComponentsBuilder.fromUri(original.getURI())
          .replacePath(strippedPath)
          .build(true)
          .toUri();

      var mutated = original.mutate()
          .uri(newUri)
          .header("X-Forwarded-Host", host)
          .header("X-Forwarded-Proto", scheme)
          .header("X-Forwarded-Prefix", prefix)
          .build();

      log.info("Redirecting {} to {}", original.getURI(), newUri);
      return chain.filter(exchange.mutate().request(mutated).build());
    };
  }

  private static String getOrDefault(String value, String defaultValue) {
    return value == null ? defaultValue : value;
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

  @Setter
  @Getter
  public static class Config {

    // prefix to supply, e.g. /api-stress-test
    private String prefix;
  }

}
