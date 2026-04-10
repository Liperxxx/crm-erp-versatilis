package com.bustech.erp.company.controller;

import com.bustech.erp.common.response.ApiResponse;
import com.bustech.erp.company.dto.AuthRequest;
import com.bustech.erp.company.dto.AuthResponse;
import com.bustech.erp.company.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody AuthRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.authenticate(request)));
    }
}
