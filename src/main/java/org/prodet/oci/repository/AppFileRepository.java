package org.prodet.oci.repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.prodet.oci.dto.AppFileDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class AppFileRepository {

    private final JdbcTemplate jdbcTemplate;

    public AppFileRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long insertStarted(String storagePath, String fileName, OffsetDateTime creationStartedAt) {
        return insertStarted(storagePath, fileName, creationStartedAt, null);
    }

    public long insertStarted(String storagePath, String fileName, OffsetDateTime creationStartedAt, Long generationId) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO app_file (storage_path, file_name, creation_started_at, generation_id) VALUES (?, ?, ?, ?)",
                new String[]{"ID"}
            );
            ps.setString(1, storagePath);
            ps.setString(2, fileName);
            ps.setObject(3, creationStartedAt);
            ps.setObject(4, generationId);
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

    public void updateFinished(long id, OffsetDateTime creationFinishedAt, long fileSizeBytes) {
        int updated = jdbcTemplate.update(
            "UPDATE app_file SET creation_finished_at = ?, file_size_bytes = ? WHERE id = ?",
            ps -> {
                ps.setObject(1, creationFinishedAt);
                ps.setLong(2, fileSizeBytes);
                ps.setLong(3, id);
            }
        );

        if (updated != 1) {
            throw new IllegalStateException("Expected to update 1 row for app_file id=" + id + ", but updated " + updated);
        }
    }

    public List<AppFileDto> findLatest(int limit) {
        return jdbcTemplate.query(
            """
                SELECT id, generation_id, storage_path, file_name, creation_started_at, creation_finished_at, file_size_bytes
                FROM (
                    SELECT id, generation_id, storage_path, file_name, creation_started_at, creation_finished_at, file_size_bytes
                    FROM app_file
                    ORDER BY creation_started_at DESC, id DESC
                )
                WHERE ROWNUM <= ?
                """,
            ps -> ps.setInt(1, limit),
            (rs, rowNum) -> new AppFileDto(
                rs.getLong("id"),
                getNullableLong(rs, "generation_id"),
                rs.getString("storage_path"),
                rs.getString("file_name"),
                rs.getObject("creation_started_at", OffsetDateTime.class),
                rs.getObject("creation_finished_at", OffsetDateTime.class),
                rs.getLong("file_size_bytes")
            )
        );
    }

    public Set<String> findFileNamesByStoragePath(String storagePath) {
        List<String> names = jdbcTemplate.query(
            "SELECT file_name FROM app_file WHERE storage_path = ?",
            ps -> ps.setString(1, storagePath),
            (rs, rowNum) -> rs.getString("file_name")
        );
        return new HashSet<>(names);
    }

    public int deleteAll() {
        return jdbcTemplate.update("DELETE FROM app_file");
    }

    private static Long getNullableLong(ResultSet rs, String columnLabel) throws java.sql.SQLException {
        Object value = rs.getObject(columnLabel);
        if (value == null) return null;
        if (value instanceof Number number) return number.longValue();
        throw new IllegalStateException("Unsupported numeric type for " + columnLabel + ": " + value.getClass().getName());
    }
}
