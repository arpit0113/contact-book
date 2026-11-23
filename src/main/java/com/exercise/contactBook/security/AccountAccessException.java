package com.exercise.contactBook.security;

import org.springframework.security.core.AuthenticationException;


public class AccountAccessException extends AuthenticationException {
    private final String code;
    private final int status;

    public AccountAccessException(String code, int status) {
        super(code);
        this.code = code;
        this.status = status;
    }

    public String getCode() { return code; }
    public int getStatus() { return status; }
}