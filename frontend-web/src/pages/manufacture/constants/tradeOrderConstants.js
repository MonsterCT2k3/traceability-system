/** Loại đơn — khớp backend OrderType */
export const ORDER_TYPE = {
  MANUFACTURER_TO_SUPPLIER: 'MANUFACTURER_TO_SUPPLIER',
  RETAILER_TO_MANUFACTURER: 'RETAILER_TO_MANUFACTURER',
};

/** Trạng thái đơn — khớp backend TradeOrderStatus */
export const ORDER_STATUS = {
  PENDING: 'PENDING',
  ACCEPTED: 'ACCEPTED',
  REJECTED: 'REJECTED',
  CANCELLED: 'CANCELLED',
  ASSIGNED_TO_CARRIER: 'ASSIGNED_TO_CARRIER',
  PICKED_UP_FROM_SELLER: 'PICKED_UP_FROM_SELLER',
  DELIVERED: 'DELIVERED',
};

export const ORDER_STATUS_META = {
  PENDING: { label: 'Chờ xử lý', color: 'warning' },
  ACCEPTED: { label: 'Đã chấp nhận', color: 'processing' },
  REJECTED: { label: 'Bị từ chối', color: 'error' },
  CANCELLED: { label: 'Đã hủy', color: 'default' },
  ASSIGNED_TO_CARRIER: { label: 'Chờ VC đến nhận hàng', color: 'cyan' },
  PICKED_UP_FROM_SELLER: { label: 'Đang giao (đã lấy hàng)', color: 'geekblue' },
  DELIVERED: { label: 'Đã giao', color: 'success' },
};

export const UNIT_OPTIONS = [
  { value: 'kg', label: 'kg' },
  { value: 'litre', label: 'lít' },
  { value: 'ton', label: 'tấn' },
  { value: 'box', label: 'thùng/hộp' },
];
