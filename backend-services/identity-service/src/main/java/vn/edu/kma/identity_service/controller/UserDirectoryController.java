package vn.edu.kma.identity_service.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.kma.common.dto.response.ApiResponse;
import vn.edu.kma.identity_service.config.OpenApiConfig;
import vn.edu.kma.identity_service.dto.response.SupplierDirectoryResponse;
import vn.edu.kma.identity_service.service.UserManagementService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users/directory")
@RequiredArgsConstructor
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class UserDirectoryController {

    private final UserManagementService userManagementService;

    @GetMapping("/suppliers")
    public ResponseEntity<ApiResponse<List<SupplierDirectoryResponse>>> searchSuppliers(
            @RequestParam(value = "q", required = false) String q
    ) {
        List<SupplierDirectoryResponse> result = userManagementService.searchSuppliersDirectory(q);
        return ResponseEntity.ok(ApiResponse.<List<SupplierDirectoryResponse>>builder()
                .code(200)
                .message("OK")
                .result(result)
                .build());
    }

    @GetMapping("/transporters")
    public ResponseEntity<ApiResponse<List<SupplierDirectoryResponse>>> searchTransporters(
            @RequestParam(value = "q", required = false) String q
    ) {
        List<SupplierDirectoryResponse> result = userManagementService.searchTransportersDirectory(q);
        return ResponseEntity.ok(ApiResponse.<List<SupplierDirectoryResponse>>builder()
                .code(200)
                .message("OK")
                .result(result)
                .build());
    }

    @GetMapping("/by-id/{userId}")
    public ResponseEntity<ApiResponse<SupplierDirectoryResponse>> getByIdForDirectory(
            @PathVariable("userId") String userId
    ) {
        SupplierDirectoryResponse result = userManagementService.getDirectoryUserById(userId);
        return ResponseEntity.ok(ApiResponse.<SupplierDirectoryResponse>builder()
                .code(200)
                .message("OK")
                .result(result)
                .build());
    }
}
