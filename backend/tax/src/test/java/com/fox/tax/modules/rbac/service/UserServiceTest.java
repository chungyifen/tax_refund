package com.fox.tax.modules.rbac.service;

import com.fox.tax.modules.rbac.entity.User;
import com.fox.tax.modules.rbac.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void findAll() {
        User u1 = User.builder().username("alice").build();
        User u2 = User.builder().username("bob").build();
        when(userRepository.findAll()).thenReturn(List.of(u1, u2));

        List<User> result = userService.findAll();

        assertEquals(2, result.size());
        verify(userRepository).findAll();
    }

    @Test
    void findById() {
        User user = User.builder().username("alice").build();
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        Optional<User> result = userService.findById(1L);

        assertTrue(result.isPresent());
        assertEquals("alice", result.get().getUsername());
        verify(userRepository).findById(1L);
    }

    @Test
    void findById_NotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<User> result = userService.findById(99L);

        assertTrue(result.isEmpty());
        verify(userRepository).findById(99L);
    }

    @Test
    void save_encodesPassword() {
        User user = User.builder().username("alice").password("raw123").build();
        when(passwordEncoder.encode("raw123")).thenReturn("encoded123");
        when(userRepository.save(user)).thenReturn(user);

        User result = userService.save(user);

        assertEquals("encoded123", result.getPassword());
        verify(passwordEncoder).encode("raw123");
        verify(userRepository).save(user);
    }

    @Test
    void save_nullPassword_doesNotEncode() {
        User user = User.builder().username("alice").password(null).build();
        when(userRepository.save(user)).thenReturn(user);

        userService.save(user);

        verify(passwordEncoder, never()).encode(any());
        verify(userRepository).save(user);
    }

    @Test
    void deleteById() {
        doNothing().when(userRepository).deleteById(1L);

        userService.deleteById(1L);

        verify(userRepository).deleteById(1L);
    }

    @Test
    void changePassword() {
        User user = User.builder().username("alice").password("old").build();
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newPass")).thenReturn("encodedNew");
        when(userRepository.save(user)).thenReturn(user);

        userService.changePassword(1L, "newPass");

        assertEquals("encodedNew", user.getPassword());
        verify(passwordEncoder).encode("newPass");
        verify(userRepository).save(user);
    }

    @Test
    void changePassword_userNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.changePassword(99L, "newPass"));

        assertEquals("使用者不存在", ex.getMessage());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void findByUsername() {
        User user = User.builder().username("alice").build();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        Optional<User> result = userService.findByUsername("alice");

        assertTrue(result.isPresent());
        assertEquals("alice", result.get().getUsername());
        verify(userRepository).findByUsername("alice");
    }
}
