package com.bustech.erp.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class AccessDeniedException extends RuntimeException {

    public AccessDeniedException() {
        super("Acesso negado: você não tem permissão para acessar este recurso.");
    }

    public AccessDeniedException(String message) {
        super(message);
    }
}
