package org.mundell.mediaserver.service.eztv.downloader.services;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mundell.mediaserver.eztv.dal.models.EztvDownloaderQueue;
import org.mundell.mediaserver.eztv.dal.repositories.EztvDownloaderQueueRepository;
import org.mundell.mediaserver.service.eztv.downloader.configs.MediaServerConfiguration;
import org.mundell.mediaserver.service.eztv.downloader.utils.EztvDownloaderCallable;
import org.mundell.mediaserver.yts.dal.common.DownloadStatusCodes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

@Slf4j
@Component
@RequiredArgsConstructor
public class EztvDownloadService {

    @Value("${application.downloader.max-concurrent:10}")
    private int maxConcurrent;

    private final EztvDownloaderQueueRepository downloaderQueueRepository;
    private final MediaServerConfiguration config;

    private Semaphore semaphore;
    private ExecutorService executor;

    @PostConstruct
    public void init() {
        semaphore = new Semaphore(maxConcurrent);
        executor  = Executors.newVirtualThreadPerTaskExecutor();

        // Reset any entries stuck in PROCESSING state from a previous crash
        List<EztvDownloaderQueue> stuck = downloaderQueueRepository.findAllByStatus(DownloadStatusCodes.PROCESSING.getStatus());
        stuck.forEach(q -> q.setStatus(DownloadStatusCodes.NEW.getStatus()));
        if (!stuck.isEmpty()) {
            downloaderQueueRepository.saveAll(stuck);
            log.warn("Reset {} stuck PROCESSING entries to NEW on startup", stuck.size());
        }
    }

    @Scheduled(
            initialDelayString = "${application.downloader.initial-delay-ms:15000}",
            fixedDelayString   = "${application.downloader.poll-delay-ms:60000}")
    public void scheduledDownload() {
        List<EztvDownloaderQueue> pending = downloaderQueueRepository.findAllByStatus(DownloadStatusCodes.NEW.getStatus());
        if (pending.isEmpty()) {
            log.debug("No pending EZTV downloads");
            return;
        }

        log.info("Pending EZTV downloads: {} | available slots: {}/{}", pending.size(), semaphore.availablePermits(), maxConcurrent);

        String downloadTarget  = config.getSeriesDownloadLocation();
        String seriesDestination = config.getSeriesLocation();

        for (EztvDownloaderQueue item : pending) {
            if (!semaphore.tryAcquire()) {
                log.info("All {} download slots in use — deferring {} remaining item(s)", maxConcurrent, pending.size());
                break;
            }
            EztvDownloaderCallable callable = new EztvDownloaderCallable(downloadTarget, seriesDestination, item, downloaderQueueRepository);
            CompletableFuture.supplyAsync(() -> {
                try {
                    return callable.call();
                } catch (Exception e) {
                    log.error("Unhandled download error for '{}': {}", item.getTitle(), e.getMessage(), e);
                    return false;
                }
            }, executor).whenComplete((v, ex) -> semaphore.release());
        }
    }
}
