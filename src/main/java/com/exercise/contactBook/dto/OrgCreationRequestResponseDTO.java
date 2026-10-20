package com.exercise.contactBook.dto;

import com.exercise.contactBook.enums.OrgRequestStatus;
import lombok.Data;

import java.time.Instant;

@Data
public class OrgCreationRequestResponseDTO {
private Long id;
private String orgName;
private String orgDomain;
private String adminName;
private String adminEmail;
private OrgRequestStatus status;
private Instant createdAt;
private String reviewNote;

}
