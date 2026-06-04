package vn.edu.kma.trade_logistics_service.domain;

/**
 * NSX đặt NCC (nguyên liệu) hoặc Retailer đặt NSX (thùng sản phẩm).
 */
public enum OrderType {
    MANUFACTURER_TO_SUPPLIER,
    MANUFACTURER_TO_MANUFACTURER,
    RETAILER_TO_MANUFACTURER
}
