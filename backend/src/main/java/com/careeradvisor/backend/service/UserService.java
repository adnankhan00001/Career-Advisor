package com.careeradvisor.backend.service;

import com.careeradvisor.backend.dto.AuthResponse;
import com.careeradvisor.backend.dto.LoginRequest;
import com.careeradvisor.backend.dto.SignupRequest;
import com.careeradvisor.backend.exception.DuplicateResourceException;
import com.careeradvisor.backend.model.Role;
import com.careeradvisor.backend.model.User;
import com.careeradvisor.backend.model.UserStatus;
import com.careeradvisor.backend.repository.UserRepository;
import com.careeradvisor.backend.security.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtils jwtUtils;

    @Transactional
    public AuthResponse register(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered: " + request.getEmail());
        }

        // Enforce Role.USER and UserStatus.ACTIVE explicitly - never allow role or status override from registration payload
        User user = User.builder()
                .name(request.getName().trim())
                .email(request.getEmail().trim().toLowerCase())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build();

        User savedUser = userRepository.save(user);

        String token = jwtUtils.generateToken(savedUser.getEmail(), savedUser.getId(), savedUser.getRole());

        return new AuthResponse(token, savedUser.getId(), savedUser.getName(), savedUser.getEmail(), savedUser.getRole().name());
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        if (user.getStatus() == UserStatus.SUSPENDED) {
            String msg = StringUtils.hasText(user.getSuspensionReason())
                    ? "Your account has been suspended: " + user.getSuspensionReason()
                    : "Your account has been suspended. Please contact administrator.";
            throw new LockedException(msg);
        }

        if (user.getStatus() == UserStatus.DEACTIVATED) {
            throw new DisabledException("Your account has been deactivated.");
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.getPassword())
        );

        Role userRole = user.getRole() != null ? user.getRole() : Role.USER;

        String token = jwtUtils.generateToken(user.getEmail(), user.getId(), userRole);

        return new AuthResponse(token, user.getId(), user.getName(), user.getEmail(), userRole.name());
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }
}