package com.exercise.contactBook.service;

import com.exercise.contactBook.dto.ContactRequestDTO;
import com.exercise.contactBook.dto.ContactResponseDTO;
import com.exercise.contactBook.model.Contact;
import com.exercise.contactBook.model.User;
import com.exercise.contactBook.repository.ContactRepository;
import com.exercise.contactBook.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContactServiceTest {

    @Mock
    private ContactRepository contactRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ContactService contactService;

    private User user;

    @BeforeEach
    void setUp() {

        user = new User();
        user.setId(1L);
        user.setName("Arpit");
        user.setEmail("arpit@gmail.com");

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "arpit@gmail.com",
                        null
                )
        );
    }

    @Test
    void addContact_shouldSaveAndReturnContact() {
        ContactRequestDTO dto = new ContactRequestDTO();
        dto.setName("Rahul");
        dto.setEmail("rahul@gmail.com");
        dto.setPhone("9876543210");
        dto.setNotes("Friend");

        when(userRepository.findByEmail("arpit@gmail.com"))
                .thenReturn(Optional.of(user));

        Contact savedContact = new Contact();
        savedContact.setId(10L);
        savedContact.setName("Rahul");
        savedContact.setEmail("rahul@gmail.com");
        savedContact.setPhone("9876543210");
        savedContact.setNotes("Friend");
        savedContact.setUser(user);

        when(contactRepository.save(any(Contact.class)))
                .thenReturn(savedContact);

        ContactResponseDTO result = contactService.addContact(dto);

        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals("Rahul", result.getName());
        assertEquals("rahul@gmail.com", result.getEmail());
        assertEquals("9876543210", result.getPhone());
        assertEquals("Friend", result.getNotes());
        assertEquals(1L, result.getUserId());

        verify(userRepository).findByEmail("arpit@gmail.com");
        verify(contactRepository).save(any(Contact.class));
    }
}