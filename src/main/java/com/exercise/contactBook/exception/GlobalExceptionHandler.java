package com.exercise.contactBook.exception;

import com.exercise.contactBook.dto.ErrorResponseDTO;
import com.exercise.contactBook.dto.FieldErrorDTO;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.AccessDeniedException;
import java.util.*;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
   public ResponseEntity<ErrorResponseDTO> handleNotFound(ResourceNotFoundException ex , HttpServletRequest request){
       return build(HttpStatus.NOT_FOUND , ex.getMessage() , null , request);
   }
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<Map<String, String>> handleDisabledAccount(DisabledException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponseDTO> handleResponseStatus(ResponseStatusException ex, HttpServletRequest request){
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        return build(status, ex.getReason(), null , request);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponseDTO> handleMaxUpload(MaxUploadSizeExceededException ex , HttpServletRequest request){

        return build(HttpStatus.BAD_REQUEST, "file too large(Max 5MB)", null, request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public  ResponseEntity<ErrorResponseDTO> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request){
        return build(HttpStatus.FORBIDDEN,"You dont have permission to perform this action",null, request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidation(MethodArgumentNotValidException ex , HttpServletRequest request){
        List<FieldErrorDTO> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> new FieldErrorDTO(fieldError.getField() , fieldError.getDefaultMessage()))
                .toList();
        return build(HttpStatus.BAD_REQUEST , "Validation failed" , fieldErrors , request);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponseDTO> handleMethodValidation(HandlerMethodValidationException ex , HttpServletRequest request){
        return build(HttpStatus.BAD_REQUEST , "Validation failed" + ex.getMessage() , null , request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDTO> handleUnreadableJSON(HttpMessageNotReadableException ex , HttpServletRequest request){
        log.warn("Unreadable JSON {} {} : {}", request.getMethod() , request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.BAD_REQUEST , "Wrong JSON request Body" , null , request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponseDTO> handleDataIntegrity(DataIntegrityViolationException ex , HttpServletRequest request){
        String traceId = UUID.randomUUID().toString();
        log.error("Data integrity violation [traceId={}]", traceId , ex);
        ResponseEntity<ErrorResponseDTO> response = build(HttpStatus.CONFLICT , "this operation conflicts with existing data", null , request);
        response.getBody().setTraceId(traceId);
        return response;
    }

   @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleAllOther(Exception ex, HttpServletRequest request){
        String traceId = UUID.randomUUID().toString();
        log.error("Unhandled exception [traceId={}]", traceId , ex);
        ResponseEntity<ErrorResponseDTO> response = build(HttpStatus.INTERNAL_SERVER_ERROR , "Something went wrong", null , request);
        response.getBody().setTraceId(traceId);
        return response;
    }

    //helper methods

    private ResponseEntity<ErrorResponseDTO> build(HttpStatus status , String msg , List<FieldErrorDTO> fieldErrors, HttpServletRequest request){
       ErrorResponseDTO body = new ErrorResponseDTO();
       body.setStatus(status.value());
       body.setError(status.getReasonPhrase());
       body.setMessage(msg);
       body.setPath(request.getRequestURI());
       body.setFieldErrors(fieldErrors);
        return ResponseEntity.status(status).body(body);
    }

}
