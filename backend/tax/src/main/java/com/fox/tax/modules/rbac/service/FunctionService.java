package com.fox.tax.modules.rbac.service;

import com.fox.tax.modules.rbac.entity.Function;
import com.fox.tax.modules.rbac.repository.FunctionRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FunctionService {

    private final FunctionRepository functionRepository;

    public List<Function> findAll() {
        return functionRepository.findAll();
    }

    public Optional<Function> findById(Long id) {
        return functionRepository.findById(id);
    }

    @Transactional
    public Function save(Function function) {
        return functionRepository.save(function);
    }

    @Transactional
    public void deleteById(Long id) {
        functionRepository.deleteById(id);
    }
}
