package com.ncwu.authservice.exception;

/**
 * WeChat OAuth异常
 * @author jingxu
 * @version 1.0.0
 * @since 2026/2/12
 */
public class WeChatOAuthException extends RuntimeException {
    
    public WeChatOAuthException(String message) {
        super(message);
    }
    
    public WeChatOAuthException(String message, Throwable cause) {
        super(message, cause);
    }
}
