package com.exercise.contactBook.dto;
import com.exercise.contactBook.enums.ContactActionType;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PolicyDTO {
    private Long id;
    private ContactActionType actionType;
    private Long requesterRoleId;
    private String requesterRoleName;
    private Long workflowId;
    private String workflowName;
}
