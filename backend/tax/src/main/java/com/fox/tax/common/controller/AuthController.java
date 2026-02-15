package com.fox.tax.common.controller;

import com.fox.tax.common.security.JwtTokenProvider;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    // 登入 API
    @PostMapping("/login")
    public ResponseEntity<JwtAuthResponse> login(@RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String token = jwtTokenProvider.generateToken(authentication);

        return ResponseEntity.ok(new JwtAuthResponse(token));
    }

    // 獲取使用者資訊 API
    @GetMapping("/info")
    public ResponseEntity<UserInfoResponse> getUserInfo(Authentication authentication) {
        // authentication.getPrincipal() 應該是 UserDetails
        // 這裡簡化處理，直接回傳 UserDetails 的資訊，實際專案可能需要查 DB 獲取更多資訊 (如 Avatar)

        String username = authentication.getName();
        // 假設我們要回傳 roles, name, avatar
        // 這裡先寫死 avatar，roles 從 authentication 拿 (需確認 authorities 格式)

        List<String> roles = authentication.getAuthorities().stream()
                .map(grantedAuthority -> grantedAuthority.getAuthority())
                .collect(Collectors.toList());

        return ResponseEntity.ok(new UserInfoResponse(
                roles,
                username, // name (display name) - 這裡暫時用 username
                "https://wpimg.wallstcn.com/f778738c-e4f8-4870-b634-56703b4acafe.gif" // avatar
        ));
    }

    // 登出 API（JWT 無狀態，僅回傳成功，前端負責清除 Token）
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok().build();
    }

    // DTO: 登入請求
    @Data
    public static class LoginRequest {
        private String username;
        private String password;
    }

    // DTO: JWT 回應
    @Data
    public static class JwtAuthResponse {
        private String accessToken;
        private String tokenType = "Bearer";

        public JwtAuthResponse(String accessToken) {
            this.accessToken = accessToken;
        }
    }

    @Data
    public static class UserInfoResponse {
        private List<String> roles;
        private String name;
        private String avatar;

        public UserInfoResponse(List<String> roles, String name, String avatar) {
            this.roles = roles;
            this.name = name;
            this.avatar = avatar;
        }
    }
}
