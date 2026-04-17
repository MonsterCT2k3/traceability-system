import React from 'react';
import { Table, Button, Space, Popconfirm, Typography } from 'antd';
import { EyeOutlined, StopOutlined } from '@ant-design/icons';
import OrderStatusTag from './OrderStatusTag';
import { ORDER_TYPE, ORDER_STATUS } from '../../constants/tradeOrderConstants';

const { Text } = Typography;

const PurchaseOrderTable = ({
  orders,
  loading,
  onViewDetail,
  onCancel,
  cancellingId,
}) => {
  const columns = [
    {
      title: 'Mã đơn',
      dataIndex: 'orderCode',
      key: 'orderCode',
      render: (t) => <Text strong>{t}</Text>,
    },
    {
      title: 'NCC (seller)',
      dataIndex: 'sellerId',
      key: 'sellerId',
      ellipsis: true,
      render: (id) => <Text copyable={{ text: id }}>{id}</Text>,
    },
    {
      title: 'Số dòng',
      key: 'lines',
      width: 88,
      render: (_, r) => r.lines?.length ?? 0,
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      render: (s) => <OrderStatusTag status={s} />,
    },
    {
      title: 'Tạo lúc',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 168,
      render: (d) => (d ? new Date(d).toLocaleString('vi-VN') : '—'),
    },
    {
      title: '',
      key: 'actions',
      width: 200,
      render: (_, record) => (
        <Space size="small">
          <Button type="link" size="small" icon={<EyeOutlined />} onClick={() => onViewDetail(record)}>
            Chi tiết
          </Button>
          {record.status === ORDER_STATUS.PENDING && (
            <Popconfirm
              title="Hủy đơn này?"
              description="Chỉ hủy được khi đơn còn chờ NCC xử lý."
              okText="Hủy đơn"
              cancelText="Không"
              onConfirm={() => onCancel(record.id)}
            >
              <Button type="link" size="small" danger icon={<StopOutlined />} loading={cancellingId === record.id}>
                Hủy
              </Button>
            </Popconfirm>
          )}
        </Space>
      ),
    },
  ];

  const data = (orders || []).filter((o) => o.orderType === ORDER_TYPE.MANUFACTURER_TO_SUPPLIER);

  return (
    <Table
      rowKey="id"
      loading={loading}
      columns={columns}
      dataSource={data}
      pagination={{ pageSize: 8, showSizeChanger: true, pageSizeOptions: ['8', '16', '32'] }}
    />
  );
};

export default PurchaseOrderTable;
