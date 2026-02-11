package com.ncwu.authservice.exception;

/**
 * GitHub OAuth异常
 * @author jingxu
 * @version 1.0.0
 * @since 2026/2/11
 */
public class GitHubOAuthException extends RuntimeException {
    
    public GitHubOAuthException(String message) {
        super(message);
    }
    
    public GitHubOAuthException(String message, Throwable cause) {
        super(message, cause);
    }
}
