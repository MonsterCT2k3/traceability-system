package vn.edu.kma.identity_service.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.kma.common.dto.request.UpdateUserRoleRequest;
import vn.edu.kma.common.dto.response.ApiResponse;
import vn.edu.kma.identity_service.dto.response.UserResponse;
import vn.edu.kma.identity_service.config.OpenApiConfig;
import vn.edu.kma.identity_service.service.UserManagementService;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class AdminUserController {

    private final UserManagementService userManagementService;

    @PatchMapping("/{userId}/role")
    public ResponseEntity<ApiResponse<UserResponse>> updateRole(
            @PathVariable String userId,
            @RequestBody UpdateUserRoleRequest request
    ) {
        UserResponse result = userManagementService.updateUserRole(userId, request.getRole());
        return ResponseEntity.ok(
                ApiResponse.<UserResponse>builder()
                        .code(200)
                        .message("Đã cập nhật vai trò")
                        .result(result)
                        .build()
        );
    }
}
