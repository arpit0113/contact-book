package com.exercise.contactBook.controller;

import com.exercise.contactBook.dto.OrgCreationRequestDTO;
import com.exercise.contactBook.dto.OtpVerifyRequestDTO;
import com.exercise.contactBook.dto.ResendRequestDTO;
import com.exercise.contactBook.service.OrganizationRequestService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth/organizations")
public class OrganizationRequestController {

    private final OrganizationRequestService organizationRequestService;

    public OrganizationRequestController(OrganizationRequestService organizationRequestService) {
        this.organizationRequestService = organizationRequestService;
    }

    @PostMapping("/request")
    public ResponseEntity<Map<String, Object>> requestOrg(@Valid @RequestBody OrgCreationRequestDTO dto) {
        Long requestId = organizationRequestService.submitRequest(dto);
        return ResponseEntity.ok(Map.of(
                "requestId", requestId,
                "message", " OTP has been sent to your email. It expires 3 minutes"
        ));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, String>> verifyOtp(@Valid @RequestBody OtpVerifyRequestDTO dto) {
        organizationRequestService.verifyOtp(dto);
        return ResponseEntity.ok(Map.of("message", "Email verified. Your request is now awaiting admin approval."));
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<Map<String, String>> resendOtp(@RequestBody ResendRequestDTO dto) {
        organizationRequestService.resendOtp(dto);
        return ResponseEntity.ok(Map.of("message", "A new otp has been sent."));
    }
}