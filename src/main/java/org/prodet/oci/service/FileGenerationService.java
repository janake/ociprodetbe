package org.prodet.oci.service;

import org.prodet.oci.config.properties.StorageProperties;
import org.prodet.oci.dto.AppFileDto;
import org.prodet.oci.dto.AppGenerationDto;
import org.prodet.oci.repository.AppGenerationRepository;
import org.prodet.oci.repository.AppFileRepository;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

@Service
public class FileGenerationService {

    private static final int MAX_COUNT = 1_000;
    private static final String XML_PATTERN = "classpath*:media/seed-files/xml/*.xml";
    private static final String JSON_PATTERN = "classpath*:media/seed-files/json/*.json";
    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmssSSS");

    private final AppFileRepository appFileRepository;
    private final AppGenerationRepository appGenerationRepository;
    private final Path generatedDir;

    public FileGenerationService(AppFileRepository appFileRepository, AppGenerationRepository appGenerationRepository, StorageProperties storageProperties) {
        this.appFileRepository = appFileRepository;
        this.appGenerationRepository = appGenerationRepository;
        this.generatedDir = Paths.get(storageProperties.getLocation()).resolve("generated");
    }

    public List<AppFileDto> generateFiles(int count) {
        if (count < 1 || count > MAX_COUNT) {
            throw new IllegalArgumentException("count must be between 1 and " + MAX_COUNT);
        }

        Resource[] xmlSeeds = loadResources(XML_PATTERN);
        Resource[] jsonSeeds = loadResources(JSON_PATTERN);
        if (xmlSeeds.length == 0 || jsonSeeds.length == 0) {
            throw new IllegalStateException("Missing seed files under src/main/resources/media/seed-files (xml/json)");
        }

        Filesystem.mkdirs(generatedDir);

        int xmlCount = count / 2;
        int jsonCount = count - xmlCount;
        List<FileType> types = new ArrayList<>(count);
        types.addAll(Collections.nCopies(xmlCount, FileType.XML));
        types.addAll(Collections.nCopies(jsonCount, FileType.JSON));
        Collections.shuffle(types, ThreadLocalRandom.current());

        OffsetDateTime generationStartedAt = OffsetDateTime.now(ZoneOffset.UTC);
        long generationId = appGenerationRepository.insertStarted(count, generationStartedAt);

        List<AppFileDto> results = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            FileType type = types.get(i);
            Resource seed = pickSeed(type == FileType.XML ? xmlSeeds : jsonSeeds);

            String extension = type == FileType.XML ? "xml" : "json";
            String fileName = "gen-" + OffsetDateTime.now(ZoneOffset.UTC).format(FILE_TS) + "-" + (i + 1) + "-" + randomSuffix() + "." + extension;

            String storagePath = generatedDir.toAbsolutePath().normalize().toString();
            OffsetDateTime startedAt = (i == 0) ? generationStartedAt : OffsetDateTime.now(ZoneOffset.UTC);

            long id = appFileRepository.insertStarted(storagePath, fileName, startedAt, generationId);

            Path target = generatedDir.resolve(fileName).normalize().toAbsolutePath();
            try (InputStream in = seed.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create file: " + target, e);
            }

            long size;
            try {
                size = Files.size(target);
            } catch (IOException e) {
                throw new RuntimeException("Failed to read file size for: " + target, e);
            }

            OffsetDateTime finishedAt = OffsetDateTime.now(ZoneOffset.UTC);
            appFileRepository.updateFinished(id, finishedAt, size);

            if (i == count - 1) {
                appGenerationRepository.updateFinished(generationId, finishedAt, count);
            }

            results.add(new AppFileDto(id, generationId, storagePath, fileName, startedAt, finishedAt, size));
        }

        return results;
    }

    public List<AppGenerationDto> listLatestGenerations(int limit) {
        if (limit < 1 || limit > 1_000) {
            throw new IllegalArgumentException("limit must be between 1 and 1000");
        }
        return appGenerationRepository.findLatest(limit);
    }

    public List<AppFileDto> listLatest(int limit) {
        if (limit < 1 || limit > 5_000) {
            throw new IllegalArgumentException("limit must be between 1 and 5000");
        }
        return appFileRepository.findLatest(limit);
    }

    public List<AppFileDto> syncFilesystemAndListLatest(int limit) {
        if (limit < 1 || limit > 5_000) {
            throw new IllegalArgumentException("limit must be between 1 and 5000");
        }

        Path dir = generatedDir.toAbsolutePath().normalize();
        if (!Files.exists(dir)) {
            return appFileRepository.findLatest(limit);
        }

        Set<String> knownNames = appFileRepository.findFileNamesByStoragePath(dir.toString());

        try (Stream<Path> stream = Files.list(dir)) {
            stream
                .filter(Files::isRegularFile)
                .forEach(path -> {
                    String fileName = path.getFileName().toString();
                    if (knownNames.contains(fileName)) {
                        return;
                    }

                    OffsetDateTime ts = fileTimestampUtc(path);
                    long size = fileSize(path);

                    long id = appFileRepository.insertStarted(dir.toString(), fileName, ts);
                    appFileRepository.updateFinished(id, ts, size);
                });
        } catch (IOException e) {
            throw new RuntimeException("Failed to sync filesystem from: " + dir, e);
        }

        return appFileRepository.findLatest(limit);
    }

    public org.prodet.oci.dto.CleanResultDto cleanAllGenerated() {
        Path dir = generatedDir.toAbsolutePath().normalize();

        int deletedFiles = 0;
        if (Files.exists(dir)) {
            try (Stream<Path> stream = Files.walk(dir)) {
                List<Path> toDelete = stream
                    .sorted((a, b) -> b.getNameCount() - a.getNameCount()) // children first
                    .filter(p -> !p.equals(dir))
                    .toList();

                for (Path p : toDelete) {
                    try {
                        boolean isRegularFile = Files.isRegularFile(p);
                        Files.deleteIfExists(p);
                        if (isRegularFile) {
                            deletedFiles++;
                        }
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to delete: " + p, e);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to list/delete generated files under: " + dir, e);
            }
        }

        Filesystem.mkdirs(dir);
        int deletedDbRows = appFileRepository.deleteAll();
        appGenerationRepository.deleteAll();

        return new org.prodet.oci.dto.CleanResultDto(deletedDbRows, deletedFiles);
    }

    private static OffsetDateTime fileTimestampUtc(Path path) {
        try {
            FileTime ft = Files.getLastModifiedTime(path);
            Instant instant = ft.toInstant();
            return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
        } catch (IOException e) {
            return OffsetDateTime.now(ZoneOffset.UTC);
        }
    }

    private static long fileSize(Path path) {
        try {
            return Files.size(path);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file size for: " + path, e);
        }
    }

    private static Resource[] loadResources(String pattern) {
        try {
            return new PathMatchingResourcePatternResolver().getResources(pattern);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load seed resources: " + pattern, e);
        }
    }

    private static Resource pickSeed(Resource[] seeds) {
        int index = ThreadLocalRandom.current().nextInt(seeds.length);
        return seeds[index];
    }

    private static String randomSuffix() {
        long value = ThreadLocalRandom.current().nextLong();
        return Long.toUnsignedString(value, 16);
    }

    private enum FileType {
        XML,
        JSON
    }

    private static final class Filesystem {
        private static void mkdirs(Path path) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                throw new RuntimeException("Could not create directory: " + path, e);
            }
        }
    }

}
