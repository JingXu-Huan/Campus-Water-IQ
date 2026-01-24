package com.ncwu.iotservice.exception;


/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/1/24
 */
public class DeserializationFailedException extends RuntimeException {
    public DeserializationFailedException(String message) {
        super(message);
    }
}
