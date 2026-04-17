package com.seichou.logos.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ErrorResponse (统一错误响应体)
 * 用于在发生异常时，向前端返回标准格式的 JSON 数据。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ErrorResponse {
    private int status;       // HTTP 状态码
    private String message;   // 错误信息
    private String path;      // 请求路径
    private LocalDateTime timestamp; // 发生时间
}
