package vn.edu.kma.trade_logistics_service.domain;

public enum TradeOrderStatus {
    PENDING,
    PROCESSING, // Thêm trạng thái chờ xử lý qua Kafka
    ACCEPTED,
    ERROR,      // Thêm trạng thái lỗi khi có sự cố bất đồng bộ
    REJECTED,
    CANCELLED,
    /** Đã gán đơn vị vận chuyển, chờ VC đến nhận hàng tại người bán */
    ASSIGNED_TO_CARRIER,
    /** VC đã nhận hàng từ người bán, đang vận chuyển tới người mua */
    PICKED_UP_FROM_SELLER,
    /** Giao hàng thành công */
    DELIVERED
}
