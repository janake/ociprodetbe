package org.prodet.oci.controller;

import org.prodet.oci.dto.AppFileDto;
import org.prodet.oci.dto.AppGenerationDto;
import org.prodet.oci.dto.CleanResultDto;
import org.prodet.oci.service.FileGenerationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@RequestMapping("/api/files")
public class FileGenerationController {

    private final FileGenerationService fileGenerationService;

    public FileGenerationController(FileGenerationService fileGenerationService) {
        this.fileGenerationService = fileGenerationService;
    }

    @GetMapping
    public ResponseEntity<List<AppFileDto>> listLatest(@RequestParam(name = "limit", defaultValue = "200") int limit) {
        try {
            return ResponseEntity.ok(fileGenerationService.listLatest(limit));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(BAD_REQUEST, e.getMessage(), e);
        }
    }

    @GetMapping("/generations")
    public ResponseEntity<List<AppGenerationDto>> listLatestGenerations(@RequestParam(name = "limit", defaultValue = "50") int limit) {
        try {
            return ResponseEntity.ok(fileGenerationService.listLatestGenerations(limit));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(BAD_REQUEST, e.getMessage(), e);
        }
    }

    @PostMapping("/generate")
    public ResponseEntity<List<AppFileDto>> generate(@RequestParam("count") int count) {
        try {
            return ResponseEntity.ok(fileGenerationService.generateFiles(count));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(BAD_REQUEST, e.getMessage(), e);
        }
    }

    @PostMapping("/sync")
    public ResponseEntity<List<AppFileDto>> sync(@RequestParam(name = "limit", defaultValue = "200") int limit) {
        try {
            return ResponseEntity.ok(fileGenerationService.syncFilesystemAndListLatest(limit));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(BAD_REQUEST, e.getMessage(), e);
        }
    }

    @PostMapping("/clean")
    public ResponseEntity<CleanResultDto> clean() {
        return ResponseEntity.ok(fileGenerationService.cleanAllGenerated());
    }
}
