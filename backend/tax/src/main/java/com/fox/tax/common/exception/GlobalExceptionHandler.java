package com.fox.tax.common.exception;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全域異常處理器
 *
 * <p>統一攔截 Controller 與 Service 拋出的例外狀況，並轉換為標準 JSON 格式回傳給前端。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  /**
   * 處理 Spring Security 權限與認證例外，直接向上拋出交由 Spring Security Filter 處理，
   * 確保維持標準 401/403 回應行為。
   */
  @ExceptionHandler({AccessDeniedException.class, AuthenticationException.class})
  public void handleSecurityException(RuntimeException ex) {
    throw ex;
  }

  /** 處理表單與 DTO 參數驗證失敗例外 (@Valid) */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> handleValidationException(
      MethodArgumentNotValidException ex) {
    String errorMsg =
        ex.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining("; "));
    log.warn("參數驗證失敗: {}", errorMsg);
    return buildErrorResponse(HttpStatus.BAD_REQUEST, errorMsg);
  }

  /** 處理非法參數例外 */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(
      IllegalArgumentException ex) {
    log.warn("參數錯誤: {}", ex.getMessage());
    return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  /** 處理業務邏輯執行期例外 (如報單不存在、進口報單核銷餘額不足等) */
  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
    log.error("業務邏輯執行例外: {}", ex.getMessage(), ex);
    return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  /** 建立統一的錯誤回應結構 */
  private ResponseEntity<Map<String, Object>> buildErrorResponse(
      HttpStatus status, String message) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("timestamp", LocalDateTime.now().toString());
    body.put("status", status.value());
    body.put("error", status.getReasonPhrase());
    body.put("message", message);
    return ResponseEntity.status(status).body(body);
  }
}
