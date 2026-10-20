package com.exercise.contactBook.dto;

import com.exercise.contactBook.enums.ContactActionType;
import com.exercise.contactBook.enums.RequestStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
public class ContactChangeRequestDTO {
private Long id;
private ContactActionType actionType;
private Long targetContactId;
private String requestedName;
private List<PhoneDTO> requestedPhones;
private List<EmailDTO> requestedEmails;
private String requestedNotes;
private RequestStatus status;
private String requestedByName;
private String reviewNote;
}
