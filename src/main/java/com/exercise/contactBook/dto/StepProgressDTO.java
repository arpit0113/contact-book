package com.exercise.contactBook.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StepProgressDTO {
    private int order;
    private String roleName;
    private String state;
}
