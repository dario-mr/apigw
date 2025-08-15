package com.dario.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class KeyResolverConfig {

  @Bean("ipKeyResolver")
  public KeyResolver ipKeyResolver() {
    return exchange -> {
      var xff = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
      if (xff != null && !xff.isBlank()) {
        return Mono.just(xff.split(",")[0].trim());
      }

      var remote = exchange.getRequest().getRemoteAddress();
      return Mono.just(remote != null ? remote.getAddress().getHostAddress() : "unknown");
    };
  }
}
