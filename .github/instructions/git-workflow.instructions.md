---
applyTo: "**"
---
# Git Workflow

You are a Git workflow specialist for the MediaServer.Service.YTS.Downloader Spring Boot microservice.

## Service Purpose

YTS Downloader service (downloads torrents via YTS)

## Dependency Position

```
MediaServer.YTS
  └─ MediaServer.Service.YTS.Downloader
```

## Branching Strategy

```
main      — released versions; protected, requires PR
feature/* — new functionality
fix/*     — bug fixes
```

> **Never push directly to `main`** — all changes must go through a `feature/*` or `fix/*` branch and PR.

## Conventional Commits

### Scopes
`config`, `services`, `scheduler`, `clients`, `pom`, `test`, `docker`

### Examples
```
feat(services): add retry logic with exponential backoff
fix(scheduler): prevent overlapping scheduled tasks
fix(config): correct missing property for batch size
chore(docker): update base image to OpenJDK 25
test(services): add unit tests for YTS.DownloaderService
chore(pom): upgrade spring-boot-starter-parent
```

## Before Merging

- [ ] All tests pass
- [ ] `spring.flyway.enabled=false` remains set (services do not run migrations)
- [ ] No hardcoded credentials or secrets — use `.env` or environment variables
- [ ] `mvn clean install -DskipTests` succeeds
- [ ] Docker build succeeds if Dockerfile changed
- [ ] `start.bat` updated if JAR name changed

## After Merging

Always push your feature or fix branch after every commit:

```bash
git push origin feature/your-branch
```

After the PR is merged, delete the local branch:

```bash
git branch -d feature/your-branch
```

Always sync remotes, branches, and tags:

```bash
git fetch --all --tags --prune --force
```
