import React from 'react';
import { Drawer, Descriptions, Table, Typography, Empty, Spin } from 'antd';
import { useQuery } from '@tanstack/react-query';
import api from '../../../../lib/api';
import UserDirectoryDisplay from '../../../../components/trade-order/UserDirectoryDisplay';
import OrderStatusTag from '../../../manufacture/components/orders/OrderStatusTag';
import { ORDER_TYPE } from '../../../manufacture/constants/tradeOrderConstants';

const { Text } = Typography;

const SellerOrderDetailDrawer = ({ open, loading, order, onClose }) => {
  const isM2S = order?.orderType === ORDER_TYPE.MANUFACTURER_TO_SUPPLIER;

  const RawBatchNameDisplay = ({ batchId }) => {
    const { data: batch, isLoading } = useQuery({
      queryKey: ['rawBatchDetail', batchId],
      queryFn: async () => {
        const res = await api.get(`/product/api/v1/raw-batches/${batchId}`);
        return res.data?.result;
      },
      enabled: !!batchId,
      staleTime: 5 * 60 * 1000,
    });

    if (isLoading) return <Text type="secondary" style={{ fontSize: 13 }}>Đang tải...</Text>;
    if (!batch) return <Text type="secondary" style={{ fontSize: 13 }}>{batchId?.substring(0, 8)}...</Text>;
    return <span>{batch.materialName}</span>;
  };

  const lineColumns = [
    { title: '#', dataIndex: 'lineIndex', key: 'lineIndex', width: 48 },
    { 
      title: 'Tên lô hàng', 
      dataIndex: 'targetRawBatchId', 
      key: 'targetRawBatchId', 
      render: (val) => <RawBatchNameDisplay batchId={val} />
    },
    {
      title: 'Số lượng đặt',
      key: 'qty',
      render: (_, r) => (r.quantityRequested != null ? `${r.quantityRequested} ${r.unit || ''}` : '—'),
    },
  ];

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
