import React from 'react';
import { Button, Popconfirm, Space, Table, Tag, Typography } from 'antd';
import { EyeOutlined, StopOutlined } from '@ant-design/icons';
import OrderStatusTag from './OrderStatusTag';
import { ORDER_STATUS, ORDER_TYPE } from '../../constants/tradeOrderConstants';

const { Text } = Typography;

const ORDER_TYPE_META = {
  [ORDER_TYPE.MANUFACTURER_TO_SUPPLIER]: {
    color: 'green',
    label: 'Từ nhà cung cấp',
  },
  [ORDER_TYPE.MANUFACTURER_TO_MANUFACTURER]: {
    color: 'blue',
    label: 'Từ nhà sản xuất',
  },
};

const PurchaseOrderTable = ({
  orders,
  loading,
  onViewDetail,
  onCancel,
  cancellingId,
  orderTypes = [ORDER_TYPE.MANUFACTURER_TO_SUPPLIER],
}) => {
  const columns = [
    {
      title: 'Mã đơn',
      dataIndex: 'orderCode',
      key: 'orderCode',
      render: (value) => <Text strong>{value}</Text>,
    },
    {
      title: 'Nguồn mua',
      dataIndex: 'orderType',
      key: 'orderType',
      width: 170,
      render: (value) => {
        const meta = ORDER_TYPE_META[value] || { color: 'default', label: value };
        return <Tag color={meta.color}>{meta.label}</Tag>;
      },
    },
    {
      title: 'Người bán',
      dataIndex: 'sellerId',
      key: 'sellerId',
      ellipsis: true,
      render: (id) => <Text copyable={{ text: id }}>{id}</Text>,
    },
    {
      title: 'Số dòng',
      key: 'lines',
      width: 96,
      render: (_, record) => record.lines?.length ?? 0,
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      width: 150,
      render: (status) => <OrderStatusTag status={status} />,
    },
    {
      title: 'Tạo lúc',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 180,
      render: (value) => (value ? new Date(value).toLocaleString('vi-VN') : '-'),
    },
    {
      title: '',
      key: 'actions',
      width: 210,
      render: (_, record) => (
        <Space size="small">
          <Button type="link" size="small" icon={<EyeOutlined />} onClick={() => onViewDetail(record)}>
            Chi tiết
          </Button>
          {record.status === ORDER_STATUS.PENDING && (
            <Popconfirm
              title="Hủy đơn này?"
              description="Chỉ hủy được khi đơn còn ở trạng thái chờ xử lý."
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

  const data = (orders || []).filter((order) => orderTypes.includes(order.orderType));

  return (
    <Table
      rowKey="id"
      loading={loading}
      columns={columns}
      dataSource={data}
      pagination={{ pageSize: 8, showSizeChanger: true, pageSizeOptions: ['8', '16', '32'] }}
      scroll={{ x: 980 }}
    />
  );
};

export default PurchaseOrderTable;
