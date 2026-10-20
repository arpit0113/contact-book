package com.exercise.contactBook.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkImportResultDTO {
    private int totalRows;
    private int created;
    private int pendingApproval;
    private int skipped;
    private List<BulkImportRowErrorDTO> errors;
}
