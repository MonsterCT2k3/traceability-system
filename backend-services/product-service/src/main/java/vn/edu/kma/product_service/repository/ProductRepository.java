package vn.edu.kma.product_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.kma.product_service.entity.Product;

public interface ProductRepository extends JpaRepository<Product, String> {


}
