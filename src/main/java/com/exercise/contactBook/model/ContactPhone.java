package com.exercise.contactBook.model;

import com.exercise.contactBook.enums.PhoneLable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "contact_phones", indexes = {
@Index(name = "idx_contact_phones_contact", columnList = "contact_id"),
@Index(name = "idx_contact_phones_phone", columnList = "phone")
})
@Getter
@Setter
@NoArgsConstructor
public class ContactPhone {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id", nullable = false)
    private Contact contact;

    @Column(nullable = false)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PhoneLable lable = PhoneLable.MOBILE;

    @Column(nullable = false)
    private boolean isPrimary = false;
}
