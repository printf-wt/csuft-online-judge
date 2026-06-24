# First-phase production requirements

The application refuses to start with the `prod` profile unless database credentials,
a strong JWT secret, and sandbox judge execution are explicitly configured.

## Required environment

Start from `.env.example`. Never commit real values.

- `SPRING_PROFILES_ACTIVE=prod`
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- `JWT_SECRET`: at least 32 random bytes
- `JUDGE_EXECUTION_MODE=SANDBOX`
- `JUDGE_SANDBOX_COMMAND`: absolute path to the sandbox adapter

## Docker sandbox adapter

The supplied `scripts/csuft-oj-sandbox.sh` is intended for a Linux judge host with Docker.
Install it outside the application directory, make it executable, and configure its path:

```bash
install -m 0755 scripts/csuft-oj-sandbox.sh /usr/local/bin/csuft-oj-sandbox
export JUDGE_SANDBOX_COMMAND=/usr/local/bin/csuft-oj-sandbox
```

Pre-pull the pinned runtime images before accepting submissions:

```bash
docker pull gcc:14
docker pull eclipse-temurin:17-jdk
docker pull python:3.12
docker pull golang:1.23
```

The adapter disables networking and capabilities, runs without root privileges, uses a
read-only container root, mounts the workspace read-only while user programs run, and
applies CPU, memory, file, process-count, and temporary-storage limits. The application
separately enforces wall-clock and output limits.

Do not expose Docker's remote API. Run the judge worker on a dedicated host and restrict
the service account to the minimum Docker permissions available in the deployment.

## Queue recovery

The in-process queue is bounded. Submission state remains in MySQL, and pending or
interrupted states are recovered after restart and whenever worker capacity becomes
available. For horizontal scaling, replace `JudgeTaskPublisher` with Redis Stream or
RabbitMQ before running more than one API instance.
