package org.prodet.oci;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.prodet.oci.service.FileGenerationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@SpringBootTest
@ActiveProfiles("dev")
class FileGenerationServiceTest {

    @TempDir
    static Path tempDir;

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("storage.location", () -> tempDir.resolve("uploads").toString());
    }

    @Autowired
    FileGenerationService fileGenerationService;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void generatesFilesAndPersistsMetadata() throws Exception {
        var generated = fileGenerationService.generateFiles(6);

        Assertions.assertThat(generated).hasSize(6);
        Integer rows = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM app_file", Integer.class);
        Assertions.assertThat(rows).isEqualTo(6);
        Integer generations = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM app_generation", Integer.class);
        Assertions.assertThat(generations).isEqualTo(1);

        for (var dto : generated) {
            Path file = Paths.get(dto.storagePath()).resolve(dto.fileName());
            Assertions.assertThat(Files.exists(file)).isTrue();
            Assertions.assertThat(dto.generationId()).isNotNull();
            Assertions.assertThat(dto.creationFinishedAt()).isNotNull();
            Assertions.assertThat(dto.fileSizeBytes()).isEqualTo(Files.size(file));
        }
    }

    @Test
    void syncAddsMissingFilesystemFilesWithSameTimestamps() throws Exception {
        Path generatedDir = tempDir.resolve("uploads").resolve("generated");
        Files.createDirectories(generatedDir);

        Path file = generatedDir.resolve("manual-1.json");
        Files.writeString(file, "{\"hello\":\"world\"}");
        FileTime ft = FileTime.from(OffsetDateTime.of(2025, 1, 2, 3, 4, 5, 0, ZoneOffset.UTC).toInstant());
        Files.setLastModifiedTime(file, ft);

        var rows = fileGenerationService.syncFilesystemAndListLatest(200);

        Assertions.assertThat(rows).anySatisfy(dto -> {
            if (!dto.fileName().equals("manual-1.json")) return;
            Assertions.assertThat(dto.creationStartedAt()).isEqualTo(dto.creationFinishedAt());
            Assertions.assertThat(dto.creationStartedAt()).isEqualTo(OffsetDateTime.ofInstant(ft.toInstant(), ZoneOffset.UTC));
            Assertions.assertThat(dto.generationId()).isNull();
            Assertions.assertThat(dto.fileSizeBytes()).isEqualTo(Files.size(file));
        });

        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM app_file WHERE file_name = 'manual-1.json'",
            Integer.class
        );
        Assertions.assertThat(count).isEqualTo(1);
    }

    @Test
    void cleanDeletesGeneratedFilesAndDbRows() throws Exception {
        fileGenerationService.generateFiles(3);
        Integer rowsBefore = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM app_file", Integer.class);
        Assertions.assertThat(rowsBefore).isGreaterThanOrEqualTo(3);
        Integer generationsBefore = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM app_generation", Integer.class);
        Assertions.assertThat(generationsBefore).isGreaterThanOrEqualTo(1);

        Path generatedDir = tempDir.resolve("uploads").resolve("generated");
        Assertions.assertThat(Files.exists(generatedDir)).isTrue();

        var result = fileGenerationService.cleanAllGenerated();

        Integer rowsAfter = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM app_file", Integer.class);
        Assertions.assertThat(rowsAfter).isEqualTo(0);
        Integer generationsAfter = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM app_generation", Integer.class);
        Assertions.assertThat(generationsAfter).isEqualTo(0);
        Assertions.assertThat(result.deletedDbRows()).isEqualTo(rowsBefore);
        Assertions.assertThat(Files.list(generatedDir).findAny()).isEmpty();
    }
}
