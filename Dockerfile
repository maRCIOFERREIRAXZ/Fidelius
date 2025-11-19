# ---------- Build stage ----------
FROM gradle:8.10.2-jdk21 AS build

ARG GRADLE_USER_HOME=/home/gradle/.gradle
ENV GRADLE_USER_HOME=${GRADLE_USER_HOME}
WORKDIR /home/gradle/project

# --- Copy wrapper and minimal build metadata first ---
COPY backend/gradlew ./backend/gradlew
COPY backend/gradlew.bat ./backend/gradlew.bat
COPY backend/gradle ./backend/gradle
COPY backend/build.gradle.kts ./backend/build.gradle.kts
COPY backend/settings.gradle.kts ./backend/settings.gradle.kts

# Ensure wrapper is executable and pre-resolve dependencies
RUN set -eux; \
    cd backend; \
    chmod +x ./gradlew; \
    # Use wrapper if present otherwise fallback to system gradle (image has gradle)
    if [ -f ./gradlew ]; then GRADLE_CMD=./gradlew; else GRADLE_CMD=gradle; fi; \
    # A lightweight task to populate the Gradle cache so dependencies layer can be reused.
    $GRADLE_CMD --no-daemon help || true

# --- Copy the rest of the project ---
COPY . .

# Build the fat jar (shadowJar). Use wrapper if present.
WORKDIR /home/gradle/project/backend
RUN set -eux; \
    if [ -f ./gradlew ]; then chmod +x ./gradlew && ./gradlew shadowJar -x test --no-daemon; else gradle shadowJar -x test --no-daemon; fi

# Find produced jar and copy to a stable path
RUN set -eux; \
    out="/home/gradle/server.jar"; \
    jar=$(find build/libs -type f \( -iname '*-all.jar' -o -iname '*-shadow.jar' -o -iname '*shadow*.jar' -o -iname '*.jar' \) | head -n1); \
    if [ -z "$jar" ]; then echo "ERROR: no jar found in build/libs"; exit 1; fi; \
    cp "$jar" "$out"; \
    ls -l "$out"

# ---------- Runtime stage ----------
FROM eclipse-temurin:21-jre-jammy AS runtime

WORKDIR /app

# Copy the built jar from the build stage
COPY --from=build /home/gradle/server.jar ./server.jar

# Mountable data dir
VOLUME /app/data
ENV PORT=8080
EXPOSE 8080

# Create non-root user and set ownership
RUN addgroup --system appgroup \
 && adduser --system --ingroup appgroup appuser \
 && chown -R appuser:appgroup /app

USER appuser

ENTRYPOINT ["java","-jar","/app/server.jar"]
