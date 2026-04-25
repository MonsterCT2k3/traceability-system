import React, { useState } from 'react';
import { Alert, Button, Descriptions, Drawer, Empty, message, Popconfirm, Space, Spin, Table, Typography } from 'antd';
import { CheckOutlined, CloseOutlined, EyeOutlined, CarOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import api from '../../../../lib/api';
import UserDirectoryDisplay from '../../../../components/trade-order/UserDirectoryDisplay';
import OrderStatusTag from './OrderStatusTag';
import { ORDER_STATUS, ORDER_TYPE } from '../../constants/tradeOrderConstants';
import AssignCarrierModal from '../../../supplier/components/orders/AssignCarrierModal';

const { Title, Paragraph, Text } = Typography;
const QUERY_KEY = ['manufactureRetailIncomingOrders'];

const fetchIncomingRetailOrders = async () => {
  const res = await api.get('/product/api/v1/orders/mine/seller');
  const all = res.data?.result ?? [];
  return all.filter((o) => o.orderType === ORDER_TYPE.RETAILER_TO_MANUFACTURER);
};

const lineColumns = [
  { title: '#', dataIndex: 'lineIndex', key: 'lineIndex', width: 56 },
  { title: 'Sản phẩm ID', dataIndex: 'productId', key: 'productId', ellipsis: true },
  {
    title: 'Số thùng',
    dataIndex: 'quantityCartons',
    key: 'quantityCartons',
    width: 120,
    render: (v) => v ?? 0,
  },
];

const ManufactureRetailOrdersManagement = () => {
  const queryClient = useQueryClient();
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [detailLoading, setDetailLoading] = useState(false);
  const [selectedOrder, setSelectedOrder] = useState(null);
  const [assignModalOpen, setAssignModalOpen] = useState(false);
  const [orderToAssign, setOrderToAssign] = useState(null);
  const [actingId, setActingId] = useState(null);

  const { data: orders = [], isLoading } = useQuery({
    queryKey: QUERY_KEY,
    queryFn: fetchIncomingRetailOrders,
  });

  const invalidate = () => queryClient.invalidateQueries({ queryKey: QUERY_KEY });

  const acceptMutation = useMutation({
    mutationFn: (orderId) => api.post(`/product/api/v1/orders/${orderId}/accept`),
    onMutate: (id) => setActingId(id),
    onSuccess: () => {
      message.success('Đã chấp nhận đơn');
      invalidate();
    },
    onError: (e) => message.error(e.response?.data?.message || 'Không chấp nhận được'),
    onSettled: () => setActingId(null),
  });

  const rejectMutation = useMutation({
    mutationFn: (orderId) => api.post(`/product/api/v1/orders/${orderId}/reject`),
    onMutate: (id) => setActingId(id),
    onSuccess: () => {
      message.success('Đã từ chối đơn');
      invalidate();
    },
    onError: (e) => message.error(e.response?.data?.message || 'Không từ chối được'),
    onSettled: () => setActingId(null),
  });

  const assignMutation = useMutation({
    mutationFn: ({ orderId, carrierId }) =>
      api.post(`/product/api/v1/orders/${orderId}/assign-carrier`, { carrierId }),
    onSuccess: () => {
      message.success('Đã gán đơn vị vận chuyển');
      setAssignModalOpen(false);
      setOrderToAssign(null);
      invalidate();
    },
    onError: (e) => message.error(e.response?.data?.message || 'Không gán được VC'),
  });

  const openDetail = async (order) => {
    setDrawerOpen(true);
    setDetailLoading(true);
    setSelectedOrder(null);
    try {
      const res = await api.get(`/product/api/v1/orders/${order.id}`);
      setSelectedOrder(res.data?.result ?? order);
    } catch (e) {
      message.error(e.response?.data?.message || 'Không tải chi tiết');
      setSelectedOrder(order);
    } finally {
      setDetailLoading(false);
    }
  };

  const columns = [
    {
      title: 'Mã đơn',
      dataIndex: 'orderCode',
      key: 'orderCode',
      render: (t) => <Text strong>{t}</Text>,
    },
    {
      title: 'Người đặt (Retailer)',
      dataIndex: 'buyerId',
      key: 'buyerId',
      render: (id) => <UserDirectoryDisplay userId={id} />,
    },
    {
      title: 'Số dòng',
      key: 'lineCount',
      width: 90,
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
      width: 170,
      render: (d) => (d ? new Date(d).toLocaleString('vi-VN') : '—'),
    },
    {
      title: '',
      key: 'actions',
      width: 300,
      fixed: 'right',
      render: (_, record) => (
        <Space size="small" wrap>
          <Button type="link" size="small" icon={<EyeOutlined />} onClick={() => openDetail(record)}>
            Chi tiết
          </Button>
          {record.status === ORDER_STATUS.PENDING && (
            <>
              <Popconfirm title="Chấp nhận đơn này?" okText="Chấp nhận" cancelText="Không" onConfirm={() => acceptMutation.mutate(record.id)}>
                <Button type="link" size="small" icon={<CheckOutlined />} loading={actingId === record.id}>
                  Chấp nhận
                </Button>
              </Popconfirm>
              <Popconfirm title="Từ chối đơn?" okText="Từ chối" cancelText="Không" onConfirm={() => rejectMutation.mutate(record.id)}>
                <Button type="link" size="small" danger icon={<CloseOutlined />} loading={actingId === record.id}>
                  Từ chối
                </Button>
              </Popconfirm>
            </>
          )}
          {record.status === ORDER_STATUS.ACCEPTED && (
            <Button
              type="link"
              size="small"
              icon={<CarOutlined />}
              onClick={() => {
                setOrderToAssign(record);
                setAssignModalOpen(true);
              }}
            >
              Gán VC
            </Button>
          )}
        </Space>
      ),
    },
  ];

  return (
    <div>
      <Title level={4} style={{ marginTop: 0 }}>
        Đơn hàng từ Retailer
      </Title>
      <Paragraph type="secondary">
        Quản lý đơn loại <strong>RETAILER_TO_MANUFACTURER</strong>: chấp nhận/từ chối đơn và gán đơn vị vận chuyển.
      </Paragraph>
      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 16 }}
        message="Luồng xử lý"
        description="Chờ xử lý → Chấp nhận / Từ chối → (nếu chấp nhận) Gán đơn vị vận chuyển → VC xác nhận đã giao."
      />

      <Table
        rowKey="id"
        loading={isLoading}
        columns={columns}
        dataSource={orders}
        scroll={{ x: 1080 }}
        pagination={{ pageSize: 8, showSizeChanger: true, pageSizeOptions: ['8', '16', '24'] }}
      />

      <Drawer
        title={`Đơn ${selectedOrder?.orderCode || ''}`}
        placement="right"
        width={680}
        open={drawerOpen}
        onClose={() => {
          setDrawerOpen(false);
          setSelectedOrder(null);
        }}
      >
        {detailLoading && !selectedOrder ? (
          <div style={{ padding: 48, textAlign: 'center' }}>
            <Spin size="large" />
          </div>
        ) : !selectedOrder ? (
          <Empty description="Không có dữ liệu" />
        ) : (
          <>
            <Descriptions bordered column={1} size="small" style={{ marginBottom: 16 }}>
              <Descriptions.Item label="Mã đơn">{selectedOrder.orderCode}</Descriptions.Item>
              <Descriptions.Item label="Loại">{selectedOrder.orderType}</Descriptions.Item>
              <Descriptions.Item label="Trạng thái">
                <OrderStatusTag status={selectedOrder.status} />
              </Descriptions.Item>
              <Descriptions.Item label="Người đặt (Retailer)">
                <UserDirectoryDisplay userId={selectedOrder.buyerId} />
              </Descriptions.Item>
              <Descriptions.Item label="Đơn vị vận chuyển">
                {selectedOrder.carrierId ? <UserDirectoryDisplay userId={selectedOrder.carrierId} /> : '—'}
              </Descriptions.Item>
              <Descriptions.Item label="Ghi chú">{selectedOrder.note || '—'}</Descriptions.Item>
            </Descriptions>
            <Typography.Title level={5}>Dòng đặt hàng</Typography.Title>
            <Table
              size="small"
              rowKey={(r) => r.id || `${r.lineIndex}`}
              columns={lineColumns}
              dataSource={[...(selectedOrder.lines || [])].sort((a, b) => (a.lineIndex ?? 0) - (b.lineIndex ?? 0))}
              pagination={false}
            />
          </>
        )}
      </Drawer>

      <AssignCarrierModal
        open={assignModalOpen}
        order={orderToAssign}
        submitting={assignMutation.isPending}
        onCancel={() => {
          setAssignModalOpen(false);
          setOrderToAssign(null);
        }}
        onSubmit={async ({ carrierId }) => {
          if (!orderToAssign?.id) return;
          await assignMutation.mutateAsync({ orderId: orderToAssign.id, carrierId });
        }}
      />
    </div>
  );
};

export default ManufactureRetailOrdersManagement;
