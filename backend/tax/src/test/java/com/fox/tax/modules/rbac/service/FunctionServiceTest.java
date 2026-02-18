package com.fox.tax.modules.rbac.service;

import com.fox.tax.modules.rbac.entity.Function;
import com.fox.tax.modules.rbac.repository.FunctionRepository;
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
class FunctionServiceTest {

    @Mock
    private FunctionRepository functionRepository;

    @InjectMocks
    private FunctionService functionService;

    @Test
    void findAll() {
        Function f1 = Function.builder().code("USER_READ").name("查詢使用者").build();
        Function f2 = Function.builder().code("USER_WRITE").name("編輯使用者").build();
        when(functionRepository.findAll()).thenReturn(List.of(f1, f2));

        List<Function> result = functionService.findAll();

        assertEquals(2, result.size());
        verify(functionRepository).findAll();
    }

    @Test
    void findById() {
        Function function = Function.builder().code("USER_READ").name("查詢使用者").build();
        function.setId(1L);
        when(functionRepository.findById(1L)).thenReturn(Optional.of(function));

        Optional<Function> result = functionService.findById(1L);

        assertTrue(result.isPresent());
        assertEquals("USER_READ", result.get().getCode());
        verify(functionRepository).findById(1L);
    }

    @Test
    void findById_NotFound() {
        when(functionRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Function> result = functionService.findById(99L);

        assertTrue(result.isEmpty());
        verify(functionRepository).findById(99L);
    }

    @Test
    void save() {
        Function function = Function.builder().code("USER_READ").name("查詢使用者").build();
        when(functionRepository.save(function)).thenReturn(function);

        Function result = functionService.save(function);

        assertEquals("USER_READ", result.getCode());
        verify(functionRepository).save(function);
    }

    @Test
    void deleteById() {
        doNothing().when(functionRepository).deleteById(1L);

        functionService.deleteById(1L);

        verify(functionRepository).deleteById(1L);
    }
}
