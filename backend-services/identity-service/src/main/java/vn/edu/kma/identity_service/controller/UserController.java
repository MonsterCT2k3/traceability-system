package vn.edu.kma.identity_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.kma.identity_service.dto.request.LoginRequest;
import vn.edu.kma.identity_service.dto.response.ApiResponse;
import vn.edu.kma.identity_service.dto.response.AuthResponse;
import vn.edu.kma.identity_service.dto.response.UserResponse;
import vn.edu.kma.identity_service.entity.User;
import vn.edu.kma.identity_service.service.UserService;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(@RequestBody User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<UserResponse>builder()
                        .code(201)
                        .message("Đăng ký thành công")
                        .result(userService.register(user))
                        .build()
        );
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@RequestBody LoginRequest request) {
        String token = userService.login(request.getUsername(), request.getPassword());
        HttpStatus status = HttpStatus.OK; // 200

        return ResponseEntity.status(status).body(
                ApiResponse.<AuthResponse>builder()
                        .code(status.value()) // Lấy số 200 dán vào đây
                        .message("Đăng nhập thành công")
                        .result(new AuthResponse(token, "Bearer"))
                        .build()
        );
    }
}