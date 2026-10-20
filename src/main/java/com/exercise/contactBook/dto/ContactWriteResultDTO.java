package com.exercise.contactBook.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContactWriteResultDTO {
    private boolean pendingApproval;
    private ContactResponseDTO contact;
    private ContactChangeRequestDTO request;
}
