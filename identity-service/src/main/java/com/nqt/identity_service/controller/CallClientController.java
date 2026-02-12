package com.nqt.identity_service.controller;

import com.nqt.identity_service.dto.response.APIResponse;
import com.nqt.identity_service.repository.httpclient.DemoServiceClient;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/test-call")
public class CallClientController {
    DemoServiceClient demoServiceClient;

    @PostMapping
    public ResponseEntity<APIResponse<String>> callDemo(@RequestBody String message) {
        return demoServiceClient.testClient(message);
    }
}
