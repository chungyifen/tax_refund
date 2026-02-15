package com.fox.tax.modules.rbac.service;

import com.fox.tax.modules.rbac.entity.Function;
import com.fox.tax.modules.rbac.entity.Role;
import com.fox.tax.modules.rbac.entity.User;
import com.fox.tax.modules.rbac.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        Set<GrantedAuthority> authorities = new HashSet<>();
        if (user.getRoles() != null) {
            for (Role role : user.getRoles()) {
                // Add role name as authority (e.g. ROLE_ADMIN)
                authorities.add(new SimpleGrantedAuthority(role.getName()));

                // Add functions as authorities (e.g. USER_VIEW)
                if (role.getFunctions() != null) {
                    for (Function function : role.getFunctions()) {
                        authorities.add(new SimpleGrantedAuthority(function.getCode()));
                    }
                }
            }
        }

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .disabled(!user.isEnabled())
                .accountExpired(false)
                .credentialsExpired(false)
                .accountLocked(false)
                .authorities(authorities)
                .build();
    }
}
