package com.ncwu.iotdevice.exception;


/**
 * @author jingxu
 * @version 1.0.0
 * @since 2025/12/23
 */
public class MessageSendException extends RuntimeException {
    public MessageSendException(String message) {
        super(message);
    }
    public MessageSendException(String message, Throwable cause) {
        super(message, cause);
    }
}
