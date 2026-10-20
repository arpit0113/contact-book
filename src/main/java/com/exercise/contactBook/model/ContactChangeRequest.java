package com.exercise.contactBook.model;

import com.exercise.contactBook.enums.ContactActionType;
import com.exercise.contactBook.enums.RequestStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "contact_change_request")
@Getter
@Setter
@NoArgsConstructor
public class ContactChangeRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContactActionType actionType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_contact_id")
    private Contact targetContact;

    private String requestedName;
    private String requestedNotes;


    @OneToMany(mappedBy = "changeRequest", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ContactChangeRequestPhone> requestedPhones = new ArrayList<>();

    @OneToMany(mappedBy = "changeRequest", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ContactChangeRequestEmail> requestedEmails = new ArrayList<>();


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by", nullable = false)
    private User requestedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status = RequestStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    private String reviewNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id")
    private ApprovalWorkflow workflow;

    private Integer currentStep = 1;

    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ApprovalAction> actions = new ArrayList<>();
}

