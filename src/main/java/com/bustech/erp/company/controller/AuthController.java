package com.bustech.erp.company.controller;

import com.bustech.erp.common.response.ApiResponse;
import com.bustech.erp.company.dto.AuthRequest;
import com.bustech.erp.company.dto.AuthResponse;
import com.bustech.erp.company.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody AuthRequest request) {
        log.debug("POST /api/auth/login - email={}", request.email());
        AuthResponse response = authService.authenticate(request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
