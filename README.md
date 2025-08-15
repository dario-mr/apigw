# API Gateway Stack

Lightweight, self-contained API gateway with auto-TLS and path-prefix routing.

## Components

- **Caddy**: TLS termination (Let’s Encrypt), HTTP → HTTPS, security headers, reverse-proxy to Gateway.
- **Gateway**: Spring Cloud Gateway with prefix stripping and `X-Forwarded-*` support for proper redirects.
- **Backends**: `api-stress-test`, `ichiro-family-tree`, etc.

## How to run

1. Point your domain (e.g. `dariolab.com`, `www.dariolab.com`) to this server’s IP.
2. Edit the config files if you need to change hostnames, paths, or behavior:
    - [Caddyfile](Caddyfile): defines the public domain, TLS/Let’s Encrypt setup, security headers, and reverse-proxy
      rules into the gateway.
    - [docker-compose.yml](docker-compose.yml): orchestrates the whole stack (Caddy, gateway, Redis, and backend
      services), networking, and volume persistence.
    - [application.yml](src/main/resources/application.yml): config for the Spring Cloud Gateway itself - route
      prefixes, forwarding logic, and downstream service addresses.
3. Start everything:
   ```shell
   docker compose up -d --build
   ```

## Useful commands

### Logs

```shell
docker compose logs -f caddy gateway
```

### Update docker images

```shell
docker compose pull
docker compose up -d --force-recreate
# recreate specific container
docker compose up -d --force-recreate --no-deps gateway
```

### Clean unused images

```shell
docker image prune -f
```

### Purge everything

Stops and removes all containers, networks, and volumes that were created by `docker compose up`.

Use carefully.

```shell
docker compose down --remove-orphans
```