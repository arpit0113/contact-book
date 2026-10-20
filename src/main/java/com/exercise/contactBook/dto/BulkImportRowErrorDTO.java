package com.exercise.contactBook.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BulkImportRowErrorDTO {
    private int row;
    private String message;
}
