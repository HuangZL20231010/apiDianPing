package com.api.config;

import com.api.dto.Result;
import com.api.exception.BadRequestException;
import com.api.exception.NoContentException;
import com.api.exception.UnauthorizedException;
import com.api.exception.UnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class WebExceptionAdvice {

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Result> handleBadRequestException(BadRequestException ex) {
        return new ResponseEntity<>(Result.fail(ex.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(NoContentException.class)
    public ResponseEntity<Result> handleNoContentException(NoContentException ex) {
        return new ResponseEntity<>(Result.fail(ex.getMessage()), HttpStatus.NO_CONTENT);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Result> handleUnauthorizedException(UnauthorizedException ex) {
        return new ResponseEntity<>(Result.fail(ex.getMessage()), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Result> handleRuntimeException(RuntimeException e) {
        return new ResponseEntity<>(Result.fail("服务器异常，请重试"), HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(UnavailableException.class)
    public ResponseEntity<Result> handleUnavailableException(UnauthorizedException ex) {
        return new ResponseEntity<>(Result.fail(ex.getMessage()), HttpStatus.SERVICE_UNAVAILABLE);
    }


}
