package com.exercise.contactBook.dto;

import com.exercise.contactBook.enums.ApprovalMode;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class WorkFlowDTO {
 private Long id;
 private String name;
 private ApprovalMode mode;
 private List<WorkflowStepDTO> steps;
 private long pendingCount;
}
