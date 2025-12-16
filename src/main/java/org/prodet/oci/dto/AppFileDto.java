package org.prodet.oci.dto;

import java.time.OffsetDateTime;

public record AppFileDto(
    long id,
    Long generationId,
    String storagePath,
    String fileName,
    OffsetDateTime creationStartedAt,
    OffsetDateTime creationFinishedAt,
    long fileSizeBytes
) {}
