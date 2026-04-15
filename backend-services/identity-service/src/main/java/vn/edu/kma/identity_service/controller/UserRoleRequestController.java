package vn.edu.kma.identity_service.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import vn.edu.kma.common.dto.response.ApiResponse;
import vn.edu.kma.identity_service.config.OpenApiConfig;
import vn.edu.kma.identity_service.dto.request.CreateRoleRequestDto;
import vn.edu.kma.identity_service.dto.response.RoleRequestResponse;
import vn.edu.kma.identity_service.entity.User;
import vn.edu.kma.identity_service.repository.UserRepository;
import vn.edu.kma.identity_service.service.RoleRequestService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users/role-requests")
@RequiredArgsConstructor
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class UserRoleRequestController {

    private final RoleRequestService roleRequestService;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<ApiResponse<RoleRequestResponse>> createRequest(
            @RequestBody CreateRoleRequestDto request
    ) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        RoleRequestResponse result = roleRequestService.createRequest(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.<RoleRequestResponse>builder()
                .code(200)
                .message("Đã gửi yêu cầu thành công")
                .result(result)
                .build());
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RoleRequestResponse>>> getMyRequests() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        List<RoleRequestResponse> result = roleRequestService.getMyRequests(user.getId());
        return ResponseEntity.ok(ApiResponse.<List<RoleRequestResponse>>builder()
                .code(200)
                .result(result)
                .build());
    }
}

