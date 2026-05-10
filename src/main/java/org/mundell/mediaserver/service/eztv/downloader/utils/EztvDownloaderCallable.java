package org.mundell.mediaserver.service.eztv.downloader.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.mundell.mediaserver.eztv.dal.models.EztvDownloaderQueue;
import org.mundell.mediaserver.eztv.dal.repositories.EztvDownloaderQueueRepository;
import org.mundell.mediaserver.yts.dal.common.DownloadStatusCodes;
import org.mundell.mediaserver.yts.dal.listeners.DownloadListener;
import org.mundell.mediaserver.yts.dal.utils.BitlyDownloader;

import bt.metainfo.Torrent;
import bt.metainfo.TorrentFile;
import bt.torrent.TorrentSessionState;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EztvDownloaderCallable implements Callable<Boolean> {

    private static final List<String> SUPPORTED_EXTENSIONS = new ArrayList<>(
            Arrays.stream("mkv,mp4,avi,mov,ts,srt".split(",")).toList()
    );

    private final String targetDirectory;
    private EztvDownloaderQueue queue;
    private final EztvDownloaderQueueRepository downloaderQueueRepository;

    public EztvDownloaderCallable(String downloadTarget,
                                   EztvDownloaderQueue queue,
                                   EztvDownloaderQueueRepository downloaderQueueRepository) {
        this.targetDirectory         = downloadTarget;
        this.queue                   = queue;
        this.downloaderQueueRepository = downloaderQueueRepository;
    }

    @Override
    public Boolean call() {
        String magnet = queue.getMagnet();
        if (magnet == null || magnet.isBlank()) {
            log.warn("No magnet URL for queue entry '{}' — skipping", queue.getTitle());
            queue.setStatus(DownloadStatusCodes.FAILED.getStatus());
            queue.setError("No magnet URL available");
            queue = downloaderQueueRepository.save(queue);
            return false;
        }

        DecimalFormat df = new DecimalFormat("#.00");
        BitlyDownloader downloader = new BitlyDownloader(Paths.get(this.targetDirectory));
        downloader.init();

        try {
            log.debug("Starting download: {}", queue.getTitle());
            queue.setStatus(DownloadStatusCodes.PROCESSING.getStatus());
            queue = downloaderQueueRepository.save(queue);

            downloader.magnetDownload(magnet, new DownloadListener() {

                @Override
                public void update(Torrent torrent) {
                    // Torrent metadata identified — no action needed
                }

                @Override
                public void update(TorrentSessionState state) {
                    double pct = (((double) state.getPiecesComplete()) / state.getPiecesTotal()) * 100;
                    queue.setProgress(Double.parseDouble(df.format(pct)));
                    queue = downloaderQueueRepository.save(queue);
                    log.debug("{}: peers={}, progress={}%",
                            queue.getTitle(), state.getConnectedPeers().size(), df.format(pct));
                }

                @Override
                public void completed(Torrent torrentFile) {
                    log.debug("Completed: {}", queue.getTitle());
                    moveMedia(torrentFile.getName(), queue.getTitle(), torrentFile);
                    queue.setStatus(DownloadStatusCodes.COMPLETED.getStatus());
                    queue.setError(null);
                    queue = downloaderQueueRepository.save(queue);
                }
            });
        } catch (Exception e) {
            log.error("Download failed for '{}': {}", queue.getTitle(), e.getMessage());
            queue.setStatus(DownloadStatusCodes.FAILED.getStatus());
            queue.setError(e.getMessage());
            queue = downloaderQueueRepository.save(queue);
        }

        return false;
    }

    private void moveMedia(String source, String destination, Torrent torrentFile) {
        var sourceFolder      = Paths.get(this.targetDirectory, source).toFile();
        var destinationFolder = Paths.get(this.targetDirectory, destination).toFile();

        if (!destinationFolder.exists()) {
            destinationFolder.mkdirs();
        }

        if (!sourceFolder.exists()) {
            for (TorrentFile selected : torrentFile.getFiles()) {
                for (String file : selected.getPathElements()) {
                    try {
                        var item = Paths.get(this.targetDirectory, file).toFile();
                        FileUtils.moveFile(item,
                                Paths.get(destinationFolder.getAbsolutePath(),
                                        String.format("%s.%s", destination, FilenameUtils.getExtension(item.getName()))).toFile(),
                                StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        log.error("Failed to move file: {}", e.getMessage());
                    }
                }
            }
        } else {
            File[] files = sourceFolder.listFiles((dir, name) ->
                    SUPPORTED_EXTENSIONS.stream().anyMatch(name::endsWith));
            if (files != null) {
                for (File selected : files) {
                    try {
                        FileUtils.moveFile(selected,
                                Paths.get(destinationFolder.getAbsolutePath(),
                                        String.format("%s.%s", destination, FilenameUtils.getExtension(selected.getName()))).toFile(),
                                StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        log.error("Failed to move file: {}", e.getMessage());
                    }
                }
            }
            try {
                if (!source.equals(destination)) {
                    FileUtils.deleteDirectory(sourceFolder);
                } else {
                    File[] remaining = sourceFolder.listFiles((dir, name) ->
                            SUPPORTED_EXTENSIONS.stream().noneMatch(name::endsWith));
                    if (remaining != null) {
                        for (File leftover : remaining) {
                            try {
                                FileUtils.delete(leftover);
                            } catch (IOException e) {
                                log.error("Failed to delete leftover: {}", e.getMessage());
                            }
                        }
                    }
                }
            } catch (IOException e) {
                log.error("Failed to clean staging directory: {}", e.getMessage());
            }
        }
    }
}
