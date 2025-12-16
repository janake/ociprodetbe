package org.prodet.oci.repository;

import org.prodet.oci.dto.AppGenerationDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

@Repository
public class AppGenerationRepository {

    private final JdbcTemplate jdbcTemplate;

    public AppGenerationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long insertStarted(int requestedCount, OffsetDateTime generationStartedAt) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO app_generation (requested_count, generation_started_at) VALUES (?, ?)",
                new String[]{"ID"}
            );
            ps.setInt(1, requestedCount);
            ps.setObject(2, generationStartedAt);
            return ps;
        }, keyHolder);

        var keys = keyHolder.getKeys();
        if (keys != null) {
            Object id = keys.getOrDefault("ID", keys.get("id"));
            if (id instanceof Number number) {
                return number.longValue();
            }
        }

        Object singleKey = keyHolder.getKey();
        if (singleKey instanceof Number number) {
            return number.longValue();
        }

        throw new IllegalStateException(
            "Insert did not return a numeric generated id (got: " + (singleKey == null ? "null" : singleKey.getClass().getName()) + ")"
        );
    }

    public void updateFinished(long id, OffsetDateTime generationFinishedAt, int createdCount) {
        int updated = jdbcTemplate.update(
            "UPDATE app_generation SET generation_finished_at = ?, created_count = ? WHERE id = ?",
            ps -> {
                ps.setObject(1, generationFinishedAt);
                ps.setInt(2, createdCount);
                ps.setLong(3, id);
            }
        );

        if (updated != 1) {
            throw new IllegalStateException("Expected to update 1 row for app_generation id=" + id + ", but updated " + updated);
        }
    }

    public List<AppGenerationDto> findLatest(int limit) {
        return jdbcTemplate.query(
            """
                SELECT id, requested_count, created_count, generation_started_at, generation_finished_at
                FROM (
                    SELECT id, requested_count, created_count, generation_started_at, generation_finished_at
                    FROM app_generation
                    ORDER BY id DESC
                )
                WHERE ROWNUM <= ?
                """,
            ps -> ps.setInt(1, limit),
            (rs, rowNum) -> {
                long id = rs.getLong("id");
                int requestedCount = rs.getInt("requested_count");
                Integer createdCount = rs.getObject("created_count", Integer.class);
                OffsetDateTime startedAt = rs.getObject("generation_started_at", OffsetDateTime.class);
                OffsetDateTime finishedAt = rs.getObject("generation_finished_at", OffsetDateTime.class);
                Long durationMillis = finishedAt == null ? null : Duration.between(startedAt, finishedAt).toMillis();
                return new AppGenerationDto(id, requestedCount, createdCount, startedAt, finishedAt, durationMillis);
            }
        );
    }

    public int deleteAll() {
        return jdbcTemplate.update("DELETE FROM app_generation");
    }
}

