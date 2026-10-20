package com.exercise.contactBook.dto;

import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class ErrorResponseDTO {
    private Instant timestamp = Instant.now();
    private int  status;
    private String error;
    private String message ;
    private String path;
    private List<FieldErrorDTO> fieldErrors;
    private String traceId;

}
