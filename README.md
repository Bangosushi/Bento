# Bento

Bento is a dual-component system for managing a Minecraft server: a Fabric mod that runs on the server and a Spring Boot Discord bot that handles player onboarding, whitelist management, and moderation through Discord. Together, they automate server access control and player interview workflows.

## Stack

- **Language(s):** Java (99.6%), Dockerfile (0.4%)
- **Framework / runtime:** Spring Boot 3 (Discord bot), Fabric Loader 0.19.5+ (Minecraft mod), Java 25
- **Notable libraries:** JDA (Java Discord API), Spring Data JPA, Flyway (database migrations), PostgreSQL, Fabric API

## Organization

```
bento-bot/                 Spring Boot Discord bot (REST API + event handlers)
  src/main/
    java/com/bento/bot/    Bot logic: Discord listeners, commands, services
    resources/
      application.yml      Configuration: Discord token, DB, Minecraft mod URLs
      db/                  Flyway migrations for PostgreSQL schema
  Dockerfile               Container image for bot deployment

bento-mod/                 Fabric mod for Minecraft server
  src/main/
    java/com/bento/mod/    Mod entry point: whitelist/op sync, join events
    resources/
      fabric.mod.json      Mod metadata (entry point, version, dependencies)
  gradle.properties        Minecraft/loader versions

docker-compose.yml         Services: bot container, PostgreSQL 16
build.gradle.kts           Multi-project Gradle build (both modules)
settings.gradle.kts        Project includes
.env.example               Environment variables: Discord tokens, channel/role IDs, mod URLs
```

### How it works

When a player joins the Minecraft server, the Fabric mod detects the join event and sends it to the bot's REST API. The bot runs Discord interview workflows (7 onboarding questions) in a welcome channel, stores responses in PostgreSQL, and syncs whitelist/op status back to the server via the mod. A scheduled cron job (2 AM daily) flags inactive players for removal after configurable thresholds (7 no-show days, 60 inactive days). The bot and mod communicate over HTTP (configurable URLs for survival/creative servers).

## Getting Started

### Prerequisites

- Docker and Docker Compose (for containerized setup)
- Java 25+ (for local development)
- PostgreSQL 16 (if running locally without Docker)

### Setup

1. **Set up environment variables** from `.env.example`:
   ```bash
   cp .env.example .env
   # Edit .env with your Discord token, guild ID, channel IDs, role IDs, and Minecraft mod URLs
   ```

2. **Run with Docker Compose** (bot + PostgreSQL):
   ```bash
   docker-compose up -d
   ```
   The bot starts on port 8080 and connects to PostgreSQL.

3. **Or build and run locally** (requires PostgreSQL running separately):
   ```bash
   ./gradlew build
   java -jar bento-bot/build/libs/bento-bot-1.0.0-SNAPSHOT.jar
   ```

4. **Build the Fabric mod** for your server:
   ```bash
   ./gradlew bento-mod:build
   # JAR available at: bento-mod/build/libs/bento-mod-1.0.0.jar
   ```

## Configuration

The bot is configured via environment variables (see `.env.example`):

- **Database:** `DB_URL`, `DB_USER`, `DB_PASSWORD`
- **Discord:** `DISCORD_TOKEN`, `GUILD_ID`, channel/role IDs for welcome, review, general channels
- **Minecraft:** `SURVIVAL_MOD_URL`, `CREATIVE_MOD_URL` — URLs where the mod can reach the bot's REST API
- **Inactivity rules:** Cron schedule and no-show/inactive day thresholds (configurable in `application.yml`)

## Features

- 🤖 **Discord-driven onboarding:** Automated 7-question interview workflow
- 📋 **Whitelist management:** Sync player whitelist/op status between Discord and Minecraft
- ⏱️ **Inactivity detection:** Automatic flagging of inactive/no-show players
- 🔗 **Bi-directional communication:** Fabric mod ↔ Spring Boot bot via REST API
- 🐘 **PostgreSQL persistence:** Stores player applications and state
