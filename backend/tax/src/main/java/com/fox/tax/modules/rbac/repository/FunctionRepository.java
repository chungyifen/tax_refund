package com.fox.tax.modules.rbac.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.fox.tax.modules.rbac.entity.Function;

import java.util.Optional;

@Repository
public interface FunctionRepository extends JpaRepository<Function, Long> {
    Optional<Function> findByCode(String code);
}
