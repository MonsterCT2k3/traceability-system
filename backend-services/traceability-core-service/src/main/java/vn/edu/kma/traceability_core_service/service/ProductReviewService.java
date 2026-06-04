package vn.edu.kma.traceability_core_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.kma.common.dto.response.ApiResponse;
import vn.edu.kma.traceability_core_service.client.CatalogClient;
import vn.edu.kma.traceability_core_service.dto.request.ReviewCreateRequest;
import vn.edu.kma.traceability_core_service.dto.response.*;
import vn.edu.kma.traceability_core_service.entity.*;
import vn.edu.kma.traceability_core_service.repository.*;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ProductReviewService {
    private final ClaimTokenService claimTokenService;
    private final ProductUnitClaimRepository claimRepository;
    private final ProductReviewRepository reviewRepository;
    private final ReviewMediaRepository mediaRepository;
    private final CatalogClient catalogClient;
    private final IdentityClient identityClient;
    private final CloudinaryService cloudinaryService;

    @Transactional(readOnly = true)
    public ClaimResolveResponse resolve(String token, String userId) {
        ProductUnitClaim claim = claimRepository.findByClaimTokenHash(claimTokenService.hash(token))
                .orElseThrow(() -> new RuntimeException("Claim QR khong hop le"));
        if ("REVOKED".equals(claim.getStatus())) throw new RuntimeException("Claim QR da bi thu hoi");
        ProductReviewResponse existing = null;
        String status = claim.getStatus();
        if ("CLAIMED".equals(status)) {
            if (!userId.equals(claim.getClaimedByUserId())) throw new RuntimeException("Claim QR da duoc su dung");
            status = "CLAIMED_BY_ME";
            existing = reviewRepository.findByClaimId(claim.getId()).map(this::toResponse).orElse(null);
        }
        Map<String, Object> product = product(claim.getProductUnit().getProductId());
        return ClaimResolveResponse.builder()
                .claimStatus(status).productUnitId(claim.getProductUnit().getId())
                .unitSerial(claim.getProductUnit().getUnitSerial()).productId(claim.getProductUnit().getProductId())
                .productName(value(product, "name")).productImageUrl(value(product, "imageUrl"))
                .existingReview(existing).build();
    }

    @Transactional
    public ReviewMediaResponse upload(MultipartFile file, String requestedType, String userId) throws Exception {
        if (file == null || file.isEmpty()) throw new RuntimeException("File rong");
        String contentType = Optional.ofNullable(file.getContentType()).orElse("");
        boolean video = contentType.startsWith("video/");
        boolean image = contentType.startsWith("image/");
        if (!video && !image) throw new RuntimeException("Chi ho tro anh hoac video");
        if (video && file.getSize() > 50L * 1024 * 1024) throw new RuntimeException("Video vuot qua 50 MB");
        if (image && file.getSize() > 5L * 1024 * 1024) throw new RuntimeException("Anh vuot qua 5 MB");
        Map<?, ?> uploaded = cloudinaryService.uploadReviewMedia(file, video);
        Integer duration = uploaded.get("duration") instanceof Number n ? n.intValue() : null;
        if (video && duration != null && duration > 30) throw new RuntimeException("Video vuot qua 30 giay");
        String publicId = String.valueOf(uploaded.get("public_id"));
        String url = String.valueOf(uploaded.get("secure_url") != null ? uploaded.get("secure_url") : uploaded.get("url"));
        String thumbnail = video ? url.replace("/video/upload/", "/video/upload/so_0/").replaceAll("\\.[^.]+$", ".jpg") : url;
        ReviewMedia saved = mediaRepository.save(ReviewMedia.builder()
                .uploaderId(userId).mediaType(video ? "VIDEO" : "IMAGE").mediaUrl(url).thumbnailUrl(thumbnail)
                .cloudinaryPublicId(publicId).fileSize(file.getSize()).durationSeconds(duration)
                .sortOrder(0).status("UPLOADED").build());
        return mediaResponse(saved);
    }

    @Transactional
    public ProductReviewResponse create(ReviewCreateRequest request, String userId) {
        validateReview(request);
        ProductUnitClaim claim = claimRepository.findByHashForUpdate(claimTokenService.hash(request.getClaimToken()))
                .orElseThrow(() -> new RuntimeException("Claim QR khong hop le"));
        if (!"AVAILABLE".equals(claim.getStatus())) throw new RuntimeException("Claim QR da duoc su dung");
        List<String> ids = request.getMediaIds() == null ? List.of() : request.getMediaIds();
        List<ReviewMedia> media = mediaRepository.findByIdIn(ids);
        if (media.size() != new HashSet<>(ids).size()) throw new RuntimeException("Media khong ton tai");
        long images = media.stream().filter(m -> "IMAGE".equals(m.getMediaType())).count();
        long videos = media.stream().filter(m -> "VIDEO".equals(m.getMediaType())).count();
        if (images > 5 || videos > 1) throw new RuntimeException("Toi da 5 anh va 1 video");
        if (media.stream().anyMatch(m -> !userId.equals(m.getUploaderId()) || !"UPLOADED".equals(m.getStatus()))) {
            throw new RuntimeException("Media khong hop le");
        }
        ProductUnit unit = claim.getProductUnit();
        ProductReview review = reviewRepository.save(ProductReview.builder()
                .claim(claim).productUnit(unit).productId(unit.getProductId()).reviewerId(userId)
                .rating(request.getRating()).content(blankToNull(request.getContent())).status("PUBLISHED").build());
        for (int i = 0; i < media.size(); i++) {
            ReviewMedia item = media.get(i);
            item.setReview(review); item.setStatus("ATTACHED"); item.setSortOrder(i); item.setAttachedAt(LocalDateTime.now());
            mediaRepository.save(item);
        }
        claim.setStatus("CLAIMED"); claim.setClaimedByUserId(userId); claim.setClaimedAt(LocalDateTime.now());
        claimRepository.save(claim);
        return toResponse(review);
    }

    @Transactional
    public ProductReviewResponse update(String reviewId, ReviewCreateRequest request, String userId) {
        validateReview(request);
        ProductReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Danh gia khong ton tai"));
        if (!userId.equals(review.getReviewerId())) {
            throw new RuntimeException("Ban khong co quyen sua danh gia nay");
        }
        review.setRating(request.getRating());
        review.setContent(blankToNull(request.getContent()));
        return toResponse(reviewRepository.save(review));
    }

    @Transactional(readOnly = true)
    public Page<ProductReviewResponse> list(String productId, Pageable pageable) {
        return reviewRepository.findByProductIdAndStatusOrderByCreatedAtDesc(productId, "PUBLISHED", pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<ProductReviewResponse> listMine(String userId, Pageable pageable) {
        return reviewRepository.findByReviewerIdOrderByCreatedAtDesc(userId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ReviewSummaryResponse summary(String productId) {
        List<ProductReviewResponse> reviews = list(productId, PageRequest.of(0, 10000)).getContent();
        Map<Integer, Long> counts = new LinkedHashMap<>();
        for (int i = 5; i >= 1; i--) { final int rating = i; counts.put(i, reviews.stream().filter(r -> r.getRating() == rating).count()); }
        double avg = reviews.stream().mapToInt(ProductReviewResponse::getRating).average().orElse(0);
        return ReviewSummaryResponse.builder().totalReviews(reviews.size()).averageRating(Math.round(avg * 10.0) / 10.0).ratingCounts(counts).build();
    }

    private ProductReviewResponse toResponse(ProductReview review) {
        Map<String, Object> p = product(review.getProductId());
        Map<String, Object> actor = identity(review.getReviewerId());
        return ProductReviewResponse.builder().id(review.getId()).productId(review.getProductId())
                .productName(value(p, "name")).productImageUrl(value(p, "imageUrl"))
                .productDescription(value(p, "description"))
                .unitSerial(review.getProductUnit().getUnitSerial()).rating(review.getRating()).content(review.getContent())
                .reviewerName(value(actor, "fullName")).reviewerAvatarUrl(value(actor, "avatarUrl")).verifiedOwnership(true)
                .media(mediaRepository.findByReviewIdOrderBySortOrderAsc(review.getId()).stream().map(this::mediaResponse).toList())
                .createdAt(review.getCreatedAt()).updatedAt(review.getUpdatedAt()).build();
    }

    private ReviewMediaResponse mediaResponse(ReviewMedia m) {
        return ReviewMediaResponse.builder().id(m.getId()).mediaType(m.getMediaType()).mediaUrl(m.getMediaUrl()).thumbnailUrl(m.getThumbnailUrl()).build();
    }
    private Map<String, Object> product(String id) { ApiResponse<Map<String,Object>> r = catalogClient.getProductById(id); return r == null || r.getResult() == null ? Map.of() : r.getResult(); }
    private Map<String, Object> identity(String id) { try { ApiResponse<Map<String,Object>> r = identityClient.getUserById(id); return r == null || r.getResult() == null ? Map.of() : r.getResult(); } catch (Exception e) { return Map.of(); } }
    private static String value(Map<String,Object> m, String key) { Object v = m.get(key); return v == null ? null : v.toString(); }
    private static String blankToNull(String s) { return s == null || s.isBlank() ? null : s.trim(); }
    private static void validateReview(ReviewCreateRequest request) {
        if (request.getRating() == null || request.getRating() < 1 || request.getRating() > 5) {
            throw new RuntimeException("Rating phai tu 1 den 5");
        }
        if (request.getContent() != null && request.getContent().length() > 2000) {
            throw new RuntimeException("Noi dung qua dai");
        }
    }
}
