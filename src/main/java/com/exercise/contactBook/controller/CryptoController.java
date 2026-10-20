package com.exercise.contactBook.controller;

import com.exercise.contactBook.crypto.CryptoKeyService;
import com.exercise.contactBook.dto.PublicKeyResponseDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/crypto")
public class CryptoController {
    private final CryptoKeyService cryptoKeyService;

    public CryptoController(CryptoKeyService cryptoKeyService) {
        this.cryptoKeyService = cryptoKeyService;
    }

    @GetMapping("/public-key")
    public PublicKeyResponseDTO getPublicKey(){
        return new PublicKeyResponseDTO(cryptoKeyService.getKeyId(),cryptoKeyService.getPublicKeyBase64());
    }
}
