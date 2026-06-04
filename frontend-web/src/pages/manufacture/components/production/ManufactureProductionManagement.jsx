import React, { useMemo } from 'react';
import { Button, Card, Col, DatePicker, Divider, Form, Input, InputNumber, Row, Select, Space, Statistic, Typography, message } from 'antd';
import dayjs from 'dayjs';
import { useMutation, useQuery } from '@tanstack/react-query';
import api from '../../../../lib/api';

const { Title, Text } = Typography;

const makeBatchNo = () => {
  const d = dayjs();
  return `BATCH-${d.format('YYYYMMDD')}-${Math.floor(Math.random() * 9000 + 1000)}`;
};

const ManufactureProductionManagement = () => {
  const [form] = Form.useForm();

  const { data: summary, isLoading: summaryLoading } = useQuery({
    queryKey: ['banknoteSerialSummary'],
    queryFn: async () => {
      const res = await api.get('/product/api/v1/banknote-serials/summary');
      return res.data?.result ?? { totalSerials: 0, mySerials: 0, usedSerials: 0, availableSerials: 0 };
    },
  });

  const { data: products = [], isLoading: productsLoading } = useQuery({
    queryKey: ['manufacturerProducts'],
    queryFn: async () => {
      const res = await api.get('/catalog/api/v1/products/my');
      return res.data?.result ?? [];
    },
  });

  const { data: myRawBatches = [], isLoading: rawLoading } = useQuery({
    queryKey: ['manufacturerMyRawBatches'],
    queryFn: async () => {
      const res = await api.get('/product/api/v1/raw-batches/my');
      return res.data?.result ?? [];
    },
  });

  const { data: myPallets = [], isLoading: palletLoading } = useQuery({
    queryKey: ['manufacturerMyPallets'],
    queryFn: async () => {
      const res = await api.get('/product/api/v1/products/pallets/my');
      return res.data?.result ?? [];
    },
  });

  const { data: profile } = useQuery({
    queryKey: ['userProfile'],
    queryFn: async () => {
      const response = await api.get('/identity/api/v1/users/profile');
      return response.data?.result ?? null;
    },
    staleTime: 5 * 60 * 1000,
  });

  const rawOptions = useMemo(
    () =>
      (myRawBatches || [])
        .filter((rb) => rb?.id && rb?.batchIdHex)
        .map((rb) => ({
          value: rb.id,
          label: `${rb.rawBatchCode} · ${rb.materialName} · ${rb.quantity} ${rb.unit}`,
        })),
    [myRawBatches],
  );

  const inputOptions = useMemo(() => [
    {
      label: 'Lo nguyen lieu',
      options: rawOptions.map((option) => ({ ...option, value: `RAW_BATCH:${option.value}` })),
    },
    {
      label: 'Pallet co the dung lam dau vao',
      options: (myPallets || [])
        .filter((p) => p.inputStatus === 'AVAILABLE')
        .map((p) => ({ value: `PALLET:${p.id}`, label: `${p.palletCode} - ${p.palletName}` })),
    },
  ], [rawOptions, myPallets]);

  const createBatchMutation = useMutation({
    mutationFn: async (values) => {
      const payload = {
        palletName: values.palletName?.trim(),
        batchNo: values.batchNo?.trim(),
        manufacturedAt: values.manufacturedAt ? values.manufacturedAt.format('YYYY-MM-DD') : '',
        expiryAt: values.expiryAt ? values.expiryAt.format('YYYY-MM-DD') : '',
        quantity: String(values.quantity),
        unit: values.unit?.trim(),
        packagingType: values.packagingType?.trim(),
        processingMethod: values.processingMethod?.trim(),
        location: values.location?.trim(),
        note: values.note?.trim() || '',
        inputs: (values.inputs || []).map((value) => {
          const separator = value.indexOf(':');
          return { type: value.slice(0, separator), id: value.slice(separator + 1) };
        }),
      };
      return api.post(`/product/api/v1/products/${values.productId}/pallets/anchor`, payload);
    },
    onSuccess: (res) => {
      const result = res?.data?.result;
      message.success(`Tạo lô sản xuất thành công: ${result?.palletCode || 'OK'}`);
      form.resetFields();
    },
    onError: (err) => {
      message.error(err?.response?.data?.message || 'Không tạo được lô sản xuất');
    },
  });

  const onCreateBatch = (values) => {
    createBatchMutation.mutate(values);
  };

  const fillLocationFromProfile = () => {
    if (profile?.location) {
      form.setFieldValue('location', profile.location);
      message.success('Đã lấy vị trí gốc từ tài khoản');
      return;
    }
    message.warning('Bạn chưa có vị trí gốc trong hồ sơ tài khoản');
  };

  return (
    <div>
      <Title level={4} style={{ marginBottom: 4 }}>Sản xuất</Title>
      <Text type="secondary">Xem số lượng seri hiện có và tạo lô sản xuất.</Text>
      <Divider />

      <Row gutter={[16, 16]}>
        <Col xs={24} md={8}>
          <Card loading={summaryLoading}>
            <Statistic title="Seri khả dụng để đóng gói" value={summary?.availableSerials ?? summary?.mySerials ?? 0} />
          </Card>
        </Col>
      </Row>

      <Card style={{ marginTop: 16 }}>
        <Form
          form={form}
          layout="vertical"
          onFinish={onCreateBatch}
          initialValues={{
            unit: 'kg',
            packagingType: 'THUNG',
            processingMethod: 'STANDARD',
            manufacturedAt: dayjs(),
            batchNo: makeBatchNo(),
          }}
        >
          <Row gutter={16}>
            <Col xs={24} md={12}>
              <Form.Item name="productId" label="Sản phẩm" rules={[{ required: true, message: 'Chọn sản phẩm' }]}>
                <Select
                  showSearch
                  placeholder="Chọn sản phẩm catalog"
                  loading={productsLoading}
                  options={(products || []).map((p) => ({
                    value: p.id,
                    label: `${p.name}${p.price != null ? ` (${new Intl.NumberFormat('vi-VN').format(p.price)} đ)` : ''}`,
                  }))}
                  optionFilterProp="label"
                />
              </Form.Item>
            </Col>
            <Col xs={24} md={12}>
              <Form.Item name="inputs" label="Đầu vào sản xuất thuộc sở hữu của bạn" rules={[{ required: true, message: 'Chọn ít nhất 1 đầu vào' }]}>
                <Select
                  mode="multiple"
                  placeholder="Chọn lô nguyên liệu hoặc pallet"
                  loading={rawLoading || palletLoading}
                  options={inputOptions}
                  optionFilterProp="label"
                />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col xs={24} md={8}>
              <Form.Item name="palletName" label="Tên lô/pallet" rules={[{ required: true, message: 'Nhập tên lô' }]}>
                <Input placeholder="VD: Lô sữa tiệt trùng tháng 04" />
              </Form.Item>
            </Col>
            <Col xs={24} md={12}>
              <Form.Item
                name="batchNo"
                label="Mã batch nội bộ"
                extra="Mã quản trị nội bộ để truy vết nhanh theo ca/ngày. Đã tự sinh sẵn, có thể sửa."
                rules={[{ required: true, message: 'Nhập batchNo' }]}
              >
                <Input placeholder="VD: BAT-2026-0001" />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col xs={24} md={6}>
              <Form.Item name="manufacturedAt" label="Ngày sản xuất" rules={[{ required: true, message: 'Chọn ngày sản xuất' }]}>
                <DatePicker style={{ width: '100%' }} format="YYYY-MM-DD" />
              </Form.Item>
            </Col>
            <Col xs={24} md={6}>
              <Form.Item name="expiryAt" label="Hạn dùng">
                <DatePicker style={{ width: '100%' }} format="YYYY-MM-DD" />
              </Form.Item>
            </Col>
            <Col xs={24} md={6}>
              <Form.Item name="quantity" label="Số lượng" rules={[{ required: true, message: 'Nhập số lượng' }]}>
                <InputNumber min={0.0001} style={{ width: '100%' }} placeholder="VD: 1000" />
              </Form.Item>
            </Col>
            <Col xs={24} md={6}>
              <Form.Item name="unit" label="Đơn vị" rules={[{ required: true, message: 'Nhập đơn vị' }]}>
                <Select
                  placeholder="Chọn đơn vị"
                  options={[
                    { value: 'kg', label: 'Kilogram (kg)' },
                    { value: 'g', label: 'Gram (g)' },
                    { value: 'litre', label: 'Lít (litre)' },
                    { value: 'ml', label: 'Mililit (ml)' },
                    { value: 'box', label: 'Thùng/Hộp (box)' },
                    { value: 'pack', label: 'Gói (pack)' },
                    { value: 'piece', label: 'Cái (piece)' },
                  ]}
                />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col xs={24} md={12}>
              <Form.Item name="packagingType" label="Kiểu đóng gói">
                <Input placeholder="VD: THUNG, HOP, CHAI" />
              </Form.Item>
            </Col>
            <Col xs={24} md={12}>
              <Form.Item name="processingMethod" label="Phương pháp chế biến">
                <Input placeholder="VD: PASTEURIZED / STANDARD" />
              </Form.Item>
            </Col>
          </Row>

          <Form.Item
            name="location"
            label="Địa điểm sản xuất"
            extra="Nên dùng vị trí gốc của tài khoản để chuẩn hóa dữ liệu."
            rules={[{ required: true, message: 'Nhập địa điểm sản xuất' }]}
          >
            <Input
              placeholder="VD: KCN A - Hà Nội"
              addonAfter={(
                <Button type="link" size="small" onClick={fillLocationFromProfile} style={{ margin: '-4px -11px' }}>
                  Lấy vị trí gốc
                </Button>
              )}
            />
          </Form.Item>
          <Form.Item name="note" label="Ghi chú">
            <Input.TextArea rows={3} placeholder="Ghi chú cho lô sản xuất..." />
          </Form.Item>

          <Space>
            <Button type="primary" htmlType="submit" loading={createBatchMutation.isPending}>
              Tạo lô sản xuất
            </Button>
            <Button onClick={() => form.resetFields()}>
              Làm mới
            </Button>
          </Space>
        </Form>
      </Card>
    </div>
  );
};

export default ManufactureProductionManagement;
