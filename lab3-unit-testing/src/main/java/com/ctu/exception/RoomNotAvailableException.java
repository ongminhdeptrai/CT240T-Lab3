package com.ctu.exception;

public class RoomNotAvailableExceptionextends RuntimeException {
    public RoomNotAvailableException(String message) {
        super(message);
    }
}