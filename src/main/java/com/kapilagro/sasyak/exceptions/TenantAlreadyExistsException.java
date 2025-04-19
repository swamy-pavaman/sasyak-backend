package com.kapilagro.sasyak.exceptions;

public class TenantAlreadyExistsException extends RuntimeException  {
    public TenantAlreadyExistsException(String message) {
        super(message);

    }
}
