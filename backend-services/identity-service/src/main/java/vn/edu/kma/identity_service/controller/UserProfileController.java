package vn.edu.kma.identity_service.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.kma.common.dto.response.ApiResponse;
import vn.edu.kma.identity_service.config.OpenApiConfig;
import vn.edu.kma.identity_service.dto.request.UpdateProfileRequest;
import vn.edu.kma.identity_service.dto.response.UserResponse;
import vn.edu.kma.identity_service.service.UserManagementService;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/users/profile")
@RequiredArgsConstructor
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class UserProfileController {

    private final UserManagementService userManagementService;

    @GetMapping
    public ResponseEntity<ApiResponse<UserResponse>> getMyProfile() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        UserResponse result = userManagementService.getUserByUsername(username);
        return ResponseEntity.ok(ApiResponse.<UserResponse>builder()
                .code(200)
                .result(result)
                .build());
    }

    @PutMapping
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @RequestBody UpdateProfileRequest request
    ) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        UserResponse result = userManagementService.updateProfile(username, request);
        return ResponseEntity.ok(ApiResponse.<UserResponse>builder()
                .code(200)
                .message("Cập nhật thông tin thành công")
                .result(result)
                .build());
    }

    @PostMapping("/avatar")
    public ResponseEntity<ApiResponse<UserResponse>> uploadAvatar(
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        UserResponse result = userManagementService.updateAvatar(username, file);
        return ResponseEntity.ok(ApiResponse.<UserResponse>builder()
                .code(200)
                .message("Cập nhật ảnh đại diện thành công")
                .result(result)
                .build());
    }
}
