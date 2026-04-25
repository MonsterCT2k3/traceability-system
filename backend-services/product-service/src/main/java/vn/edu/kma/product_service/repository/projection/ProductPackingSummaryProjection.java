package vn.edu.kma.product_service.repository.projection;

public interface ProductPackingSummaryProjection {
    String getProductId();
    String getProductName();
    long getCartonsCount();
    long getUnitsCount();
}
