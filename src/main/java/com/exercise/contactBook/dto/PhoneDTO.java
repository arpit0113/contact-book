package com.exercise.contactBook.dto;

import com.exercise.contactBook.enums.EmailLable;
import com.exercise.contactBook.enums.PhoneLable;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PhoneDTO {

    @NotBlank(message = "Phone number is required")
    @Valid
    private String phone;
    private PhoneLable lable = PhoneLable.OTHER;

    @JsonProperty("primary")
    @JsonSetter(nulls = Nulls.SKIP)
    private boolean isPrimary = false;
}
