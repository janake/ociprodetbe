package org.prodet.oci.controller;

import org.prodet.oci.service.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

record FileUploadResponse(String message, String fileName) {}

@RestController
@RequestMapping("/api")
public class FileUploadController {

    private final StorageService storageService;

    @Autowired
    public FileUploadController(StorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping("/upload")
    public ResponseEntity<FileUploadResponse> handleFileUpload(@RequestParam("file") MultipartFile file) {
        storageService.store(file);
        var response = new FileUploadResponse("You successfully uploaded " + file.getOriginalFilename() + "!", file.getOriginalFilename());
        return ResponseEntity.ok().body(response);
    }
}
