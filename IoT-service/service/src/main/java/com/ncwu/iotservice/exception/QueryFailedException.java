package com.ncwu.iotservice.exception;


/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/1/21
 */
public class QueryFailedException extends RuntimeException {
    public QueryFailedException(String message) {
        super(message);
    }
}
