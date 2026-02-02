package com.nqt.api_gateway.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
public class SwaggerProxyController {

    @Autowired
    private WebClient.Builder webClientBuilder;

    @Autowired
    private DiscoveryClient discoveryClient;

    @Value("${app.api-prefix}")
    private String apiPrefix;


    @GetMapping("/v3/api-docs/{serviceName}")
    public Mono<Map<String, Object>> getSwagger(@PathVariable String serviceName) {
        // 1. Xác định shortName để làm prefix (ví dụ: identity-service -> identity)
        String shortName = serviceName.replace("-service", "").toLowerCase();

        // 2. Tìm instance của service từ Discovery Client
        return Mono.justOrEmpty(discoveryClient.getInstances(serviceName).stream().findFirst())
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Service not found: " + serviceName)))
                .flatMap(instance -> {
                    String url = instance.getUri() + "/v3/api-docs";

                    // 3. Gọi WebClient với TypeReference để tránh lỗi "incompatible types"
                    return webClientBuilder.build()
                            .get()
                            .uri(url)
                            .retrieve()
                            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                            .map(swagger -> modifySwaggerJson(swagger, shortName));
                });
    }


    @SuppressWarnings("unchecked")
    private Map<String, Object> modifySwaggerJson(Map<String, Object> swagger, String shortName) {
        // 1. Xử lý sửa lại các đường dẫn (Paths)
        Object pathsObj = swagger.get("paths");
        if (pathsObj instanceof Map) {
            Map<String, Object> oldPaths = (Map<String, Object>) pathsObj;
            Map<String, Object> newPaths = new LinkedHashMap<>();

            oldPaths.forEach((path, details) -> {
                // Biến đổi: /login -> /api/identity/login
                String newPath = apiPrefix + shortName + path;
                newPaths.put(newPath, details);
            });

            swagger.put("paths", newPaths);
        }

        swagger.put("servers", List.of(Map.of(
                "url", "/",
                "description", "Gateway Server"
        )));

        return swagger;
    }
}