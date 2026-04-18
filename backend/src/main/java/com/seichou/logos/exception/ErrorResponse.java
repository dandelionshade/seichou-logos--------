package com.seichou.logos.exception;

import java.time.LocalDateTime;

/**
 * ErrorResponse (统一错误响应体)
 * 用于在发生异常时，向前端返回标准格式的 JSON 数据。
 */
public class ErrorResponse {
    private int status;       // HTTP 状态码
    private String message;   // 错误信息
    private String path;      // 请求路径
    private LocalDateTime timestamp; // 发生时间

    public ErrorResponse() {
    }

    public ErrorResponse(int status, String message, String path, LocalDateTime timestamp) {
        this.status = status;
        this.message = message;
        this.path = path;
        this.timestamp = timestamp;
    }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
