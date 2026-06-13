# Development

## Prerequisites

- Docker Desktop with Docker Compose, or
- a local JDK 21 installation

This project requires Java 21.

## Docker Compose workflow

Start a shell in the Gradle container:

```bash
docker compose run --rm gradle bash
```

Run tests:

```bash
docker compose run --rm gradle ./gradlew test --no-daemon
```

Build the mod jars:

```bash
docker compose run --rm gradle ./gradlew build --no-daemon
```

Run a clean build:

```bash
docker compose run --rm gradle ./gradlew clean build --no-daemon
```

## Native JDK 21 workflow

Run tests:

```bash
./gradlew test
```

Build the mod jars:

```bash
./gradlew build
```

## Build outputs

- Fabric 1.21.11 jar: `fabric-1_21_11/build/libs/`

## Release notes

Release and publish tasks require `SAPS_TOKEN`, `CURSE_DEPLOY_TOKEN`, and `GITHUB_TOKEN`.
They are not needed for normal local test or build runs.

## Troubleshooting

- If Gradle reports a JVM/version mismatch, make sure you are using Java 21.
- If Docker Compose commands fail, verify Docker Desktop is running and the repo is mounted at `/workspace`.
