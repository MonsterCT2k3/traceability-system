package vn.edu.kma.trade_logistics_service.repository.projection;

public interface ProductPackingSummaryProjection {
    String getProductId();
    String getProductName();
    long getCartonsCount();
    long getUnitsCount();
}
