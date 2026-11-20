package com.example.personalblog.controller;

import com.example.personalblog.exception.RuleViolation;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;

@ControllerAdvice(assignableTypes = ImageApiController.class)
@org.springframework.core.annotation.Order(org.springframework.core.Ordered.HIGHEST_PRECEDENCE)
public class ApiExceptionHandler {
    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler({
        RuleViolation.class,
        MethodArgumentNotValidException.class,
        org.springframework.http.converter.HttpMessageNotReadableException.class,
        org.springframework.web.bind.MissingServletRequestParameterException.class,
        org.springframework.web.multipart.support.MissingServletRequestPartException.class
    })
    @ResponseBody
    public ResponseEntity<Map<String, String>> invalid(Exception error) {
        return ResponseEntity.badRequest()
                .body(
                        Map.of(
                                "message",
                                error instanceof RuleViolation
                                        ? error.getMessage()
                                        : "請確認上傳檔案或預覽內容，最多 50000 字"));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseBody
    public ResponseEntity<Map<String, String>> tooLarge() {
        return ResponseEntity.status(413).body(Map.of("message", "圖片最多 5 MiB"));
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public ResponseEntity<Map<String, String>> storage(Exception error) {
        String reference = java.util.UUID.randomUUID().toString();
        log.error("預覽或上傳錯誤 [{}]", reference, error);
        return ResponseEntity.internalServerError()
                .body(Map.of("message", "操作失敗，請稍後重試。錯誤編號：" + reference));
    }
}
