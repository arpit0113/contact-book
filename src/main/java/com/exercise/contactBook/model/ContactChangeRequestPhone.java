package com.exercise.contactBook.model;


import com.exercise.contactBook.enums.PhoneLable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "contact_change_request_phones")
@Getter
@Setter
@NoArgsConstructor
public class ContactChangeRequestPhone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "change_request_id", nullable = false)
    private ContactChangeRequest changeRequest;

    @Column(nullable = false)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PhoneLable lable = PhoneLable.MOBILE;

    @Column(nullable = false)
    private boolean isPrimary = false;
}
