package com.mediahost.dashboard.service;

import com.mediahost.dashboard.model.dto.request.LoginRequest;
import com.mediahost.dashboard.model.dto.response.AuthResponse;
import com.mediahost.dashboard.model.entity.User;
import com.mediahost.dashboard.repository.UserRepository;
import com.mediahost.dashboard.util.JwtService;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AuthService {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    public AuthResponse login(LoginRequest request) {
        try {
            User user = userRepository.findByUserId(request.getUserId())
                    .orElseThrow(() -> new EntityNotFoundException("User not found"));

            if(!user.getPassword().equals(request.getPassword()))
                throw new BadCredentialsException("Wrong password");

            Map<String, Object> claims = new HashMap<>();
            claims.put("userId", user.getUserId());
            claims.put("name", user.getName());
            claims.put("email", user.getEmail());
            claims.put("level", user.getLevel());

            String accessToken = jwtService.generateToken(user.getUserId(), claims);
            String refreshToken = jwtService.generateRefreshToken(user.getUserId());

            return AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .userId(user.getUserId())
                    .name(user.getName())
                    .level(user.getLevel())
                    .build();

        } catch (BadCredentialsException e) {
            throw new BadCredentialsException("Invalid username or password");
        }
    }

    public AuthResponse refreshToken(String refreshToken) {
        String username = jwtService.extractUsername(refreshToken);

        if (username == null)
            throw new BadCredentialsException("Invalid refresh token");

        User user = userRepository.findByUserId(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getUserId());
        claims.put("name", user.getName());
        claims.put("email", user.getEmail());
        claims.put("level", user.getLevel());

        String newAccessToken = jwtService.generateToken(username, claims);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .userId(user.getUserId())
                .name(user.getName())
                .level(user.getLevel())
                .build();
    }
}