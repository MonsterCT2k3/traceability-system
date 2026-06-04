package vn.edu.kma.traceability_core_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.kma.common.dto.response.ApiResponse;
import vn.edu.kma.traceability_core_service.dto.request.*;
import vn.edu.kma.traceability_core_service.dto.response.*;
import vn.edu.kma.traceability_core_service.service.ProductReviewService;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ProductReviewController {
    private final ProductReviewService service;

    @PostMapping("/claims/resolve")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<ClaimResolveResponse>> resolve(@RequestBody ClaimResolveRequest request, Authentication auth) {
        return ok(service.resolve(request.getClaimToken(), auth.getName()));
    }

    @PostMapping("/review-media")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<ReviewMediaResponse>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "mediaType", required = false) String mediaType,
            Authentication auth) throws Exception {
        return ok(service.upload(file, mediaType, auth.getName()));
    }

    @PostMapping("/reviews")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<ProductReviewResponse>> create(@RequestBody ReviewCreateRequest request, Authentication auth) {
        return ok(service.create(request, auth.getName()));
    }

    @PutMapping("/reviews/{reviewId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<ProductReviewResponse>> update(
            @PathVariable String reviewId,
            @RequestBody ReviewCreateRequest request,
            Authentication auth) {
        return ok(service.update(reviewId, request, auth.getName()));
    }

    @GetMapping("/reviews/my")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Page<ProductReviewResponse>>> listMine(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        return ok(service.listMine(auth.getName(), PageRequest.of(page, Math.min(Math.max(size, 1), 50))));
    }

    @GetMapping("/products/{productId}/reviews")
    public ResponseEntity<ApiResponse<Page<ProductReviewResponse>>> list(
            @PathVariable String productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ok(service.list(productId, PageRequest.of(page, Math.min(Math.max(size, 1), 50))));
    }

    @GetMapping("/products/{productId}/review-summary")
    public ResponseEntity<ApiResponse<ReviewSummaryResponse>> summary(@PathVariable String productId) {
        return ok(service.summary(productId));
    }

    private static <T> ResponseEntity<ApiResponse<T>> ok(T result) {
        return ResponseEntity.ok(ApiResponse.<T>builder().code(200).message("OK").result(result).build());
    }
}
