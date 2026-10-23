package com.careeradvisor.backend.service;

import com.careeradvisor.backend.dto.AdminUserDetailDto;
import com.careeradvisor.backend.model.Role;
import com.careeradvisor.backend.model.User;
import com.careeradvisor.backend.model.UserStatus;
import com.careeradvisor.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AdminServiceIntegrationTest {

    @Autowired
    private AdminService adminService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void testGetUserDetailRealDatabase() {
        User admin = userRepository.findByEmail("admin@careeradvisor.dev").orElseGet(() -> {
            User u = User.builder()
                    .name("System Administrator")
                    .email("admin@careeradvisor.dev")
                    .password("hash")
                    .role(Role.ADMIN)
                    .status(UserStatus.ACTIVE)
                    .build();
            return userRepository.save(u);
        });

        assertNotNull(admin);
        AdminUserDetailDto detail = adminService.getUserDetail(admin.getId());
        assertNotNull(detail);
        assertEquals(admin.getEmail(), detail.getEmail());
        System.out.println(">>> SUCCESSFULLY RETRIEVED USER DETAIL: " + detail);
    }
}
