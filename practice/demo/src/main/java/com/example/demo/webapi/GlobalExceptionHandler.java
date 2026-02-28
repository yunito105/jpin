package com.example.demo.webapi;

import com.example.demo.common.logging.AppLogger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * グローバル例外ハンドラ。
 *
 * <p>アプリケーション全体で発生する例外を横断的にハンドリングし、
 * 統一的なエラーレスポンスを返却する。</p>
 *
 * @author store-order-system
 * @since 1.0.0
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final AppLogger log = AppLogger.of(GlobalExceptionHandler.class);

    /**
     * バリデーションエラーをハンドリングする。
     *
     * @param ex バリデーション例外
     * @return エラーレスポンス（400 Bad Request）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(
            MethodArgumentNotValidException ex) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "バリデーションエラー");

        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        body.put("message", errors);

        log.warn("バリデーションエラー: {}", errors);
        return ResponseEntity.badRequest().body(body);
    }

    /**
     * 業務エラー（不正な引数）をハンドリングする。
     *
     * @param ex 不正引数例外
     * @return エラーレスポンス（400 Bad Request）
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(
            IllegalArgumentException ex) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "リクエストエラー");
        body.put("message", ex.getMessage());

        log.warn("リクエストエラー: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(body);
    }

    /**
     * 業務エラー（不正な状態遷移）をハンドリングする。
     *
     * @param ex 不正状態例外
     * @return エラーレスポンス（409 Conflict）
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalStateException(
            IllegalStateException ex) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", HttpStatus.CONFLICT.value());
        body.put("error", "状態遷移エラー");
        body.put("message", ex.getMessage());

        log.warn("状態遷移エラー: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }
}
