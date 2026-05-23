package vn.edu.kma.product_service.service.impl;

import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import vn.edu.kma.product_service.service.IdentityClient;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.kma.product_service.dto.request.ProductRequest;
import vn.edu.kma.product_service.dto.response.PackingUnitSerialItem;
import vn.edu.kma.product_service.dto.response.PalletBulkCartonResult;
import vn.edu.kma.product_service.dto.response.ProductPackingManifestResponse;
import vn.edu.kma.product_service.dto.response.ProductPackingSummaryResponse;
import vn.edu.kma.product_service.entity.Carton;
import vn.edu.kma.product_service.entity.Product;
import vn.edu.kma.product_service.repository.CartonRepository;
import vn.edu.kma.product_service.repository.ProductRepository;
import vn.edu.kma.product_service.repository.ProductUnitRepository;
import vn.edu.kma.product_service.repository.projection.ProductPackingSummaryProjection;
import vn.edu.kma.product_service.service.CloudinaryService;
import vn.edu.kma.product_service.service.ProductService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;
    private final CartonRepository cartonRepository;
    private final ProductUnitRepository productUnitRepository;
    private final CloudinaryService cloudinaryService;
    private final IdentityClient identityClient;

    @Override
    public Product createProduct(ProductRequest request, MultipartFile image, String token) {
        try {
            // 1. Lấy User ID từ Token (Logic tách biệt)
            String userId = extractUserIdFromToken(token);

            String imageUrl = request.getImageUrl();
            if (image != null && !image.isEmpty()) {
                imageUrl = cloudinaryService.uploadImage(image);
            }

            // 2. Map từ DTO sang Entity (Có thể dùng MapStruct sau này)
            Product product = Product.builder()
                    .name(request.getName())
                    .description(request.getDescription())
                    .price(request.getPrice())
                    .imageUrl(imageUrl)
                    .ownerId(userId) // Gán chủ sở hữu
                    .build();

            // 3. Lưu vào DB (cần id trước khi hash payload)
            Product saved = productRepository.save(product);
            return saved;

        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tạo mặt hàng catalog: " + e.getMessage());
        }
    }

    @Override
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Override
    public Product getProductById(String id) {
        return productRepository.findById(id).orElseThrow(() -> new RuntimeException("Mặt hàng catalog không tồn tại"));
    }

    @Override
    public List<ProductPackingSummaryResponse> getMyPackingSummary(String tokenHeader) {
        try {
            String manufacturerId = extractUserIdFromToken(tokenHeader);
            List<ProductPackingSummaryProjection> rows = cartonRepository.summarizePackingByManufacturer(manufacturerId);
            return rows.stream()
                    .map(r -> ProductPackingSummaryResponse.builder()
                            .productId(r.getProductId())
                            .productName(r.getProductName())
                            .cartonsCount(r.getCartonsCount())
                            .unitsCount(r.getUnitsCount())
                            .build())
                    .toList();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi lấy thống kê sản xuất: " + e.getMessage());
        }
    }

    @Override
    public List<ProductPackingManifestResponse> getMyPackingManifest(String tokenHeader) {
        try {
            String manufacturerId = extractUserIdFromToken(tokenHeader);
            List<Carton> cartons = cartonRepository.findByManufacturerIdWithFallback(manufacturerId);
            Map<String, ProductPackingManifestResponse> grouped = new LinkedHashMap<>();

            Map<String, String> ownerNameCache = new java.util.HashMap<>();
            for (Carton carton : cartons) {
                List<PackingUnitSerialItem> unitSerials = productUnitRepository.findByCartonIdOrderByCreatedAtAsc(carton.getId()).stream()
                        .map(u -> PackingUnitSerialItem.builder().unitSerial(u.getUnitSerial()).build())
                        .toList();

                ProductPackingManifestResponse current = grouped.computeIfAbsent(carton.getProduct().getId(), id ->
                        ProductPackingManifestResponse.builder()
                                .productId(id)
                                .productName(productRepository.findById(id).map(Product::getName).orElse("Unknown"))
                                .cartons(new java.util.ArrayList<>())
                                .build()
                );

                String status = (carton.getStatus() != null) ? carton.getStatus() : "IN_STOCK";
                String ownerName = ownerNameCache.computeIfAbsent(carton.getOwnerId(), this::fetchOwnerName);

                current.getCartons().add(PalletBulkCartonResult.builder()
                        .cartonCode(carton.getCartonCode())
                        .ownerId(carton.getOwnerId())
                        .ownerName(ownerName)
                        .status(status)
                        .units(unitSerials)
                        .build());
            }
            return new java.util.ArrayList<>(grouped.values());
        } catch (Exception e) {
            throw new RuntimeException("Lỗi lấy manifest đóng gói: " + e.getMessage());
        }
    }

    private String fetchOwnerName(String actorId) {
        if (actorId == null || actorId.isBlank()) return "Unknown";
        try {
            vn.edu.kma.common.dto.response.ApiResponse<java.util.Map<String, Object>> response = identityClient.getUserById(actorId);
            if (response != null && response.getResult() != null) {
                java.util.Map<String, Object> result = response.getResult();
                Object fullNameObj = result.get("fullName");
                if (fullNameObj != null && !fullNameObj.toString().isBlank()) {
                    return fullNameObj.toString();
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return "Unknown";
    }

    private String resolveProductName(String productId) {
        return productRepository.findById(productId)
                .map(Product::getName)
                .orElse(productId);
    }

    private String extractUserIdFromToken(String token) throws Exception {
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        SignedJWT signedJWT = SignedJWT.parse(token);
        return signedJWT.getJWTClaimsSet().getStringClaim("userId");
    }
}
