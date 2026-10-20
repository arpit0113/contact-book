package com.exercise.contactBook.model;

import com.exercise.contactBook.enums.ApprovalDecision;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "approval_actions")
@Getter
@Setter
@NoArgsConstructor
public class ApprovalAction {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id")
    private ContactChangeRequest request;


    private int stepOrder;
    private Long actorId;
    private String actorName;


    @Enumerated(EnumType.STRING)
    private ApprovalDecision decision;


    private String note;
    private Instant actedAt = Instant.now();
}