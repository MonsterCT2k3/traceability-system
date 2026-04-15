package vn.edu.kma.identity_service.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import vn.edu.kma.common.dto.response.ApiResponse;
import vn.edu.kma.common.exception.AppException;
import vn.edu.kma.common.exception.ErrorCode;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<Void>> handleAppException(AppException ex) {
        ErrorCode ec = ex.getErrorCode();
        return ResponseEntity.status(ec.getStatusCode()).body(
                ApiResponse.<Void>builder()
                        .code(ec.getCode())
                        .message(ec.getMessage())
                        .build()
        );
    }
}
