package com.exercise.contactBook.model;
import com.exercise.contactBook.enums.ApprovalMode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "approval_workflows")
@Getter
@Setter
@NoArgsConstructor
public class ApprovalWorkflow {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id")
    private Organization organization;


    @Column(nullable = false)
    private String name;


    @OneToMany(mappedBy = "workflow", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stepOrder ASC")
    private List<ApprovalStep> steps = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private ApprovalMode mode = ApprovalMode.SEQUENTIAL;
}