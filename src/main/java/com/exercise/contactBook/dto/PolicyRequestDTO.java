package com.exercise.contactBook.dto;
import com.exercise.contactBook.enums.ContactActionType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PolicyRequestDTO {
    @NotNull private ContactActionType actionType;
    @NotNull private Long requesterRoleId;
    @NotNull private Long workflowId;
}
