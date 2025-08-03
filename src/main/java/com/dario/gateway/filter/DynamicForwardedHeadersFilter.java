package com.dario.gateway.filter;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;

/**
 * Adds dynamic forwarded headers.
 */
@Component
@Slf4j
public class DynamicForwardedHeadersFilter extends AbstractGatewayFilterFactory<DynamicForwardedHeadersFilter.Config> {

  public DynamicForwardedHeadersFilter() {
    super(Config.class);
  }

  @Override
  public GatewayFilter apply(Config config) {
    return (exchange, chain) -> {
      var original = exchange.getRequest();

      var host = getOrEmptyString(original.getHeaders().getFirst("Host"));
      var scheme = getOrEmptyString(original.getURI().getScheme());
      var prefix = getOrEmptyString(config.getPrefix());

      var mutated = original.mutate()
          .header("X-Forwarded-Host", host)
          .header("X-Forwarded-Proto", scheme)
          .header("X-Forwarded-Prefix", prefix)
          .build();

      log.info("Overriding headers: \nX-Forwarded-Proto = {}\nX-Forwarded-Host = {}\nX-Forwarded-Prefix = {}",
          scheme, host, prefix);

      return chain.filter(exchange.mutate().request(mutated).build());
    };
  }

  private static String getOrEmptyString(String value) {
    return value == null ? "" : value;
  }

  @Setter
  @Getter
  public static class Config {

    // prefix to supply, e.g. /api-stress-test
    private String prefix;
  }

}
