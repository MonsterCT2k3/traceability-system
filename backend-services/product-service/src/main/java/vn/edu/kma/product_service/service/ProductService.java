package vn.edu.kma.product_service.service;

import vn.edu.kma.product_service.dto.request.ProductRequest;
import vn.edu.kma.product_service.entity.Product;

import java.util.List;

public interface ProductService {
    Product createProduct(ProductRequest request, String token);
    List<Product> getAllProducts();
    Product getProductById(String id);
}
