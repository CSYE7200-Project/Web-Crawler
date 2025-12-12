# Web-Crawler

A multi-module **distributed web crawler** built with **sbt** and **Java 17**.

---

## Project Layout

This repository is organized as multiple sbt modules:

| Module | Description |
|--------|-------------|
| `crawler-api` | Public API definitions and shared contracts |
| `crawler-core` | Core crawling logic and utilities |
| `crawler-master` | Orchestration and coordination of workers |
| `crawler-worker` | Individual crawl execution units |

---

## Prerequisites

- **Java 17** – Verify with `java -version`
- **sbt** – Verify with `sbt --version`
- **Unix-like shell** – macOS / Linux. On Windows, use WSL or Git Bash.

---

## Build & Run

Execute the following commands from the repository root.

### 1. Clean and Compile

```bash
sbt clean compile
```

### 2. Stage the Master Distribution

```bash
sbt master/stage
```

### 3. Run the Master

```bash
./crawler-master/target/universal/stage/bin/crawler-master <workers> <concurrency>
```

**Example:**

```bash
./crawler-master/target/universal/stage/bin/crawler-master 20 20
```

| Parameter | Description |
|-----------|-------------|
| `workers` | Number of worker instances to spawn |
| `concurrency` | Level of concurrent work per worker |

Adjust both values to suit your environment and workload requirements.

---

## Troubleshooting

### Permission Denied

If the staged binary lacks execute permissions, run:

```bash
chmod +x ./crawler-master/target/universal/stage/bin/crawler-master
```

---