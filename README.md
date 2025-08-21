# API Gateway Stack

Lightweight, self-contained API gateway with auto-TLS and path-prefix routing.

## Components

- **Caddy**: automatic HTTP → HTTPS redirect, rate limiting, security headers, reverse-proxy to
  Gateway.
- **Gateway**: Spring Cloud Gateway with prefix stripping and `X-Forwarded-*` support for proper
  redirects.
- **fail2ban**: watches caddy logs for bad behavior patterns and then blocks the IP at the OS
  firewall level.
- **Watchtower**: Automatically pull new docker images.
- **Portainer**: Dashboard to manage docker containers.
- **Backends**: `api-stress-test`, `ichiro-family-tree`, etc.

## How to run

1. Make sure the domain (e.g. `dariolab.com`, `www.dariolab.com`) is pointing to the server’s IP.
2. Edit the config files if you need to change hostnames, paths, or behavior:
    - [docker-compose.yml](docker-compose.yml): orchestrates the whole stack (Caddy, gateway, and
      backend services),
      networking, and volume persistence.
    - [Caddyfile](Caddyfile): defines the public domain, TLS/Let’s Encrypt setup, rate limits,
      security headers, and
      reverse-proxy rules into the gateway.
    - [gateway config](src/main/resources/application.yml): config for the Spring Cloud Gateway
      itself - route
      prefixes, forwarding logic, and downstream service addresses.
    - [ban rules](fail2ban/jail.d/caddy.local): configures IP banning rules.
3. Start everything:
   ```shell
   docker compose up -d --build
   ```

## Caddy image

The app uses a custom caddy image that includes the `caddy-ratelimit` plugin. It is hosted in
my [docker hub](https://hub.docker.com/repository/docker/dariomr8/caddy-with-ratelimit/general).

It is built from the dockerfile [Dockerfile.caddy](Dockerfile.caddy).

To update the image after editing the Dockerfile:

```shell
docker login docker.io

docker buildx build \
  --platform linux/arm64 \
  -t docker.io/dariomr8/caddy-with-ratelimit:2.10.0 \
  -f Dockerfile.caddy \
  --push .
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

### Caddyfile

```shell
# format
caddy fmt --overwrite

# from the container (easier with plugins)
# validate
docker compose exec caddy caddy validate --config /etc/caddy/Caddyfile --adapter caddyfile
```

### fail2ban

```shell
# reload configs (e.g.: after changing fail2ban/jail.d/caddy.local)
docker compose exec fail2ban fail2ban-client reload

# check jails
docker compose exec fail2ban fail2ban-client status
docker compose exec fail2ban fail2ban-client status caddy-429
docker compose exec fail2ban fail2ban-client status caddy-badpaths

# list banned IPs
docker compose exec fail2ban fail2ban-client banned

# unban
docker compose exec fail2ban fail2ban-client set caddy-429 unbanip 86.49.248.100
docker compose exec fail2ban fail2ban-client set caddy-unknownpaths unbanip 86.49.248.100
```