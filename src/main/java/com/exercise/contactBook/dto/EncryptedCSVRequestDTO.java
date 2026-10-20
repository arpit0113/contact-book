package com.exercise.contactBook.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EncryptedCSVRequestDTO {
    @NotBlank(message = "KeyId is required")
    private String keyId;

    @NotBlank(message = "Key is required")
    private String encryptedKey; //AES (encrypted with RSA) - base64

    @NotBlank(message = "Initialization vector is required")
    private String iv; // initializaion vector 12 random bytes

    @NotBlank(message = "cipherText is required")
    private String cipherText; //ASE-GCM encrypted csv bytes in base64
}
