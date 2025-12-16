package org.prodet.oci.dto;

import java.time.OffsetDateTime;

public record AppGenerationDto(
    long id,
    int requestedCount,
    Integer createdCount,
    OffsetDateTime generationStartedAt,
    OffsetDateTime generationFinishedAt,
    Long durationMillis
) {}

