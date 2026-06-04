package vn.edu.kma.catalog_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.kma.catalog_service.entity.Product;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, String> {
    List<Product> findAllByOwnerIdOrderByNameAsc(String ownerId);
}
