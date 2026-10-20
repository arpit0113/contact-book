package com.exercise.contactBook.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "organizations")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_user_id")
    private User admin;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "organization_domains", joinColumns = @JoinColumn(name = "organization_id"))
    @Column(name = "domain", nullable = false)
    private List<String> allowedDomains = new ArrayList<>();

    @Column(nullable = false)
    private String defaultRegion = "IN";

    private Boolean workflowSeeded = false ;

}
