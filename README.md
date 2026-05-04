# MediaServer.Service.EZTV.Downloader

Spring Boot microservice that polls `eztv_downloader_queue` for pending entries and downloads TV episodes via BitTorrent magnet URL.

## How It Works

1. On startup, any entries stuck in `PROCESSING` status are reset to `NEW` (crash recovery)
2. A scheduler polls the queue every 60 seconds for `NEW` entries
3. Each entry is dispatched to a virtual-thread executor — concurrency capped by a `Semaphore`
4. The magnet URL is read directly from `EztvDownloaderQueue.magnet` (stored when the entry was created)
5. `BitlyDownloader` (from `yts-dal`) manages the bt4j torrent session
6. On completion, episode files are moved from `SERIES_DOWNLOAD_LOCATION` (staging) to `SERIES_LOCATION/{title}/`
7. Queue entry status and progress are updated throughout: `NEW → PROCESSING → COMPLETED | FAILED`

## Configuration

| Property | Default | Description |
|---|---|---|
| `application.downloader.max-concurrent` | `10` | Max parallel downloads |
| `application.downloader.initial-delay-ms` | `15000` | Delay before first poll (ms) |
| `application.downloader.poll-delay-ms` | `60000` | Polling interval (ms) |

### Database Config Keys (from `config` table)

| Key | Description |
|---|---|
| `SERIES_DOWNLOAD_LOCATION` | Staging directory for active downloads |
| `SERIES_LOCATION` | Final destination for completed episodes |

## Building

```bash
./mvnw clean package -DskipTests
```

## Running

Copy `.env.example` to `.env` and fill in values, then:

```bash
./start.sh              # Linux/WSL — defaults to psql-docker profile
start.bat [profile]     # Windows — defaults to psql-sphere profile
```

Available profiles: `psql-docker`, `psql-sphere`, `mssql`, `mssql-docker`, `mssql-sphere`, `k8s-psql`, `k8s-mssql`

## Docker

```bash
# Build
docker build -t rhm002/media-server:eztv-downloader-service-latest .

# Run
docker run --rm \
  -e SPRING_PROFILES_ACTIVE=psql-docker \
  -e SPRING_DATASOURCE_URL="jdbc:postgresql://host.docker.internal:5432/mediaserver" \
  -e SPRING_DATASOURCE_USERNAME=media \
  -e SPRING_DATASOURCE_PASSWORD=secret \
  rhm002/media-server:eztv-downloader-service-latest
```

## Dependencies

| Artifact | Purpose |
|---|---|
| `eztv-dal:2.0` | `EztvDownloaderQueue`, `EztvDownloaderQueueRepository` |
| `yts-dal:2.0` | `BitlyDownloader`, `DownloadListener`, `DownloadStatusCodes` |
| `dal-common:2.0` | `ConfigurationService` (reads `config` table) |
| `bt-core / bt-http-tracker-client / bt-dht` | bt4j BitTorrent client |
