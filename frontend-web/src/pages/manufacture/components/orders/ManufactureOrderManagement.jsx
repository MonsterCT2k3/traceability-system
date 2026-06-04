import React, { useState } from 'react';
import { Typography, Tabs, message, Alert } from 'antd';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '../../../../lib/api';
import PurchaseOrderTable from './PurchaseOrderTable';
import CreatePurchaseOrderForm from './CreatePurchaseOrderForm';
import CreateManufacturerOrderForm from './CreateManufacturerOrderForm';
import OrderDetailDrawer from './OrderDetailDrawer';
import { ORDER_TYPE } from '../../constants/tradeOrderConstants';

const { Title, Paragraph } = Typography;

const ORDERS_QUERY_KEY = ['manufactureOrdersBuyer'];

const fetchBuyerOrders = async () => {
  const res = await api.get('/trade/api/v1/orders/mine/buyer');
  return res.data?.result ?? [];
};

const ManufactureOrderManagement = () => {
  const queryClient = useQueryClient();
  const [activeTab, setActiveTab] = useState('list');
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [selectedOrder, setSelectedOrder] = useState(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [cancellingId, setCancellingId] = useState(null);
  const [createFormKey, setCreateFormKey] = useState(0);

  const { data: orders = [], isLoading } = useQuery({
    queryKey: ORDERS_QUERY_KEY,
    queryFn: fetchBuyerOrders,
  });

  const createMutation = useMutation({
    mutationFn: async (body) => {
      const res = await api.post('/trade/api/v1/orders', body);
      return res.data?.result;
    },
    onSuccess: () => {
      message.success('Đã tạo đơn đặt hàng');
      queryClient.invalidateQueries({ queryKey: ORDERS_QUERY_KEY });
      setCreateFormKey((k) => k + 1);
      setActiveTab('list');
    },
    onError: (err) => {
      message.error(err.response?.data?.message || err.message || 'Không tạo được đơn');
    },
  });

  const cancelMutation = useMutation({
    mutationFn: async (orderId) => {
      setCancellingId(orderId);
      const res = await api.post(`/trade/api/v1/orders/${orderId}/cancel`);
      return res.data?.result;
    },
    onSuccess: () => {
      message.success('Đã hủy đơn');
      queryClient.invalidateQueries({ queryKey: ORDERS_QUERY_KEY });
      setDrawerOpen(false);
      setSelectedOrder(null);
    },
    onError: (err) => {
      message.error(err.response?.data?.message || err.message || 'Không hủy được đơn');
    },
    onSettled: () => setCancellingId(null),
  });

  const openDetail = async (order) => {
    setDrawerOpen(true);
    setDetailLoading(true);
    setSelectedOrder(null);
    try {
      const res = await api.get(`/trade/api/v1/orders/${order.id}`);
      setSelectedOrder(res.data?.result ?? order);
    } catch (err) {
      message.error(err.response?.data?.message || 'Không tải chi tiết đơn');
      setSelectedOrder(order);
    } finally {
      setDetailLoading(false);
    }
  };

  const tabItems = [
    {
      key: 'list',
      label: 'Đơn đặt nguyên liệu',
      children: (
        <>
          <Alert
            type="info"
            showIcon
            style={{ marginBottom: 16 }}
            message="Đơn từ nhà sản xuất tới nhà cung cấp"
            description="Sau khi gửi, NCC sẽ chấp nhận / từ chối. Bạn có thể hủy khi đơn còn ở trạng thái chờ xử lý."
          />
          <PurchaseOrderTable
            orders={orders}
            orderTypes={[ORDER_TYPE.MANUFACTURER_TO_SUPPLIER, ORDER_TYPE.MANUFACTURER_TO_MANUFACTURER]}
            loading={isLoading}
            onViewDetail={openDetail}
            onCancel={(id) => cancelMutation.mutate(id)}
            cancellingId={cancellingId}
          />
        </>
      ),
    },
    {
      key: 'create',
      label: 'Tạo đơn mua từ NCC',
      children: (
        <CreatePurchaseOrderForm
          key={createFormKey}
          submitting={createMutation.isPending}
          onSubmit={(payload) => createMutation.mutate(payload)}
        />
      ),
    },
    {
      key: 'create-m2m',
      label: 'Tạo đơn mua pallet từ NSX',
      children: (
        <CreateManufacturerOrderForm
          key={`m2m-${createFormKey}`}
          submitting={createMutation.isPending}
          onSubmit={(payload) => createMutation.mutate(payload)}
        />
      ),
    },
  ];

  return (
    <div>
      <Title level={4} style={{ marginTop: 0 }}>
        Đặt hàng nguyên liệu (NCC)
      </Title>
      <Paragraph type="secondary">
        Quản lý đơn mua nguyên liệu từ nhà cung cấp — loại đơn <strong>MANUFACTURER_TO_SUPPLIER</strong>.
      </Paragraph>

      <Tabs activeKey={activeTab} onChange={setActiveTab} items={tabItems} />

      <OrderDetailDrawer
        open={drawerOpen}
        loading={detailLoading}
        order={selectedOrder}
        onClose={() => {
          setDrawerOpen(false);
          setSelectedOrder(null);
        }}
      />
    </div>
  );
};

export default ManufactureOrderManagement;
