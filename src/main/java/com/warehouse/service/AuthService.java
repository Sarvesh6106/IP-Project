package com.warehouse.service;

import com.warehouse.dto.AuthDto;
import com.warehouse.entity.User;
import com.warehouse.repository.UserRepository;
import com.warehouse.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    public AuthDto.AuthResponse login(AuthDto.LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (!user.getPassword().equals(request.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }

        String token = jwtUtil.generateToken(user);

        Long branchId = user.getAssignedBranch() != null ? user.getAssignedBranch().getId() : null;
        String branchName = user.getAssignedBranch() != null ? user.getAssignedBranch().getName() : null;

        return new AuthDto.AuthResponse(
                token,
                "Bearer",
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                branchId,
                branchName
        );
    }

    public AuthDto.AuthResponse register(AuthDto.RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email is already registered: " + request.getEmail());
        }

        User.Role role = User.Role.STAFF;

        User user = new User(
                null,
                request.getName(),
                request.getEmail(),
                request.getPassword(),
                role,
                null
        );

        user = userRepository.save(user);
        String token = jwtUtil.generateToken(user);

        Long branchId = user.getAssignedBranch() != null ? user.getAssignedBranch().getId() : null;
        String branchName = user.getAssignedBranch() != null ? user.getAssignedBranch().getName() : null;

        return new AuthDto.AuthResponse(
                token,
                "Bearer",
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                branchId,
                branchName
        );
    }

    public AuthDto.UserDto getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));

        Long branchId = user.getAssignedBranch() != null ? user.getAssignedBranch().getId() : null;
        String branchName = user.getAssignedBranch() != null ? user.getAssignedBranch().getName() : null;

        return new AuthDto.UserDto(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                branchId,
                branchName
        );
    }
}
