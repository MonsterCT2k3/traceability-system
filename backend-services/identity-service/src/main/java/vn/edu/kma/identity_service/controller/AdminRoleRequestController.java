package vn.edu.kma.identity_service.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.kma.common.dto.response.ApiResponse;
import vn.edu.kma.identity_service.config.OpenApiConfig;
import vn.edu.kma.identity_service.dto.response.RoleRequestResponse;
import vn.edu.kma.identity_service.service.RoleRequestService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/role-requests")
@RequiredArgsConstructor
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class AdminRoleRequestController {

    private final RoleRequestService roleRequestService;

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<RoleRequestResponse>>> getPendingRequests() {
        List<RoleRequestResponse> result = roleRequestService.getPendingRequests();
        return ResponseEntity.ok(ApiResponse.<List<RoleRequestResponse>>builder()
                .code(200)
                .result(result)
                .build());
    }

    @PostMapping("/{requestId}/approve")
    public ResponseEntity<ApiResponse<RoleRequestResponse>> approveRequest(@PathVariable String requestId) {
        RoleRequestResponse result = roleRequestService.approveRequest(requestId);
        return ResponseEntity.ok(ApiResponse.<RoleRequestResponse>builder()
                .code(200)
                .message("Đã duyệt yêu cầu")
                .result(result)
                .build());
    }

    @PostMapping("/{requestId}/reject")
    public ResponseEntity<ApiResponse<RoleRequestResponse>> rejectRequest(@PathVariable String requestId) {
        RoleRequestResponse result = roleRequestService.rejectRequest(requestId);
        return ResponseEntity.ok(ApiResponse.<RoleRequestResponse>builder()
                .code(200)
                .message("Đã từ chối yêu cầu")
                .result(result)
                .build());
    }
}
