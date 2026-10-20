package com.exercise.contactBook.model;


import com.exercise.contactBook.enums.OrgRequestStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "organization_creation_request")
@Getter
@Setter
@NoArgsConstructor
public class OrganizationCreationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String orgName;

    @Column(nullable = false)
    private String orgDomain;

    @Column(nullable = false)
    private String adminName;

    @Column(nullable = false, unique = true)
    private String adminEmail;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String otp;

    @Column(nullable = false)
    private Instant otpExpireAt;

    @Column(nullable = false)
    private boolean otpVerified = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrgRequestStatus status = OrgRequestStatus.PENDING_OTP;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    private String reviewNote;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    private Instant lastOtpSentAt;

}
