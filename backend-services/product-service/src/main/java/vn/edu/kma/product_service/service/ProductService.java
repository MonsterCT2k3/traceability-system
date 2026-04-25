package vn.edu.kma.product_service.service;

import org.springframework.web.multipart.MultipartFile;
import vn.edu.kma.product_service.dto.request.ProductRequest;
import vn.edu.kma.product_service.dto.response.ProductPackingManifestResponse;
import vn.edu.kma.product_service.dto.response.ProductPackingSummaryResponse;
import vn.edu.kma.product_service.entity.Product;

import java.util.List;

public interface ProductService {
    /**
     * @param image ảnh catalog (multipart); nếu có thì upload Cloudinary và ghi đè {@code request.imageUrl}.
     */
    Product createProduct(ProductRequest request, MultipartFile image, String token);
    List<Product> getAllProducts();
    Product getProductById(String id);
    List<ProductPackingSummaryResponse> getMyPackingSummary(String tokenHeader);
    List<ProductPackingManifestResponse> getMyPackingManifest(String tokenHeader);
}
