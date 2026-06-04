import { useEffect, useState } from 'react';
import { Alert, Button, Card, Col, Divider, Form, Input, InputNumber, Row, Select, Space, Typography, message } from 'antd';
import { useMutation, useQuery } from '@tanstack/react-query';
import QRCode from 'qrcode';
import JSZip from 'jszip';
import api from '../../../../lib/api';

const { Title, Text } = Typography;

const buildLabelImage = async ({ cartonCode, unitSerial, traceQrDataUrl, claimQrDataUrl }) => {
  if (!traceQrDataUrl || !claimQrDataUrl) return null;
  const canvas = document.createElement('canvas');
  canvas.width = 960;
  canvas.height = 500;
  const ctx = canvas.getContext('2d');
  if (!ctx) return null;

  ctx.fillStyle = '#ffffff';
  ctx.fillRect(0, 0, canvas.width, canvas.height);
  ctx.strokeStyle = '#d9d9d9';
  ctx.lineWidth = 2;
  ctx.strokeRect(8, 8, canvas.width - 16, canvas.height - 16);

  ctx.fillStyle = '#111111';
  ctx.font = 'bold 30px Arial';
  ctx.fillText('TEM TRUY XUAT VA DANH GIA SAN PHAM', 24, 52);

  ctx.font = 'bold 26px Arial';
  ctx.fillText(`Thung: ${cartonCode}`, 24, 110);
  ctx.fillText(`Seri: ${unitSerial}`, 24, 162);
  ctx.font = '20px Arial';
  ctx.fillStyle = '#444';
  ctx.fillText('QR truy xuat cong khai', 82, 238);
  ctx.fillText('QR danh gia - can phu cao', 560, 238);

  const loadImage = (source) => new Promise((resolve, reject) => {
    const image = new Image();
    image.onload = () => resolve(image);
    image.onerror = reject;
    image.src = source;
  });
  const [traceQr, claimQr] = await Promise.all([
    loadImage(traceQrDataUrl),
    loadImage(claimQrDataUrl),
  ]);
  ctx.drawImage(traceQr, 70, 250, 210, 210);
  ctx.drawImage(claimQr, 560, 250, 210, 210);

  ctx.fillStyle = '#b42318';
  ctx.font = 'bold 18px Arial';
  ctx.fillText('CHI MO SAU KHI MUA', 785, 350);

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
    const claimToken = u?.claimToken;
    if (!unitSerial || !claimToken) continue;
    const traceQrDataUrl = await QRCode.toDataURL(u?.traceQrPayload || unitSerial, {
      errorCorrectionLevel: 'M',
      margin: 1,
      width: 180,
    });
    const claimQrDataUrl = await QRCode.toDataURL(claimToken, {
      errorCorrectionLevel: 'M',
      margin: 1,
      width: 180,
    });
    const labelDataUrl = await buildLabelImage({
      cartonCode,
      unitSerial,
      traceQrDataUrl,
      claimQrDataUrl,
    });
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

// eslint-disable-next-line react/prop-types
const SerialQrCard = ({ cartonCode, unit }) => {
  // eslint-disable-next-line react/prop-types
  const { unitSerial, traceQrPayload, claimToken } = unit;
  const [qrData, setQrData] = useState({ trace: '', claim: '' });
  const [loading, setLoading] = useState(false);
  const [downloading, setDownloading] = useState(false);

  useEffect(() => {
    let cancelled = false;
    const run = async () => {
      setLoading(true);
      try {
        const [trace, claim] = await Promise.all([
          QRCode.toDataURL(traceQrPayload || unitSerial, {
            errorCorrectionLevel: 'M', margin: 1, width: 180,
          }),
          QRCode.toDataURL(claimToken, {
            errorCorrectionLevel: 'M', margin: 1, width: 180,
          }),
        ]);
        if (!cancelled) setQrData({ trace, claim });
      } catch {
        if (!cancelled) setQrData({ trace: '', claim: '' });
      } finally {
        if (!cancelled) setLoading(false);
      }
    };
    run();
    return () => {
      cancelled = true;
    };
  }, [claimToken, traceQrPayload, unitSerial]);

  const downloadLabel = async () => {
    if (!qrData.trace || !qrData.claim) return;
    try {
      setDownloading(true);
      const labelDataUrl = await buildLabelImage({
        cartonCode,
        unitSerial,
        traceQrDataUrl: qrData.trace,
        claimQrDataUrl: qrData.claim,
      });
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
    <Card size="small" style={{ width: 390 }}>
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12, textAlign: 'center' }}>
        {qrData.trace && qrData.claim ? (
          <>
            <div>
              <Text strong>Truy xuất</Text>
              <img src={qrData.trace} alt={`Trace-${unitSerial}`} style={{ width: 150, height: 150, display: 'block', margin: '4px auto' }} />
            </div>
            <div>
              <Text strong type="danger">Đánh giá · phủ cào</Text>
              <img src={qrData.claim} alt={`Claim-${unitSerial}`} style={{ width: 150, height: 150, display: 'block', margin: '4px auto' }} />
            </div>
          </>
        ) : (
          <div style={{ gridColumn: '1 / -1', height: 160, display: 'grid', placeItems: 'center' }}>
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
        <Button size="small" onClick={downloadLabel} disabled={!qrData.trace || !qrData.claim || downloading}>
          {downloading ? 'Đang tạo tem...' : 'Tải tem đôi'}
        </Button>
      </div>
    </Card>
  );
};

const ManufacturePackagingManagement = () => {
  const [packingForm] = Form.useForm();
  const [packingResult, setPackingResult] = useState(null);

  const { data: summary } = useQuery({
    queryKey: ['banknoteSerialSummary'],
    queryFn: async () => {
      const res = await api.get('/product/api/v1/banknote-serials/summary');
      return res.data?.result ?? { totalSerials: 0, mySerials: 0, usedSerials: 0, availableSerials: 0 };
    },
  });

  const { data: myPallets = [], isLoading: myPalletsLoading } = useQuery({
    queryKey: ['manufacturerMyPallets'],
    queryFn: async () => {
      const res = await api.get('/product/api/v1/products/pallets/my');
      return res.data?.result ?? [];
    },
  });

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

  return (
    <div>
      <Title level={4} style={{ marginBottom: 4 }}>Đóng gói</Title>
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
          <Alert
            style={{ marginTop: 14 }}
            type="warning"
            showIcon
            message="Hãy tải và in tem đôi ngay"
            description="QR đánh giá bí mật chỉ được trả về trong lần đóng gói này. Sau khi rời màn hình, hệ thống không thể đọc lại token gốc từ cơ sở dữ liệu."
          />
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
                      unit={u}
                    />
                  ))}
                </div>
              </Card>
            ))}
          </div>
        </Card>
      )}
    </div>
  );
};

export default ManufacturePackagingManagement;
