package com.exercise.contactBook.dto;
import com.exercise.contactBook.enums.EmailLable;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmailDTO {

    @NotBlank(message = "Email is required")
    @Email(message = "Email is invalid")
    private String email;
    private EmailLable lable = EmailLable.OTHER;

    @JsonProperty("primary")
    @JsonSetter(nulls = Nulls.SKIP)
    private boolean isPrimary = false;
}
