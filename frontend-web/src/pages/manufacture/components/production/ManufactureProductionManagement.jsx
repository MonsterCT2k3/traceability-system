import React, { useEffect, useMemo, useState } from 'react';
import { Button, Card, Col, DatePicker, Divider, Form, Input, InputNumber, Row, Select, Space, Statistic, Typography, message } from 'antd';
import dayjs from 'dayjs';
import { useMutation, useQuery } from '@tanstack/react-query';
import QRCode from 'qrcode';
import JSZip from 'jszip';
import api from '../../../../lib/api';

const { Title, Text } = Typography;

const makeBatchNo = () => {
  const d = dayjs();
  return `BATCH-${d.format('YYYYMMDD')}-${Math.floor(Math.random() * 9000 + 1000)}`;
};

const buildLabelImage = async ({ cartonCode, unitSerial, qrDataUrl }) => {
  if (!qrDataUrl) return null;
  const canvas = document.createElement('canvas');
  canvas.width = 720;
  canvas.height = 420;
  const ctx = canvas.getContext('2d');
  if (!ctx) return null;

  // Background
  ctx.fillStyle = '#ffffff';
  ctx.fillRect(0, 0, canvas.width, canvas.height);
  ctx.strokeStyle = '#d9d9d9';
  ctx.lineWidth = 2;
  ctx.strokeRect(8, 8, canvas.width - 16, canvas.height - 16);

  // Title
  ctx.fillStyle = '#111111';
  ctx.font = 'bold 30px Arial';
  ctx.fillText('TEM TRUY XUAT SAN PHAM', 24, 52);

  // Carton + serial text
  ctx.font = 'bold 26px Arial';
  ctx.fillText(`Thung: ${cartonCode}`, 24, 110);
  ctx.fillText(`Seri: ${unitSerial}`, 24, 162);
  ctx.font = '20px Arial';
  ctx.fillStyle = '#444';
  ctx.fillText('QR nay ung voi dung seri in ben tren', 24, 206);

  // QR image
  const qrImg = new Image();
  await new Promise((resolve, reject) => {
    qrImg.onload = resolve;
    qrImg.onerror = reject;
    qrImg.src = qrDataUrl;
  });
  ctx.drawImage(qrImg, 470, 80, 220, 220);

  return canvas.toDataURL('image/png');
};

const buildCartonOnlyLabelImage = ({ cartonCode }) => {
  const canvas = document.createElement('canvas');
  canvas.width = 720;
  canvas.height = 420;
  const ctx = canvas.getContext('2d');
  if (!ctx) return null;

  ctx.fillStyle = '#ffffff';
  ctx.fillRect(0, 0, canvas.width, canvas.height);
  ctx.strokeStyle = '#d9d9d9';
  ctx.lineWidth = 2;
  ctx.strokeRect(8, 8, canvas.width - 16, canvas.height - 16);

  ctx.fillStyle = '#111111';
  ctx.font = 'bold 36px Arial';
  ctx.fillText('TEM MA THUNG', 24, 64);
  ctx.font = 'bold 34px Arial';
  ctx.fillText(cartonCode, 24, 170);
  ctx.font = '22px Arial';
  ctx.fillStyle = '#444';
  ctx.fillText('Dung de dan nhan nhan dien thung dong goi', 24, 230);

  return canvas.toDataURL('image/png');
};

const dataUrlToUint8Array = (dataUrl) => {
  const base64 = dataUrl.split(',')[1] || '';
  const binary = window.atob(base64);
  const len = binary.length;
  const bytes = new Uint8Array(len);
  for (let i = 0; i < len; i += 1) {
    bytes[i] = binary.charCodeAt(i);
  }
  return bytes;
};

const downloadCartonLabelsZip = async ({ cartonCode, units }) => {
  const zip = new JSZip();
  const cartonLabelDataUrl = buildCartonOnlyLabelImage({ cartonCode });
  if (cartonLabelDataUrl) {
    zip.file(`00-carton-${cartonCode}.png`, dataUrlToUint8Array(cartonLabelDataUrl));
  }
  for (const u of units || []) {
    const unitSerial = u?.unitSerial;
    if (!unitSerial) continue;
    const qrDataUrl = await QRCode.toDataURL(unitSerial, {
      errorCorrectionLevel: 'M',
      margin: 1,
      width: 180,
    });
    const labelDataUrl = await buildLabelImage({ cartonCode, unitSerial, qrDataUrl });
    if (!labelDataUrl) continue;
    zip.file(`label-${cartonCode}-${unitSerial}.png`, dataUrlToUint8Array(labelDataUrl));
  }
  const blob = await zip.generateAsync({ type: 'blob' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `labels-${cartonCode}.zip`;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
};

const SerialQrCard = ({ cartonCode, unitSerial }) => {
  const [qrDataUrl, setQrDataUrl] = useState('');
  const [loading, setLoading] = useState(false);
  const [downloading, setDownloading] = useState(false);

  useEffect(() => {
    let cancelled = false;
    const run = async () => {
      setLoading(true);
      try {
        const dataUrl = await QRCode.toDataURL(unitSerial, {
          errorCorrectionLevel: 'M',
          margin: 1,
          width: 180,
        });
        if (!cancelled) setQrDataUrl(dataUrl);
      } catch (_) {
        if (!cancelled) setQrDataUrl('');
      } finally {
        if (!cancelled) setLoading(false);
      }
    };
    run();
    return () => {
      cancelled = true;
    };
  }, [unitSerial]);

  const downloadLabel = async () => {
    if (!qrDataUrl) return;
    try {
      setDownloading(true);
      const labelDataUrl = await buildLabelImage({ cartonCode, unitSerial, qrDataUrl });
      if (!labelDataUrl) return;
      const a = document.createElement('a');
      a.href = labelDataUrl;
      a.download = `label-${cartonCode}-${unitSerial}.png`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
    } finally {
      setDownloading(false);
    }
  };

  return (
    <Card size="small" style={{ width: 210 }}>
      <div style={{ textAlign: 'center' }}>
        {qrDataUrl ? (
          <img
            src={qrDataUrl}
            alt={`QR-${unitSerial}`}
            style={{ width: 160, height: 160, objectFit: 'contain' }}
          />
        ) : (
          <div style={{ height: 160, display: 'grid', placeItems: 'center' }}>
            <Text type="secondary">{loading ? 'Đang tạo QR...' : 'Không tạo được QR'}</Text>
          </div>
        )}
      </div>
      <div style={{ marginTop: 8 }}>
        <Text strong>{unitSerial}</Text>
      </div>
      <div style={{ marginTop: 4 }}>
        <Text type="secondary">{cartonCode}</Text>
      </div>
      <div style={{ marginTop: 8 }}>
        <Button size="small" onClick={downloadLabel} disabled={!qrDataUrl || downloading}>
          {downloading ? 'Đang tạo tem...' : 'Tải tem (thùng + seri + QR)'}
        </Button>
      </div>
    </Card>
  );
};

const ManufactureProductionManagement = () => {
  const [tab, setTab] = useState('create');
  const [form] = Form.useForm();
  const [packingForm] = Form.useForm();
  const [packingResult, setPackingResult] = useState(null);

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
      const res = await api.get('/product/api/v1/products');
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

  const { data: profile } = useQuery({
    queryKey: ['userProfile'],
    queryFn: async () => {
      const response = await api.get('/identity/api/v1/users/profile');
      return response.data?.result ?? null;
    },
    staleTime: 5 * 60 * 1000,
  });

  const { data: myPallets = [], isLoading: myPalletsLoading } = useQuery({
    queryKey: ['manufacturerMyPallets'],
    queryFn: async () => {
      const res = await api.get('/product/api/v1/products/pallets/my');
      return res.data?.result ?? [];
    },
  });

  const rawOptions = useMemo(
    () =>
      (myRawBatches || [])
        .filter((rb) => rb?.batchIdHex)
        .map((rb) => ({
          value: rb.batchIdHex,
          label: `${rb.rawBatchCode} · ${rb.materialName} · ${rb.quantity} ${rb.unit}`,
        })),
    [myRawBatches],
  );

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
        parentRawBatchIdHexes: values.parentRawBatchIdHexes || [],
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

  const packingMutation = useMutation({
    mutationFn: async (values) => {
      if (!values.palletId) {
        throw new Error('Vui lòng chọn lô sản xuất');
      }
      if (!values.cartonCount || !values.unitsPerCarton) {
        throw new Error('Vui lòng nhập số thùng và số hộp/thùng');
      }
      const payload = {
        cartonCount: values.cartonCount,
        unitsPerCarton: values.unitsPerCarton,
        unitLabel: values.unitLabel?.trim() || undefined,
        note: values.note?.trim() || undefined,
      };
      const res = await api.post(`/product/api/v1/pallets/${values.palletId}/packing-bulk`, payload);
      return res.data?.result;
    },
    onSuccess: (result) => {
      setPackingResult(result || null);
      message.success(`Đóng gói thành công: ${result?.cartonsCreated ?? 0} carton, ${result?.unitsCreated ?? 0} unit`);
    },
    onError: (err) => {
      message.error(err?.response?.data?.message || err?.message || 'Đóng gói thất bại');
    },
  });

  const onSubmitPacking = (values) => {
    const needed = Number(values.cartonCount || 0) * Number(values.unitsPerCarton || 0);
    const available = Number(summary?.availableSerials ?? summary?.mySerials ?? 0);
    if (needed <= 0) {
      message.error('Số lượng cần đóng gói phải lớn hơn 0');
      return;
    }
    if (available < needed) {
      message.error(`Không đủ seri khả dụng. Cần ${needed}, hiện có ${available}`);
      return;
    }
    packingMutation.mutate(values);
  };

  const cartonCountWatch = Form.useWatch('cartonCount', packingForm);
  const unitsPerCartonWatch = Form.useWatch('unitsPerCarton', packingForm);
  const requiredUnits = Number(cartonCountWatch || 0) * Number(unitsPerCartonWatch || 0);
  const availableUnits = Number(summary?.availableSerials ?? summary?.mySerials ?? 0);
  const canPack = requiredUnits > 0 && availableUnits >= requiredUnits;

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
      <Text type="secondary">Xem số lượng seri hiện có, tạo lô sản xuất và thao tác đóng gói.</Text>
      <Divider />

      <Row gutter={[16, 16]}>
        <Col xs={24} md={12}>
          <Card loading={summaryLoading}>
            <Statistic title="Tổng seri trên hệ thống" value={summary?.totalSerials ?? 0} />
          </Card>
        </Col>
        <Col xs={24} md={12}>
          <Card loading={summaryLoading}>
            <Statistic title="Seri do bạn đã đăng ký" value={summary?.mySerials ?? 0} />
          </Card>
        </Col>
        <Col xs={24} md={12}>
          <Card loading={summaryLoading}>
            <Statistic title="Seri khả dụng để đóng gói" value={summary?.availableSerials ?? summary?.mySerials ?? 0} />
          </Card>
        </Col>
      </Row>

      <Card style={{ marginTop: 16 }}>
        <Space style={{ marginBottom: 8 }}>
          <Button type={tab === 'create' ? 'primary' : 'default'} onClick={() => setTab('create')}>
            Tạo lô sản xuất
          </Button>
          <Button type={tab === 'packing' ? 'primary' : 'default'} onClick={() => setTab('packing')}>
            Đóng gói
          </Button>
        </Space>
        <Divider />
        {tab === 'create' ? (
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
                <Form.Item name="parentRawBatchIdHexes" label="Raw batch nguồn (thuộc sở hữu của bạn)" rules={[{ required: true, message: 'Chọn ít nhất 1 raw batch' }]}>
                  <Select
                    mode="multiple"
                    placeholder="Chọn raw batch để làm parent"
                    loading={rawLoading}
                    options={rawOptions}
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
        ) : (
          <div>
            <Title level={5}>Đóng gói</Title>
            <Text type="secondary">
              Chọn lô sản xuất, nhập số thùng và số hộp mỗi thùng. Hệ thống tự cấp seri từ kho seri khả dụng của bạn.
            </Text>
            <Divider />
            <Form
              form={packingForm}
              layout="vertical"
              onFinish={onSubmitPacking}
            >
              <Form.Item
                name="palletId"
                label="Lô sản xuất"
                rules={[{ required: true, message: 'Chọn lô sản xuất' }]}
              >
                <Select
                  showSearch
                  placeholder="Chọn lô sản xuất cần đóng gói"
                  loading={myPalletsLoading}
                  options={(myPallets || []).map((p) => ({
                    value: p.id,
                    label: `${p.palletCode || p.id} · ${p.palletName || 'Không tên'} · ${p.batchNo || 'N/A'}`,
                  }))}
                  optionFilterProp="label"
                />
              </Form.Item>
              <Row gutter={16}>
                <Col xs={24} md={12}>
                  <Form.Item
                    name="cartonCount"
                    label="Số thùng (carton)"
                    rules={[{ required: true, message: 'Nhập số thùng' }]}
                  >
                    <InputNumber min={1} max={200} style={{ width: '100%' }} placeholder="VD: 10" />
                  </Form.Item>
                </Col>
                <Col xs={24} md={12}>
                  <Form.Item
                    name="unitsPerCarton"
                    label="Số hộp mỗi thùng"
                    rules={[{ required: true, message: 'Nhập số hộp/thùng' }]}
                  >
                    <InputNumber min={1} max={200} style={{ width: '100%' }} placeholder="VD: 24" />
                  </Form.Item>
                </Col>
              </Row>
              <Row gutter={16}>
                <Col xs={24} md={12}>
                  <Form.Item name="unitLabel" label="Nhãn đơn vị (tuỳ chọn)">
                    <Input placeholder="VD: chai / hộp / gói" />
                  </Form.Item>
                </Col>
                <Col xs={24} md={12}>
                  <Form.Item name="note" label="Ghi chú (tuỳ chọn)">
                    <Input placeholder="Ghi chú cho đợt đóng gói" />
                  </Form.Item>
                </Col>
              </Row>
              <Card size="small" style={{ marginBottom: 12 }}>
                <Text>
                  Cần tạo: <b>{requiredUnits}</b> unit · Seri khả dụng: <b>{availableUnits}</b>
                </Text>
                {!canPack && requiredUnits > 0 && (
                  <>
                    <br />
                    <Text type="danger">Không đủ seri khả dụng để đóng gói theo cấu hình hiện tại.</Text>
                  </>
                )}
              </Card>
              <Space>
                <Button type="primary" htmlType="submit" loading={packingMutation.isPending} disabled={!canPack}>
                  Đóng gói
                </Button>
                <Button onClick={() => { packingForm.resetFields(); setPackingResult(null); }}>
                  Làm mới
                </Button>
              </Space>
            </Form>

            {packingResult && (
              <Card style={{ marginTop: 16 }}>
                <Title level={5} style={{ marginBottom: 6 }}>Kết quả đóng gói</Title>
                <Text>
                  Pallet: <b>{packingResult.palletCode}</b> · Carton: <b>{packingResult.cartonsCreated}</b> · Unit: <b>{packingResult.unitsCreated}</b>
                </Text>
                <Divider />
                <div style={{ display: 'grid', gap: 10 }}>
                  {(packingResult.cartons || []).map((c) => (
                    <Card key={c.cartonCode} size="small">
                      <Text strong>{c.cartonCode}</Text>
                      <div style={{ marginTop: 6 }}>
                        <Text type="secondary">
                          {(c.units || []).length} unit: {(c.units || []).map((u) => u.unitSerial).join(', ')}
                        </Text>
                      </div>
                      <div style={{ marginTop: 10 }}>
                        <Button
                          size="small"
                          onClick={() => downloadCartonLabelsZip({ cartonCode: c.cartonCode, units: c.units || [] })}
                        >
                          Tải tất cả tem của thùng này (.zip)
                        </Button>
                      </div>
                      <Divider style={{ margin: '12px 0' }} />
                      <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap' }}>
                        {(c.units || []).map((u) => (
                          <SerialQrCard
                            key={`${c.cartonCode}-${u.unitSerial}`}
                            cartonCode={c.cartonCode}
                            unitSerial={u.unitSerial}
                          />
                        ))}
                      </div>
                    </Card>
                  ))}
                </div>
              </Card>
            )}
          </div>
        )}
      </Card>
    </div>
  );
};

export default ManufactureProductionManagement;
