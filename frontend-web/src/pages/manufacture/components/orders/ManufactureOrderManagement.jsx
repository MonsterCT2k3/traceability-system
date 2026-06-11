import React, { useState } from 'react';
import { Button, Card, Drawer, Space, Typography, message } from 'antd';
import { PlusOutlined, ShoppingCartOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import api from '../../../../lib/api';
import { ORDER_TYPE } from '../../constants/tradeOrderConstants';
import CreateMaterialOrderForm from './CreateMaterialOrderForm';
import OrderDetailDrawer from './OrderDetailDrawer';
import PurchaseOrderTable from './PurchaseOrderTable';

const { Title, Paragraph } = Typography;

const ORDERS_QUERY_KEY = ['manufactureOrdersBuyer'];

const fetchBuyerOrders = async () => {
  const res = await api.get('/trade/api/v1/orders/mine/buyer');
  return res.data?.result ?? [];
};

const ManufactureOrderManagement = () => {
  const queryClient = useQueryClient();
  const [createOpen, setCreateOpen] = useState(false);
  const [detailOpen, setDetailOpen] = useState(false);
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
      setCreateFormKey((key) => key + 1);
      setCreateOpen(false);
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
      setDetailOpen(false);
      setSelectedOrder(null);
    },
    onError: (err) => {
      message.error(err.response?.data?.message || err.message || 'Không hủy được đơn');
    },
    onSettled: () => setCancellingId(null),
  });

  const openDetail = async (order) => {
    setDetailOpen(true);
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

  return (
    <div>
      <Card
        bordered={false}
        style={{
          marginBottom: 18,
          background: 'linear-gradient(135deg, #f8fbff 0%, #eef7f2 100%)',
          border: '1px solid #e6f0ec',
        }}
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', gap: 16, alignItems: 'center', flexWrap: 'wrap' }}>
          <div>
            <Space align="center" style={{ marginBottom: 8 }}>
              <ShoppingCartOutlined style={{ fontSize: 24, color: '#1677ff' }} />
              <Title level={3} style={{ margin: 0 }}>Đặt hàng nguyên liệu</Title>
            </Space>
            <Paragraph type="secondary" style={{ margin: 0, maxWidth: 780 }}>
              Tạo một đơn đặt hàng duy nhất, sau đó chọn nguồn mua là nhà cung cấp hoặc nhà sản xuất trong form.
            </Paragraph>
          </div>
          <Button type="primary" size="large" icon={<PlusOutlined />} onClick={() => setCreateOpen(true)}>
            Tạo đơn mới
          </Button>
        </div>
      </Card>

      <Card bordered={false} title="Danh sách đơn đặt hàng">
        <PurchaseOrderTable
          orders={orders}
          orderTypes={[ORDER_TYPE.MANUFACTURER_TO_SUPPLIER, ORDER_TYPE.MANUFACTURER_TO_MANUFACTURER]}
          loading={isLoading}
          onViewDetail={openDetail}
          onCancel={(id) => cancelMutation.mutate(id)}
          cancellingId={cancellingId}
        />
      </Card>

      <Drawer
        title="Tạo đơn đặt hàng"
        open={createOpen}
        onClose={() => setCreateOpen(false)}
        width={760}
        destroyOnHidden
      >
        <CreateMaterialOrderForm
          key={createFormKey}
          submitting={createMutation.isPending}
          onSubmit={(payload) => createMutation.mutate(payload)}
        />
      </Drawer>

      <OrderDetailDrawer
        open={detailOpen}
        loading={detailLoading}
        order={selectedOrder}
        onClose={() => {
          setDetailOpen(false);
          setSelectedOrder(null);
        }}
      />
    </div>
  );
};

export default ManufactureOrderManagement;
