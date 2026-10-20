package com.exercise.contactBook.model;

import com.exercise.contactBook.enums.ContactActionType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "approval_policies",
        uniqueConstraints = @UniqueConstraint(columnNames = {"organization_id", "action_type", "requester_role_id"}))
@Getter
@Setter
@NoArgsConstructor
public class ApprovalPolicy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id")
    private Organization organization;


    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false)
    private ContactActionType actionType;


    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "requester_role_id")
    private OrgRole requesterRole;


    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "workflow_id")
    private ApprovalWorkflow workflow;
}
