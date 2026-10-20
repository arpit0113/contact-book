package com.exercise.contactBook.dto;

import com.exercise.contactBook.enums.ApprovalMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class WorkFlowRequestDTO {
    @NotBlank(message = "Give the flow a name")
    @Size(max = 60, message = "Name is too long")
    private String name;


    @NotNull(message = "Choose how approval works")
    private ApprovalMode mode;


    @NotEmpty(message = "Add at least one approver")
    @Size(max = 10, message = "A flow can have at most 10 steps")
    private List<Long> roleIds;
}
