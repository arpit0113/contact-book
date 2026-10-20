package com.exercise.contactBook.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
public class OtpService {
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public OtpService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }
    public String generateOtp(){
        int code = secureRandom.nextInt(1000000);
        return String.format("%06d",code);
    }
    public String hashOtp(String otp){
        return passwordEncoder.encode(otp);
    }

    public boolean matches(String rawOtp, String hashedOtp){
        return passwordEncoder.matches(rawOtp,hashedOtp);
    }
}
