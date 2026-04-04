package vn.edu.kma.product_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.kma.product_service.entity.Carton;

import java.util.List;
import java.util.Optional;

public interface CartonRepository extends JpaRepository<Carton, String> {

    Optional<Carton> findByCartonCode(String cartonCode);

    List<Carton> findByPalletIdOrderByCreatedAtDesc(String palletId);
}
