import React, { useState } from 'react';
import { Typography, message, Alert } from 'antd';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '../../../../lib/api';
import SupplierIncomingOrdersTable from './SupplierIncomingOrdersTable';
import SellerOrderDetailDrawer from './SellerOrderDetailDrawer';
import AssignCarrierModal from './AssignCarrierModal';

const { Title, Paragraph } = Typography;

const QUERY_KEY = ['supplierIncomingOrders'];

const fetchSellerOrders = async () => {
  const res = await api.get('/product/api/v1/orders/mine/seller');
  return res.data?.result ?? [];
};

const SupplierOrderManagement = () => {
  const queryClient = useQueryClient();
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [detailLoading, setDetailLoading] = useState(false);
  const [selectedOrder, setSelectedOrder] = useState(null);
  const [assignModalOpen, setAssignModalOpen] = useState(false);
  const [orderToAssign, setOrderToAssign] = useState(null);
  const [actingId, setActingId] = useState(null);

  const { data: orders = [], isLoading } = useQuery({
    queryKey: QUERY_KEY,
    queryFn: fetchSellerOrders,
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

  return (
    <div>
      <Title level={4} style={{ marginTop: 0 }}>
        Đơn hàng gửi tới NCC
      </Title>
      <Paragraph type="secondary">
        Đơn từ nhà sản xuất đặt nguyên liệu. Khi đã chấp nhận, hãy gán đơn vị vận chuyển để giao hàng.
      </Paragraph>
      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 16 }}
        message="Luồng xử lý"
        description="Chờ xử lý → Chấp nhận / Từ chối → (nếu chấp nhận) Gán đơn vị vận chuyển → VC xác nhận đã giao."
      />

      <SupplierIncomingOrdersTable
        orders={orders}
        loading={isLoading}
        onViewDetail={openDetail}
        onAccept={(id) => acceptMutation.mutate(id)}
        onReject={(id) => rejectMutation.mutate(id)}
        onOpenAssignCarrier={(o) => {
          setOrderToAssign(o);
          setAssignModalOpen(true);
        }}
        actingId={actingId}
      />

      <SellerOrderDetailDrawer
        open={drawerOpen}
        loading={detailLoading}
        order={selectedOrder}
        onClose={() => {
          setDrawerOpen(false);
          setSelectedOrder(null);
        }}
      />

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

export default SupplierOrderManagement;
