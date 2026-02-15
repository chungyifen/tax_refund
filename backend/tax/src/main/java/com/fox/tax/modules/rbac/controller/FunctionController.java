package com.fox.tax.modules.rbac.controller;

import com.fox.tax.modules.rbac.entity.Function;
import com.fox.tax.modules.rbac.service.FunctionService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/functions")
@RequiredArgsConstructor
public class FunctionController {

    private final FunctionService functionService;

    @GetMapping
    @PreAuthorize("hasAuthority('FUNCTION_VIEW')")
    public List<Function> findAll() {
        return functionService.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('FUNCTION_VIEW')")
    public ResponseEntity<Function> findById(@PathVariable Long id) {
        return functionService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('FUNCTION_EDIT')")
    public Function create(@RequestBody Function function) {
        return functionService.save(function);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('FUNCTION_EDIT')")
    public ResponseEntity<Function> update(@PathVariable Long id, @RequestBody Function function) {
        return functionService.findById(id)
                .map(existing -> {
                    function.setId(id);
                    return ResponseEntity.ok(functionService.save(function));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('FUNCTION_EDIT')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        functionService.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
