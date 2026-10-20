package com.exercise.contactBook.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;



@Entity
@Table(name = "approval_steps")
@Getter @Setter @NoArgsConstructor
public class ApprovalStep {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workflow_id")
    private ApprovalWorkflow workflow;


    private int stepOrder;


    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "approver_role_id")
    private OrgRole approverRole;
}
