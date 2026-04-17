import React from 'react';
import { Tag } from 'antd';
import { ORDER_STATUS_META } from '../../constants/tradeOrderConstants';

/**
 * Hiển thị trạng thái đơn hàng (enum backend → nhãn tiếng Việt).
 */
const OrderStatusTag = ({ status }) => {
  const meta = ORDER_STATUS_META[status] || { label: status || '—', color: 'default' };
  return <Tag color={meta.color}>{meta.label}</Tag>;
};

export default OrderStatusTag;
