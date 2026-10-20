package com.exercise.contactBook.model;

import com.exercise.contactBook.enums.ContactType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "contacts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Contact {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContactType type;

    @OneToMany(mappedBy = "contact" , cascade = CascadeType.ALL , orphanRemoval = true , fetch = FetchType.LAZY)
    private List<ContactPhone> phones = new ArrayList<>();

    @OneToMany(mappedBy = "contact" , cascade = CascadeType.ALL , orphanRemoval = true , fetch = FetchType.LAZY)
    private List<ContactEmail> emails = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;
}
