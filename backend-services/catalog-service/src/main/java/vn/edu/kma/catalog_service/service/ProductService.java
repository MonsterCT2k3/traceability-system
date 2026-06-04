package vn.edu.kma.catalog_service.service;

import org.springframework.web.multipart.MultipartFile;
import vn.edu.kma.catalog_service.dto.request.ProductRequest;
import vn.edu.kma.catalog_service.entity.Product;

import java.util.List;

public interface ProductService {
    /**
     * @param image ảnh catalog (multipart); nếu có thì upload Cloudinary và ghi đè {@code request.imageUrl}.
     */
    Product createProduct(ProductRequest request, MultipartFile image, String token);
    List<Product> getAllProducts();
    List<Product> getMyProducts(String token);
    Product getProductById(String id);
}
