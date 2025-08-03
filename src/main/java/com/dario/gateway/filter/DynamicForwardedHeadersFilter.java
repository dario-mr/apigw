package com.dario.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;

/**
 * Adds dynamic forwarded headers and the prefix.
 */
@Component
public class DynamicForwardedHeadersFilter extends AbstractGatewayFilterFactory<DynamicForwardedHeadersFilter.Config> {

  public DynamicForwardedHeadersFilter() {
    super(Config.class);
  }

  @Override
  public GatewayFilter apply(Config config) {
    return (exchange, chain) -> {
      var original = exchange.getRequest();

      var host = original.getHeaders().getFirst("Host");
      var scheme = exchange.getRequest().getURI().getScheme();
      var prefix = config.getPrefix() != null ? config.getPrefix() : "";

      var mutated = original.mutate()
          .header("X-Forwarded-Host", host != null ? host : "")
          .header("X-Forwarded-Proto", scheme != null ? scheme : "http")
          .header("X-Forwarded-Prefix", prefix)
          .build();

      return chain.filter(exchange.mutate().request(mutated).build());
    };
  }

  public static class Config {

    // prefix to supply, e.g. /api-stress-test
    private String prefix;

    public String getPrefix() {
      return prefix;
    }

    public void setPrefix(String prefix) {
      this.prefix = prefix;
    }
  }
}
