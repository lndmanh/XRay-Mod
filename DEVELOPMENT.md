# Development

## Prerequisites

- Docker Desktop with Docker Compose, or
- a local JDK 25 installation

This project requires Java 25.

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

## Native JDK 25 workflow

Run tests:

```bash
./gradlew test
```

Build the mod jars:

```bash
./gradlew build
```

## Build outputs

- Fabric jar: `fabric/build/libs/`
- NeoForge jar: `neoforge/build/libs/`

## Release notes

Release and publish tasks require `SAPS_TOKEN`, `CURSE_DEPLOY_TOKEN`, and `GITHUB_TOKEN`.
They are not needed for normal local test or build runs.

## Troubleshooting

- If Gradle reports it needs JVM 17 or newer, make sure you are using Java 25.
- If Docker Compose commands fail, verify Docker Desktop is running and the repo is mounted at `/workspace`.
