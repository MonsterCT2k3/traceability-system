package vn.edu.kma.traceability_core_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.kma.traceability_core_service.domain.PalletInputType;
import vn.edu.kma.traceability_core_service.entity.PalletInput;

import java.util.Collection;
import java.util.List;

public interface PalletInputRepository extends JpaRepository<PalletInput, String> {
    List<PalletInput> findByOutputPalletIdOrderByCreatedAtAsc(String outputPalletId);
    List<PalletInput> findByOutputPalletIdIn(Collection<String> outputPalletIds);
    List<PalletInput> findByInputTypeAndInputId(PalletInputType inputType, String inputId);
    boolean existsByInputTypeAndInputId(PalletInputType inputType, String inputId);
}
