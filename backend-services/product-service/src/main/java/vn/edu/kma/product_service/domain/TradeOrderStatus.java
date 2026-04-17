package vn.edu.kma.product_service.domain;

public enum TradeOrderStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    CANCELLED,
    /** Đã gán đơn vị vận chuyển, chờ VC đến nhận hàng tại người bán */
    ASSIGNED_TO_CARRIER,
    /** VC đã nhận hàng từ người bán, đang vận chuyển tới người mua */
    PICKED_UP_FROM_SELLER,
    /** Giao hàng thành công */
    DELIVERED
}
