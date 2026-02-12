package com.nqt.common_starter.exception;


import com.nqt.common_starter.constant.ErrorCode;
import com.nqt.common_starter.dto.response.APIResponse;
import jakarta.validation.ConstraintViolation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;
import java.util.Objects;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    private static final String MIN_ATTRIBUTE = "min";

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    ResponseEntity<APIResponse<Object>> handleValidation(MethodArgumentNotValidException exception) {
        String enumKey = exception.getFieldError().getDefaultMessage();

        ErrorCode errorCode = ErrorCode.INVALID_KEY;
        Map<String, Object> attributes = null;
        try {
            errorCode = ErrorCode.valueOf(enumKey);

            var constraintViolation =
                    exception.getBindingResult().getAllErrors().getFirst().unwrap(ConstraintViolation.class);

            attributes = constraintViolation.getConstraintDescriptor().getAttributes();

            log.info(attributes.toString());

        } catch (IllegalArgumentException e) {

        }

        APIResponse apiResponse = new APIResponse();

        apiResponse.setCode(errorCode.getCode());
        apiResponse.setMessage(
                Objects.nonNull(attributes)
                        ? mapAttribute(errorCode.getMessage(), attributes)
                        : errorCode.getMessage());

        return ResponseEntity.badRequest().body(apiResponse);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<APIResponse<Object>> handleAccessDinedException(
            AccessDeniedException exception) {
        ErrorCode errorCode = ErrorCode.UNAUTHORIZED;
        APIResponse<Object> apiResponse = new APIResponse<>();
        apiResponse.setCode(errorCode.getCode());
        apiResponse.setMessage(errorCode.getMessage());

        return ResponseEntity.status(errorCode.getStatus()).body(apiResponse);
    }

    @ExceptionHandler(GlobalException.class)
    public ResponseEntity<APIResponse<Object>> globalException(GlobalException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        APIResponse<Object> apiResponse = new APIResponse<>();
        apiResponse.setCode(errorCode.getCode());
        apiResponse.setMessage(exception.getMessage());

        return ResponseEntity.status(errorCode.getStatus()).body(apiResponse);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<APIResponse<Object>> httpMessageNotReadableException(
            HttpMessageNotReadableException exception) {
        APIResponse<Object> apiResponse = new APIResponse<>();
        String message = exception.getMessage();

        if (message != null && message.contains("values accepted for Enum class")) {
            int start = message.indexOf("values accepted for Enum class");
            message = message.substring(start);
        }

        // Làm sạch thêm (xóa dấu chấm thừa hoặc dòng mới)
        message = message.replaceAll("\\s+", " ").trim();
        apiResponse.setCode(ErrorCode.NOT_FOUND_ENUM.getCode());
        log.info(exception.getMessage());
        apiResponse.setMessage(message);
        return ResponseEntity.status(ErrorCode.NOT_FOUND_ENUM.getStatus()).body(apiResponse);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<APIResponse<Object>> handeIllegalArgumentException(
            IllegalArgumentException exception) {
        APIResponse<Object> apiResponse = new APIResponse<>();
        apiResponse.setCode(ErrorCode.FIELDS_EMPTY.getCode());
        apiResponse.setMessage(exception.getMessage());
        return ResponseEntity.status(ErrorCode.FIELDS_EMPTY.getStatus()).body(apiResponse);
    }
    private String mapAttribute(String message, Map<String, Object> attributes) {
        String minValue = String.valueOf(attributes.get(MIN_ATTRIBUTE));

        return message.replace("{" + MIN_ATTRIBUTE + "}", minValue);
    }
}
