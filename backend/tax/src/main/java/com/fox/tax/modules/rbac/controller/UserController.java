package com.fox.tax.modules.rbac.controller;

import com.fox.tax.modules.rbac.entity.User;
import com.fox.tax.modules.rbac.service.UserService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasAuthority('USER_VIEW')")
    public List<User> findAll() {
        return userService.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_VIEW')")
    public ResponseEntity<User> findById(@PathVariable Long id) {
        return userService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('USER_EDIT')")
    public User create(@RequestBody User user) {
        return userService.save(user);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_EDIT')")
    public ResponseEntity<User> update(@PathVariable Long id, @RequestBody User user) {
        return userService.findById(id)
                .map(existing -> {
                    // 只更新前端傳來的欄位，保留密碼等敏感資料
                    existing.setUsername(user.getUsername());
                    existing.setEmail(user.getEmail());
                    existing.setEnabled(user.isEnabled());
                    existing.setRoles(user.getRoles());
                    // 密碼不在此處更新，由專用的 changePassword 端點處理
                    return ResponseEntity.ok(userService.saveWithoutPasswordEncode(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_EDIT')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.deleteById(id);
        return ResponseEntity.ok().build();
    }

    /**
     * 變更密碼端點
     */
    @PutMapping("/{id}/password")
    @PreAuthorize("hasAuthority('USER_EDIT')")
    public ResponseEntity<Void> changePassword(@PathVariable Long id, @RequestBody java.util.Map<String, String> body) {
        String newPassword = body.get("newPassword");
        if (newPassword == null || newPassword.length() < 6) {
            return ResponseEntity.badRequest().build();
        }
        userService.changePassword(id, newPassword);
        return ResponseEntity.ok().build();
    }
}
