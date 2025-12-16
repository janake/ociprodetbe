package org.prodet.oci.dto;

public record CleanResultDto(
    int deletedDbRows,
    int deletedFiles
) {}

