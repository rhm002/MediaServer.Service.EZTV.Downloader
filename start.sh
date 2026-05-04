#!/bin/bash
# MediaServer.Service.EZTV.Downloader Start Script
# Loads .env from the same directory and starts the application JAR.

set -e

find . -name "*.log" -type f -delete
find . -name "*.log.gz" -type f -delete

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
ENV_FILE="${SCRIPT_DIR}/.env"
JAR="${SCRIPT_DIR}/target/MediaServer.Service.EZTV.Downloader.jar"

# ----- Load .env -----
if [ -f "${ENV_FILE}" ]; then
    set -o allexport
    # shellcheck source=.env
    source "${ENV_FILE}"
    set +o allexport
    echo "Loaded environment from ${ENV_FILE}"
else
    echo "WARNING: No .env file found at ${ENV_FILE}. Using system environment."
fi

# ----- Validate JAR -----
if [ ! -f "${JAR}" ]; then
    echo "ERROR: JAR not found at ${JAR}"
    echo "Run './mvnw clean package -DskipTests' first."
    exit 1
fi

# ----- Start -----
echo "Starting MediaServer.Service.EZTV.Downloader (profile: ${SPRING_PROFILES_ACTIVE:-psql-docker})..."
cd "${SCRIPT_DIR}"
java \
    -jar "${JAR}" \
    --spring.profiles.active="${SPRING_PROFILES_ACTIVE:-psql-docker}" \
    --spring.datasource.url="${SPRING_DATASOURCE_URL}" \
    --spring.datasource.username="${SPRING_DATASOURCE_USERNAME}" \
    --spring.datasource.password="${SPRING_DATASOURCE_PASSWORD}"
