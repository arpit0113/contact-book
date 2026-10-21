package com.exercise.contactBook.crypto;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.SecretKeySpec;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.MGF1ParameterSpec;
import java.util.Base64;
import java.util.UUID;

@Component
public class CryptoKeyService {
    private KeyPair keyPair;
    private String keyId;

    @PostConstruct
    public void init() throws Exception{
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(3072);
        this.keyPair = generator.generateKeyPair();
        this.keyId = UUID.randomUUID().toString();
    }

    public String getKeyId() {
        return keyId;
    }

    public  String getPublicKeyBase64(){
        return Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
    }

    public byte[] decryptAESKey(String encryptedKeyBase64, String requestKeyId){
        if(!keyId.equals(requestKeyId)){
            throw new IllegalArgumentException("Unknown or expired encryption key id");
        }
        try {
            byte[] aesKeyBytes = Base64.getDecoder().decode(encryptedKeyBase64);
            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPPadding");
            MGF1ParameterSpec mgf1Spec = new MGF1ParameterSpec("SHA-256");
            OAEPParameterSpec oaepSpec =  new OAEPParameterSpec("SHA-256" , "MGF1" , mgf1Spec , PSource.PSpecified.DEFAULT);
            cipher.init(Cipher.DECRYPT_MODE , keyPair.getPrivate() , oaepSpec);
            return cipher.doFinal(aesKeyBytes);
        } catch (Exception e){
            throw new IllegalArgumentException("could not decrypt the key : ");
        }
    }

    public byte[] decrypt(byte[] aesKeyBytes , String ivBase64, String ciphertextBase64){
        try {
            SecretKey secretKey = new SecretKeySpec(aesKeyBytes , "AES");
            byte[] iv = Base64.getDecoder().decode(ivBase64);
            byte[] cipherText = Base64.getDecoder().decode(ciphertextBase64);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(128 , iv);
            cipher.init(Cipher.DECRYPT_MODE , secretKey , spec);
            return cipher.doFinal(cipherText);
        } catch (Exception e) {
            throw new IllegalArgumentException("could not decrypt tht file - data maybe corrupted or tempered");
        }
    }
}
