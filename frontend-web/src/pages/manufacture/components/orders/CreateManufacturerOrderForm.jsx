import React, { useEffect, useState } from 'react';
import { Alert, Button, Card, Form, Select, Spin, message } from 'antd';
import api from '../../../../lib/api';
import { ORDER_TYPE } from '../../constants/tradeOrderConstants';

const CreateManufacturerOrderForm = ({ submitting, onSubmit }) => {
  const [form] = Form.useForm();
  const [manufacturers, setManufacturers] = useState([]);
  const [pallets, setPallets] = useState([]);
  const [loading, setLoading] = useState(false);
  const sellerId = Form.useWatch('sellerId', form);

  useEffect(() => {
    api.get('/identity/api/v1/users/directory/manufacturers')
      .then((res) => setManufacturers(res.data?.result ?? []))
      .catch(() => setManufacturers([]));
  }, []);

  useEffect(() => {
    if (!sellerId) {
      setPallets([]);
      return;
    }
    setLoading(true);
    api.get(`/product/api/v1/products/pallets/by-owner/${encodeURIComponent(sellerId)}`)
      .then((res) => setPallets(res.data?.result ?? []))
      .catch(() => setPallets([]))
      .finally(() => setLoading(false));
  }, [sellerId]);

  const submit = (values) => {
    if (!(values.palletIds || []).length) {
      message.warning('Chọn ít nhất một pallet');
      return;
    }
    onSubmit({
      orderType: ORDER_TYPE.MANUFACTURER_TO_MANUFACTURER,
      sellerId: values.sellerId,
      lines: values.palletIds.map((targetPalletId) => ({ targetPalletId })),
    });
  };

  return (
    <Card bordered={false} style={{ maxWidth: 900 }}>
      <Alert type="info" showIcon message="Mua nguyên pallet từ nhà sản xuất khác để dùng làm đầu vào sản xuất." style={{ marginBottom: 16 }} />
      <Form form={form} layout="vertical" onFinish={submit}>
        <Form.Item name="sellerId" label="Nhà sản xuất bán" rules={[{ required: true }]}>
          <Select showSearch optionFilterProp="label" options={manufacturers.map((m) => ({
            value: m.id, label: m.fullName || m.username || m.id,
          }))} />
        </Form.Item>
        <Form.Item name="palletIds" label="Pallet" rules={[{ required: true }]}>
          <Select mode="multiple" loading={loading}
            notFoundContent={loading ? <Spin size="small" /> : 'Không có pallet AVAILABLE'}
            options={pallets.map((p) => ({ value: p.id, label: `${p.palletCode} - ${p.palletName}` }))} />
        </Form.Item>
        <Button type="primary" htmlType="submit" loading={submitting}>Gửi đơn mua pallet</Button>
      </Form>
    </Card>
  );
};

export default CreateManufacturerOrderForm;
