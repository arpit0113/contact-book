package com.exercise.contactBook.service;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class SmtpMailService implements MailService{
    private static final Logger log = LoggerFactory.getLogger(SmtpMailService.class);
    private final JavaMailSender mailSender;

    @Value("${app.mail.from:no-reply@contactbook.local}")
    private String fromAddress;

    public SmtpMailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    @Override
    public void sendOtpEmail(String toEmail, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toEmail);
            message.setSubject("Your contactBook verification code");
            message.setText("Your OTP is: " + otp + "\n OTP will expire in 3 Minutes");
            mailSender.send(message);
        }catch (Exception ex){
            log.error("Failed to send otp email to {} : {}",toEmail, ex.getMessage());
        }
    }
}
