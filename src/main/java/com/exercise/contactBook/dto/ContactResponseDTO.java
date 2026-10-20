package com.exercise.contactBook.dto;

import com.exercise.contactBook.enums.ContactType;
import lombok.Data;

import java.util.List;

@Data
public class ContactResponseDTO {
    private Long id;
    private String name;
    private List<PhoneDTO> phones;
    private List<EmailDTO> emails;
    private String notes;
    private Long userId;
    private ContactType type;
}
