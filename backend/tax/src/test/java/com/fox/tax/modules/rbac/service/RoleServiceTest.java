package com.fox.tax.modules.rbac.service;

import com.fox.tax.modules.rbac.entity.Role;
import com.fox.tax.modules.rbac.repository.RoleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private RoleService roleService;

    @Test
    void findAll() {
        Role r1 = Role.builder().name("ADMIN").description("管理員").build();
        Role r2 = Role.builder().name("USER").description("一般使用者").build();
        when(roleRepository.findAll()).thenReturn(List.of(r1, r2));

        List<Role> result = roleService.findAll();

        assertEquals(2, result.size());
        verify(roleRepository).findAll();
    }

    @Test
    void findById() {
        Role role = Role.builder().name("ADMIN").description("管理員").build();
        role.setId(1L);
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));

        Optional<Role> result = roleService.findById(1L);

        assertTrue(result.isPresent());
        assertEquals("ADMIN", result.get().getName());
        verify(roleRepository).findById(1L);
    }

    @Test
    void findById_NotFound() {
        when(roleRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Role> result = roleService.findById(99L);

        assertTrue(result.isEmpty());
        verify(roleRepository).findById(99L);
    }

    @Test
    void save() {
        Role role = Role.builder().name("ADMIN").description("管理員").build();
        when(roleRepository.save(role)).thenReturn(role);

        Role result = roleService.save(role);

        assertEquals("ADMIN", result.getName());
        verify(roleRepository).save(role);
    }

    @Test
    void deleteById() {
        doNothing().when(roleRepository).deleteById(1L);

        roleService.deleteById(1L);

        verify(roleRepository).deleteById(1L);
    }
}
