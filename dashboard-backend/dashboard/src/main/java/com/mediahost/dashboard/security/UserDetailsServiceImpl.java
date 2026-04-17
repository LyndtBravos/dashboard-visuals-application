package com.mediahost.dashboard.security;

import com.mediahost.dashboard.model.entity.User;
import com.mediahost.dashboard.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userId));

        if (user.getStatusId() != 1)
            throw new UsernameNotFoundException("User account is inactive");


        return new org.springframework.security.core.userdetails.User(
                user.getUserId(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + getUserRole(user.getLevel())))
        );
    }

    private String getUserRole(Integer level) {
        if (level == null) return "USER";

        return level >= 100 ? "ADMIN" : "USER";
    }
}