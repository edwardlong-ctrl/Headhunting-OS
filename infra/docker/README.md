# Docker Baseline

The root `docker-compose.yml` remains the safe local dependency stack for development.

Task 39 adds a provider-neutral production-like deployment baseline at:

```text
infra/docker/compose.production-like.yml
```

Use it only with reviewed environment values:

```sh
docker compose \
  --env-file infra/deployment/production-like.env.example \
  -f infra/docker/compose.production-like.yml \
  up --build
```

This baseline defines application image build paths for `services/core-api` and `apps/web`, PostgreSQL, MinIO as a local object-storage equivalent, and health-gated startup ordering. It does not create cloud resources, HTTPS certificates, or a real production domain.
