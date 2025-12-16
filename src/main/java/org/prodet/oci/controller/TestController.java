package org.prodet.oci.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class TestController {

    @GetMapping("/teszt")
    public Map<String, String> getTestMessage() {
        return Map.of("Hello", "Bello", "Foo", "Bar", "hogy", "smint", "version,", "1.0.1");
    }
}
