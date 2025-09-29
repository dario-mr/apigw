package com.dario.gateway.filter;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;

import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;

@Slf4j
@Component
public class ErrorAccessLogFilter implements GlobalFilter {

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    var req = exchange.getRequest();
    var errRef = new AtomicReference<Throwable>();

    return chain.filter(exchange)
        .doOnError(errRef::set) // capture handshake errors, timeouts, etc.
        .doFinally(signal -> {
          var statusCode = exchange.getResponse().getStatusCode();
          var status = (statusCode != null) ? statusCode.value() : 0;
          var ex = errRef.get();

          var hasError = signal == SignalType.ON_ERROR || ex != null;
          var isBadStatus = status >= 400;

          if (hasError || isBadStatus) {
            Route route = exchange.getAttribute(GATEWAY_ROUTE_ATTR);
            var routeId = (route != null) ? route.getId() : "no-route";

            var target = exchange.getAttribute(GATEWAY_REQUEST_URL_ATTR);
            var targetUri = (target != null) ? target.toString() : req.getURI().toString();

            var ip = req.getHeaders().getFirst("X-Forwarded-For");
            if (ip == null && req.getRemoteAddress() != null) {
              ip = req.getRemoteAddress().getAddress().getHostAddress();
            }
            if (ip == null) {
              ip = "unknown";
            }

            if (ex != null) {
              log.warn(
                  "GW_ERR ip={} method={} path={} status={} route={} target=\"{}\" term={} err={}",
                  ip, req.getMethod(), req.getURI().getRawPath(), status, routeId, targetUri,
                  signal, ex.getMessage());
            } else {
              log.warn("GW_4XX5XX ip={} method={} path={} status={} route={} target=\"{}\" term={}",
                  ip, req.getMethod(), req.getURI().getRawPath(), status, routeId, targetUri,
                  signal);
            }
          }
        });
  }
}
