package com.exercise.contactBook.model;

import com.exercise.contactBook.enums.Permission;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "org_roles",
        uniqueConstraints = @UniqueConstraint(columnNames = {"organization_id", "name"}))
@Getter
@Setter
@NoArgsConstructor
public class OrgRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    private boolean systemRole;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "org_role_permission" , joinColumns = @JoinColumn(name = "role_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "permission",  nullable = false)
    private Set<Permission> permissions = new HashSet<>();
}
