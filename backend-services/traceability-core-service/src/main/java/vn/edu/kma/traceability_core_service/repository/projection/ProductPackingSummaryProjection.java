package vn.edu.kma.traceability_core_service.repository.projection;

public interface ProductPackingSummaryProjection {
    String getProductId();
    String getProductName();
    long getCartonsCount();
    long getUnitsCount();
}

