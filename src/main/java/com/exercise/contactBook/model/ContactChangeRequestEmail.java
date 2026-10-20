package com.exercise.contactBook.model;

import com.exercise.contactBook.enums.EmailLable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "contact_change_request_emails")
@Getter
@Setter
@NoArgsConstructor
public class ContactChangeRequestEmail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "change_request_id", nullable = false)
    private ContactChangeRequest changeRequest;

    @Column(nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmailLable lable = EmailLable.OTHER;

    @Column(nullable = false)
    private boolean isPrimary = false;
}
