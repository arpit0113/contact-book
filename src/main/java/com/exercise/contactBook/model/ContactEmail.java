package com.exercise.contactBook.model;

import com.exercise.contactBook.enums.EmailLable;
import com.exercise.contactBook.enums.PhoneLable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "contact_emails", indexes = {
        @Index(name = "idx_contact_emails_contact", columnList = "contact_id"),
        @Index(name = "idx_contact_emails_email", columnList = "email")
})
@Getter
@Setter
@NoArgsConstructor
public class ContactEmail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id", nullable = false)
    private Contact contact;

    @Column(nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmailLable lable =  EmailLable.OTHER;

    @Column(nullable = false)
    private boolean isPrimary = false;
}
