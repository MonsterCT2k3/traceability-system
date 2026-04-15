package vn.edu.kma.identity_service.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import vn.edu.kma.common.security.UserRole;
import vn.edu.kma.identity_service.entity.User;
import vn.edu.kma.identity_service.repository.UserRepository;

/**
 * Chỉ khi {@code spring.profiles.active=dev}: tạo user {@code admin} / {@code admin123} nếu chưa có.
 * Không bật trên prod.
 */
@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DevAdminSeeder implements ApplicationRunner {

    private static final String ADMIN_USER = "admin";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.findByUsername(ADMIN_USER).isPresent()) {
            return;
        }
        User u = User.builder()
                .username(ADMIN_USER)
                .password(passwordEncoder.encode("admin123"))
                .fullName("Dev Admin")
                .role(UserRole.ADMIN.name())
                .build();
        userRepository.save(u);
        log.warn("DEV ONLY: đã tạo user '{}' / password 'admin123'", ADMIN_USER);
    }
}
