package com.exercise.contactBook.model;

import com.exercise.contactBook.enums.AuthProvider;
import com.exercise.contactBook.enums.Role;
import com.exercise.contactBook.enums.UserStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Generated;

import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id ;
    private String name;

    @Column(unique = true , nullable = false)
    private String email;

    private String password;

    @Enumerated(EnumType.STRING)
    private AuthProvider provider;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id") // org can be null for super admin
    private Organization organization;

    @Enumerated(EnumType.STRING)
    private Role role;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "org_role_id")
    private OrgRole orgRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    @OneToMany(mappedBy = "user")
    private List<Contact> contacts;
}
