import React from 'react';
import { Card, Col, Divider, Row, Statistic, Table, Typography } from 'antd';
import { useQuery } from '@tanstack/react-query';
import QRCode from 'qrcode';
import JSZip from 'jszip';
import api from '../../../../lib/api';

const { Title, Text } = Typography;

const buildLabelImage = async ({ cartonCode, unitSerial, qrDataUrl }) => {
  if (!qrDataUrl) return null;
  const canvas = document.createElement('canvas');
  canvas.width = 720;
  canvas.height = 420;
  const ctx = canvas.getContext('2d');
  if (!ctx) return null;

  ctx.fillStyle = '#fff';
  ctx.fillRect(0, 0, canvas.width, canvas.height);
  ctx.strokeStyle = '#d9d9d9';
  ctx.lineWidth = 2;
  ctx.strokeRect(8, 8, canvas.width - 16, canvas.height - 16);

  ctx.fillStyle = '#111';
  ctx.font = 'bold 30px Arial';
  ctx.fillText('TEM TRUY XUAT SAN PHAM', 24, 52);
  ctx.font = 'bold 26px Arial';
  ctx.fillText(`Thung: ${cartonCode}`, 24, 110);
  ctx.fillText(`Seri: ${unitSerial}`, 24, 162);
  ctx.font = '20px Arial';
  ctx.fillStyle = '#444';
  ctx.fillText('QR nay ung voi dung seri in ben tren', 24, 206);

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

  ctx.fillStyle = '#fff';
  ctx.fillRect(0, 0, canvas.width, canvas.height);
  ctx.strokeStyle = '#d9d9d9';
  ctx.lineWidth = 2;
  ctx.strokeRect(8, 8, canvas.width - 16, canvas.height - 16);

  ctx.fillStyle = '#111';
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
      width: 160,
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

const SerialLabelCard = ({ cartonCode, unitSerial }) => {
  const [qrDataUrl, setQrDataUrl] = React.useState('');
  const [loading, setLoading] = React.useState(false);
  const [downloading, setDownloading] = React.useState(false);

  React.useEffect(() => {
    let cancelled = false;
    const run = async () => {
      setLoading(true);
      try {
        const dataUrl = await QRCode.toDataURL(unitSerial, {
          errorCorrectionLevel: 'M',
          margin: 1,
          width: 160,
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

  const onDownload = async () => {
    if (!qrDataUrl) return;
    setDownloading(true);
    try {
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
    <Card size="small" style={{ width: 190 }}>
      <div style={{ textAlign: 'center' }}>
        {qrDataUrl ? (
          <img src={qrDataUrl} alt={unitSerial} style={{ width: 140, height: 140, objectFit: 'contain' }} />
        ) : (
          <div style={{ height: 140, display: 'grid', placeItems: 'center' }}>
            <Text type="secondary">{loading ? 'Đang tạo QR...' : 'Không tạo được QR'}</Text>
          </div>
        )}
      </div>
      <div style={{ marginTop: 6 }}>
        <Text type="secondary">{cartonCode}</Text>
      </div>
      <div style={{ marginTop: 2 }}>
        <Text strong>{unitSerial}</Text>
      </div>
      <div style={{ marginTop: 8 }}>
        <a onClick={onDownload} style={{ pointerEvents: downloading ? 'none' : 'auto' }}>
          {downloading ? 'Đang tạo tem...' : 'Tải tem'}
        </a>
      </div>
    </Card>
  );
};

const GoodsManagement = () => {
  const { data: manifestRows = [], isLoading } = useQuery({
    queryKey: ['goodsPackingManifestByProduct'],
    queryFn: async () => {
      const res = await api.get('/product/api/v1/products/packing-manifest/my');
      return res.data?.result ?? [];
    },
  });

  const goodsRows = manifestRows.map((row) => {
    const cartonsCount = (row.cartons || []).length;
    const unitsCount = (row.cartons || []).reduce(
      (sum, c) => sum + Number((c.units || []).length),
      0,
    );
    return {
      ...row,
      cartonsCount,
      unitsCount,
    };
  });

  const totalCartons = goodsRows.reduce((sum, row) => sum + row.cartonsCount, 0);
  const totalUnits = goodsRows.reduce((sum, row) => sum + row.unitsCount, 0);

  const columns = [
    {
      title: 'Sản phẩm',
      dataIndex: 'productName',
      key: 'productName',
      render: (value) => value || 'Không xác định',
    },
    {
      title: 'Số thùng đã đóng gói',
      dataIndex: 'cartonsCount',
      key: 'cartonsCount',
      width: 220,
      render: (value) => Number(value || 0).toLocaleString('vi-VN'),
    },
    {
      title: 'Số đơn vị thành phẩm',
      dataIndex: 'unitsCount',
      key: 'unitsCount',
      width: 220,
      render: (value) => Number(value || 0).toLocaleString('vi-VN'),
    },
  ];

  return (
    <div>
      <Title level={4}>Quản lý hàng hóa</Title>
      <Text>Xem lại thùng đã đóng gói và danh sách serial sản phẩm theo từng lô hàng.</Text>
      <Divider />

      <Row gutter={[16, 16]}>
        <Col xs={24} md={8}>
          <Card loading={isLoading}>
            <Statistic
              title="Tổng số sản phẩm đã đóng gói"
              value={goodsRows.length}
            />
          </Card>
        </Col>
        <Col xs={24} md={8}>
          <Card loading={isLoading}>
            <Statistic
              title="Tổng số thùng đã đóng gói"
              value={totalCartons}
            />
          </Card>
        </Col>
        <Col xs={24} md={8}>
          <Card loading={isLoading}>
            <Statistic
              title="Tổng số đơn vị thành phẩm"
              value={totalUnits}
            />
          </Card>
        </Col>
      </Row>

      <Card style={{ marginTop: 16 }}>
        <Table
          rowKey={(record) => record.productId}
          columns={columns}
          dataSource={goodsRows}
          loading={isLoading}
          pagination={{ pageSize: 10 }}
          expandable={{
            expandedRowRender: (record) => (
              <Table
                rowKey={(c) => c.cartonCode}
                size="small"
                pagination={false}
                columns={[
                  {
                    title: 'Mã thùng',
                    dataIndex: 'cartonCode',
                    key: 'cartonCode',
                    width: 180,
                    render: (v, carton) => (
                      <div>
                        <strong>{v}</strong>
                        <div style={{ marginTop: 8 }}>
                          <a onClick={() => downloadCartonLabelsZip({ cartonCode: carton.cartonCode, units: carton.units || [] })}>
                            Tải tất cả tem (.zip)
                          </a>
                        </div>
                      </div>
                    ),
                  },
                  {
                    title: 'Danh sách serial sản phẩm',
                    key: 'units',
                    render: (_, carton) => (
                      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
                        {(carton.units || []).map((u, idx) => (
                          <SerialLabelCard
                            key={`${carton.cartonCode}-${idx}`}
                            cartonCode={carton.cartonCode}
                            unitSerial={u.unitSerial}
                          />
                        ))}
                      </div>
                    ),
                  },
                ]}
                dataSource={record.cartons || []}
              />
            ),
          }}
          locale={{ emptyText: 'Chưa có dữ liệu đóng gói thành phẩm' }}
        />
      </Card>
    </div>
  );
};

export default GoodsManagement;
