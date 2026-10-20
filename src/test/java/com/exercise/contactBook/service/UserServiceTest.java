package com.exercise.contactBook.service;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import com.exercise.contactBook.dto.UserResponseDTO;
import com.exercise.contactBook.dto.UserUpdateRequestDTO;
import com.exercise.contactBook.model.User;
import com.exercise.contactBook.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    @Spy // Spy allows us to mock the internal getCurrentUser() call if needed
    private UserService userService;

    private User existingUser;
    private UserUpdateRequestDTO updateDto;

    @BeforeEach
    void setUp() {
        existingUser = new User();
        existingUser.setId(1L);
        existingUser.setName("Old Name");
        existingUser.setEmail("old@example.com");

        updateDto = new UserUpdateRequestDTO();
        updateDto.setName("New Name");
        updateDto.setEmail("new@example.com");
    }

    @Test
    void updateCurrentUser_Success() {

        doReturn(existingUser).when(userService).getCurrentUser();
        when(userRepository.findByEmail(updateDto.getEmail())).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));


        UserResponseDTO result = userService.updateCurrentUser(updateDto);

        assertNotNull(result);
        assertEquals("New Name", existingUser.getName());
        assertEquals("new@example.com", existingUser.getEmail());
        verify(userRepository, times(1)).save(existingUser);
    }

    @Test
    void updateCurrentUser_Success_SameEmail() {
        updateDto.setEmail("old@example.com");
        doReturn(existingUser).when(userService).getCurrentUser();


        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));


        UserResponseDTO result = userService.updateCurrentUser(updateDto);


        assertNotNull(result);
        assertEquals("New Name", existingUser.getName());
        assertEquals("old@example.com", existingUser.getEmail());
    }

    @Test
    void updateCurrentUser_ThrowsException_WhenEmailAlreadyExists() {

        doReturn(existingUser).when(userService).getCurrentUser();

        User anotherUser = new User();
        anotherUser.setId(2L);
        anotherUser.setEmail("new@example.com");

        when(userRepository.findByEmail(updateDto.getEmail())).thenReturn(Optional.of(anotherUser));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.updateCurrentUser(updateDto);
        });

        assertEquals("email already registered", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }
}

