package com.exercise.contactBook.dto;

import com.exercise.contactBook.enums.ContactType;
import com.exercise.contactBook.model.ContactEmail;
import com.exercise.contactBook.model.ContactPhone;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ContactRequestDTO {
    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must be at most 100 characters")
    private String name;

    @Valid
   private List<PhoneDTO> phones = new ArrayList<>();

    @Valid
    private List<EmailDTO> emails =  new ArrayList<>();

    @Size(max = 1000, message = "Notes must be at most 1000 characters")
    private String notes;

    @NotNull(message = "contact type is required")
    private ContactType type;


}
