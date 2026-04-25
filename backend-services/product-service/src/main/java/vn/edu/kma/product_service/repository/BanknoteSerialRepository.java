package vn.edu.kma.product_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.kma.product_service.entity.BanknoteSerial;

import java.util.Collection;
import java.util.List;

public interface BanknoteSerialRepository extends JpaRepository<BanknoteSerial, String> {

    List<BanknoteSerial> findBySerialValueIn(Collection<String> serialValues);

    List<BanknoteSerial> findBySerialValueInAndRegisteredByUserId(Collection<String> serialValues, String registeredByUserId);

    long countByRegisteredByUserId(String registeredByUserId);

    @Query(value = """
            SELECT COUNT(*)
            FROM banknote_serial b
            WHERE b.registered_by_user_id = :userId
              AND EXISTS (
                SELECT 1 FROM product_units u
                WHERE u.unit_serial = b.serial_value
              )
            """, nativeQuery = true)
    long countUsedByRegisteredByUserId(@Param("userId") String userId);

    @Query(value = """
            SELECT b.serial_value
            FROM banknote_serial b
            WHERE b.registered_by_user_id = :userId
              AND NOT EXISTS (
                SELECT 1 FROM product_units u
                WHERE u.unit_serial = b.serial_value
              )
            ORDER BY b.created_at ASC
            LIMIT :limitValue
            """, nativeQuery = true)
    List<String> findAvailableSerialValuesForUser(
            @Param("userId") String userId,
            @Param("limitValue") int limitValue
    );
}
