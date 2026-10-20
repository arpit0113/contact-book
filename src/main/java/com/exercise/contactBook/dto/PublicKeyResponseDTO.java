package com.exercise.contactBook.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PublicKeyResponseDTO {
    private String keyId;
    private String publicKey;
}
