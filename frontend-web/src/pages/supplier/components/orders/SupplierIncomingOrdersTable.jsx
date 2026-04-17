import React from 'react';
import { Table, Button, Space, Popconfirm, Typography } from 'antd';
import {
  EyeOutlined,
  CheckOutlined,
  CloseOutlined,
  CarOutlined,
} from '@ant-design/icons';
import OrderStatusTag from '../../../manufacture/components/orders/OrderStatusTag';
import { ORDER_STATUS, ORDER_TYPE } from '../../../manufacture/constants/tradeOrderConstants';

const { Text } = Typography;

const SupplierIncomingOrdersTable = ({
  orders,
  loading,
  onViewDetail,
  onAccept,
  onReject,
  onOpenAssignCarrier,
  actingId,
}) => {
  const columns = [
    {
      title: 'Mã đơn',
      dataIndex: 'orderCode',
      key: 'orderCode',
      render: (t) => <Text strong>{t}</Text>,
    },
    {
      title: 'Loại',
      dataIndex: 'orderType',
      key: 'orderType',
      width: 120,
      render: (t) => (t === ORDER_TYPE.MANUFACTURER_TO_SUPPLIER ? <Text>NSX → NCC</Text> : <Text>{t}</Text>),
    },
    {
      title: 'Người mua',
      dataIndex: 'buyerId',
      key: 'buyerId',
      ellipsis: true,
      render: (id) => <Text copyable={{ text: id }}>{id}</Text>,
    },
    {
      title: 'Dòng',
      key: 'lines',
      width: 72,
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
      width: 280,
      fixed: 'right',
      render: (_, record) => (
        <Space size="small" wrap>
          <Button type="link" size="small" icon={<EyeOutlined />} onClick={() => onViewDetail(record)}>
            Chi tiết
          </Button>
          {record.status === ORDER_STATUS.PENDING && (
            <>
              <Popconfirm title="Chấp nhận đơn này?" okText="Chấp nhận" cancelText="Không" onConfirm={() => onAccept(record.id)}>
                <Button type="link" size="small" icon={<CheckOutlined />} loading={actingId === record.id}>
                  Chấp nhận
                </Button>
              </Popconfirm>
              <Popconfirm title="Từ chối đơn?" okText="Từ chối" cancelText="Không" onConfirm={() => onReject(record.id)}>
                <Button type="link" size="small" danger icon={<CloseOutlined />} loading={actingId === record.id}>
                  Từ chối
                </Button>
              </Popconfirm>
            </>
          )}
          {record.status === ORDER_STATUS.ACCEPTED && (
            <Button type="link" size="small" icon={<CarOutlined />} onClick={() => onOpenAssignCarrier(record)}>
              Gán VC
            </Button>
          )}
        </Space>
      ),
    },
  ];

  return (
    <Table
      rowKey="id"
      loading={loading}
      columns={columns}
      dataSource={orders || []}
      scroll={{ x: 960 }}
      pagination={{ pageSize: 8, showSizeChanger: true, pageSizeOptions: ['8', '16', '24'] }}
    />
  );
};

export default SupplierIncomingOrdersTable;
