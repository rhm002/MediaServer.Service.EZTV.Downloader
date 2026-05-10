package org.mundell.mediaserver.service.eztv.downloader.services;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SeriesFileRelocatorService {

    private static final Set<String> VIDEO_EXTENSIONS = Set.of("mkv", "mp4", "avi", "mov", "ts");

    /**
     * Moves a downloaded series folder from the torrent staging area into the series library.
     *
     * <p>Source: {@code downloadTarget/{title}/}
     * <p>Destination: {@code destinationDir/{title}/}
     *
     * <p>If the destination folder already exists it is deleted before the move.
     *
     * @param downloadTarget root of the torrent staging area (SERIES_DOWNLOAD_LOCATION)
     * @param destinationDir root of the series library (SERIES_LOCATION)
     * @param title          formatted series/episode title used as the folder name
     */
    public void relocate(String downloadTarget, String destinationDir, String title) {
        Path source = Paths.get(downloadTarget, title);
        Path target = Paths.get(destinationDir, title);

        if (!source.toFile().exists() || !source.toFile().isDirectory()) {
            log.warn("Downloaded folder not found or not a directory — skipping relocate: {}", source);
            return;
        }

        File[] videoFiles = source.toFile().listFiles(
                (dir, name) -> VIDEO_EXTENSIONS.stream().anyMatch(name.toLowerCase()::endsWith));

        if (videoFiles == null || videoFiles.length == 0) {
            log.warn("No video files found in downloaded folder — skipping relocate: {}", source);
            return;
        }

        File targetFile = target.toFile();
        if (targetFile.exists()) {
            log.info("Destination already exists — deleting: {}", target);
            try {
                FileUtils.deleteDirectory(targetFile);
            } catch (IOException e) {
                log.error("Failed to delete existing destination '{}': {}", target, e.getMessage(), e);
                return;
            }
        }

        try {
            Files.createDirectories(target);
        } catch (IOException e) {
            log.error("Failed to create destination directory '{}': {}", target, e.getMessage(), e);
            return;
        }

        for (File file : videoFiles) {
            Path dest = target.resolve(file.getName());
            try {
                Files.move(file.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
                log.info("Relocated '{}' → '{}'", file.getAbsolutePath(), dest);
            } catch (IOException e) {
                log.error("Failed to move '{}' to '{}': {}", file.getAbsolutePath(), dest, e.getMessage(), e);
            }
        }

        File[] remainingFiles = source.toFile().listFiles(File::isFile);
        if (remainingFiles != null) {
            for (File file : remainingFiles) {
                Path dest = target.resolve(file.getName());
                try {
                    Files.move(file.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
                    log.info("Relocated companion file '{}' → '{}'", file.getAbsolutePath(), dest);
                } catch (IOException e) {
                    log.warn("Failed to move companion file '{}' to '{}': {}", file.getAbsolutePath(), dest, e.getMessage(), e);
                }
            }
        }

        try {
            FileUtils.deleteDirectory(source.toFile());
            log.debug("Cleaned up staging folder: {}", source);
        } catch (IOException e) {
            log.warn("Failed to clean up staging folder '{}': {}", source, e.getMessage(), e);
        }
    }
}
