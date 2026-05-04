package org.mundell.mediaserver.service.eztv.downloader.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mundell.mediaserver.yts.dal.models.TrackerUrl;
import org.mundell.mediaserver.yts.dal.repositories.TrackerUrlRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class TrackerSyncService {

    private static final String TRACKER_LIST_URL = "https://ngosang.github.io/trackerslist/trackers_best.txt";

    private final TrackerUrlRepository trackerUrlRepository;

    @Scheduled(initialDelay = 5_000, fixedDelay = 86_400_000) // 5s after startup, then every 24h
    public void sync() {
        log.info("Syncing tracker list from {}", TRACKER_LIST_URL);
        try {
            List<String> fetched;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(URI.create(TRACKER_LIST_URL).toURL().openStream()))) {
                fetched = reader.lines()
                        .map(String::trim)
                        .filter(line -> !line.isBlank())
                        .collect(Collectors.toList());
            }

            if (fetched.isEmpty()) {
                log.warn("Tracker list returned empty — keeping existing entries");
                return;
            }

            LocalDateTime now = LocalDateTime.now();
            List<TrackerUrl> entries = fetched.stream().map(url -> {
                TrackerUrl t = new TrackerUrl();
                t.setUrl(url);
                t.setUpdatedAt(now);
                return t;
            }).collect(Collectors.toList());

            trackerUrlRepository.deleteAll();
            trackerUrlRepository.saveAll(entries);
            log.info("Tracker list updated — {} trackers stored", entries.size());

        } catch (Exception e) {
            log.error("Failed to sync tracker list: {}", e.getMessage());
        }
    }
}
