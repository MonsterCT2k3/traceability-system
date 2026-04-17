import React from 'react';
import { Drawer, Descriptions, Table, Typography, Empty, Spin } from 'antd';
import UserDirectoryDisplay from '../../../../components/trade-order/UserDirectoryDisplay';
import OrderStatusTag from '../../../manufacture/components/orders/OrderStatusTag';
import { ORDER_TYPE } from '../../../manufacture/constants/tradeOrderConstants';

const { Text } = Typography;

const lineColumns = [
  { title: '#', dataIndex: 'lineIndex', key: 'lineIndex', width: 48 },
  { title: 'ID lô', dataIndex: 'targetRawBatchId', key: 'targetRawBatchId', ellipsis: true },
  {
    title: 'Số lượng đặt',
    key: 'qty',
    render: (_, r) => (r.quantityRequested != null ? `${r.quantityRequested} ${r.unit || ''}` : '—'),
  },
];

const SellerOrderDetailDrawer = ({ open, loading, order, onClose }) => {
  if (loading && !order) {
    return (
      <Drawer title="Chi tiết đơn" placement="right" width={640} open={open} onClose={onClose}>
        <div style={{ padding: 48, textAlign: 'center' }}>
          <Spin size="large" />
        </div>
      </Drawer>
    );
  }

  if (!order) {
    return (
      <Drawer title="Chi tiết đơn" placement="right" width={560} open={open} onClose={onClose}>
        <Empty description="Không có dữ liệu" />
      </Drawer>
    );
  }

  const isM2S = order.orderType === ORDER_TYPE.MANUFACTURER_TO_SUPPLIER;

  return (
    <Drawer title={`Đơn ${order.orderCode || ''}`} placement="right" width={640} open={open} onClose={onClose}>
      <Descriptions bordered column={1} size="small" style={{ marginBottom: 16 }}>
        <Descriptions.Item label="Mã đơn">{order.orderCode}</Descriptions.Item>
        <Descriptions.Item label="Loại">{order.orderType}</Descriptions.Item>
        <Descriptions.Item label="Trạng thái">
          <OrderStatusTag status={order.status} />
        </Descriptions.Item>
        <Descriptions.Item label="Người mua (NSX)">
          <UserDirectoryDisplay userId={order.buyerId} />
        </Descriptions.Item>
        <Descriptions.Item label="Ghi chú">{order.note || '—'}</Descriptions.Item>
        <Descriptions.Item label="Đơn vị vận chuyển">
          {order.carrierId ? <UserDirectoryDisplay userId={order.carrierId} /> : '—'}
        </Descriptions.Item>
        <Descriptions.Item label="Tạo lúc">
          {order.createdAt ? new Date(order.createdAt).toLocaleString('vi-VN') : '—'}
        </Descriptions.Item>
        {order.status === 'DELIVERED' && (
          <>
            <Descriptions.Item label="Chain (giao hàng)">{order.deliveryChainStatus || '—'}</Descriptions.Item>
            <Descriptions.Item label="TxHash">{order.deliveryTxHash || '—'}</Descriptions.Item>
            {order.deliveryChainError && (
              <Descriptions.Item label="Lỗi chain">
                <Text type="danger">{order.deliveryChainError}</Text>
              </Descriptions.Item>
            )}
          </>
        )}
      </Descriptions>

      <Typography.Title level={5}>Dòng đặt hàng</Typography.Title>
      {isM2S && order.lines?.length > 0 ? (
        <Table
          size="small"
          rowKey={(r) => r.id || `${r.lineIndex}`}
          columns={lineColumns}
          dataSource={[...order.lines].sort((a, b) => (a.lineIndex ?? 0) - (b.lineIndex ?? 0))}
          pagination={false}
        />
      ) : (
        <Empty description="Không có dòng lô (hoặc loại đơn khác)" />
      )}
    </Drawer>
  );
};

export default SellerOrderDetailDrawer;
