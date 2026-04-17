import React, { useEffect, useState, useMemo } from 'react';
import { Form, Input, Button, Card, Space, Select, Typography, Divider, Alert, Spin, message } from 'antd';
import { MinusCircleOutlined, PlusOutlined } from '@ant-design/icons';
import api from '../../../../lib/api';
import { ORDER_TYPE } from '../../constants/tradeOrderConstants';
import SupplierSearchSelect from './SupplierSearchSelect';

const { Text } = Typography;

/**
 * Đặt hàng NSX → NCC: chọn NCC (tìm kiếm), chọn lô từ danh sách lô của NCC, nhập số lượng.
 */
const CreatePurchaseOrderForm = ({ submitting, onSubmit }) => {
  const [form] = Form.useForm();
  const [sellerId, setSellerId] = useState(null);
  const [batches, setBatches] = useState([]);
  const [batchesLoading, setBatchesLoading] = useState(false);

  const batchOptions = useMemo(
    () =>
      (batches || []).map((b) => ({
        value: b.id,
        label: `${b.rawBatchCode} · ${b.materialName} — tồn ${b.quantity} ${b.unit}`,
      })),
    [batches]
  );

  useEffect(() => {
    if (!sellerId) {
      setBatches([]);
      form.setFieldsValue({ lines: [{}] });
      return;
    }
    let cancelled = false;
    (async () => {
      setBatchesLoading(true);
      try {
        const res = await api.get(`/product/api/v1/raw-batches/by-owner/${encodeURIComponent(sellerId)}`);
        const list = res.data?.result ?? [];
        if (!cancelled) {
          setBatches(list);
          form.setFieldsValue({ lines: [{}] });
        }
      } catch {
        if (!cancelled) {
          setBatches([]);
        }
      } finally {
        if (!cancelled) setBatchesLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [sellerId, form]);

  const handleFinish = (values) => {
    if (!sellerId) {
      message.warning('Vui lòng chọn nhà cung cấp');
      return;
    }
    const lines = (values.lines || []).map((line) => {
      const batchId = line.batchId?.trim();
      const batch = batches.find((b) => b.id === batchId);
      const qty = String(line.quantityRequested ?? '').trim();
      return {
        targetRawBatchId: batchId,
        quantityRequested: qty,
        unit: batch?.unit ?? '',
      };
    });
    const batchIds = lines.map((l) => l.targetRawBatchId).filter(Boolean);
    if (new Set(batchIds).size !== batchIds.length) {
      message.error('Không được chọn cùng một lô trên nhiều dòng');
      return;
    }
    onSubmit({
      orderType: ORDER_TYPE.MANUFACTURER_TO_SUPPLIER,
      sellerId,
      note: values.note?.trim() || undefined,
      lines,
    });
  };

  return (
    <Card bordered={false} style={{ maxWidth: 960 }}>
      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 16 }}
        message="Chọn nhà cung cấp rồi chọn từng lô và số lượng đặt. Đơn vị trên đơn phải khớp với lô."
      />

      <Form form={form} layout="vertical" onFinish={handleFinish} initialValues={{ lines: [{}] }}>
        <Form.Item label="Nhà cung cấp" required>
          <SupplierSearchSelect
            value={sellerId}
            onChange={(id) => setSellerId(id)}
            onClear={() => setSellerId(null)}
          />
        </Form.Item>

        {!sellerId ? (
          <Text type="secondary">Chọn NCC để xem danh sách lô có thể đặt.</Text>
        ) : batchesLoading ? (
          <Spin tip="Đang tải lô hàng của NCC…" />
        ) : batches.length === 0 ? (
          <Alert type="warning" message="NCC này chưa có lô nguyên liệu nào trên hệ thống." showIcon />
        ) : (
          <>
            <Text type="secondary" style={{ display: 'block', marginBottom: 12 }}>
              Đang hiển thị <strong>{batches.length}</strong> lô. Chọn lô và nhập số lượng đặt (không vượt tồn).
            </Text>
            <Form.Item label="Ghi chú đơn hàng" name="note">
              <Input.TextArea rows={2} placeholder="Tuỳ chọn" />
            </Form.Item>

            <Divider orientation="left">Các lô đặt mua</Divider>
            <Form.List name="lines">
              {(fields, { add, remove }) => (
                <>
                  {fields.map(({ key, name, ...restField }) => (
                    <Space key={key} style={{ display: 'flex', marginBottom: 12, flexWrap: 'wrap', width: '100%' }} align="start">
                      <Form.Item
                        {...restField}
                        name={[name, 'batchId']}
                        rules={[{ required: true, message: 'Chọn lô' }]}
                        style={{ flex: 1, minWidth: 280 }}
                      >
                        <Select
                          placeholder="Chọn lô nguyên liệu"
                          options={batchOptions}
                          showSearch
                          optionFilterProp="label"
                          popupMatchSelectWidth={false}
                          style={{ minWidth: 320 }}
                        />
                      </Form.Item>
                      <Form.Item
                        {...restField}
                        name={[name, 'quantityRequested']}
                        rules={[{ required: true, message: 'Nhập số lượng' }]}
                        style={{ width: 140 }}
                      >
                        <Input placeholder="Số lượng" />
                      </Form.Item>
                      <Form.Item shouldUpdate noStyle>
                        {() => {
                          const bid = form.getFieldValue(['lines', name, 'batchId']);
                          const b = batches.find((x) => x.id === bid);
                          return b ? (
                            <div style={{ paddingTop: 5 }}>
                              <Text type="secondary">
                                Đơn vị: <strong>{b.unit}</strong> · Tồn: <strong>{b.quantity}</strong>
                              </Text>
                            </div>
                          ) : null;
                        }}
                      </Form.Item>
                      <Button type="text" danger icon={<MinusCircleOutlined />} onClick={() => remove(name)} disabled={fields.length <= 1}>
                        Xóa dòng
                      </Button>
                    </Space>
                  ))}
                  <Form.Item>
                    <Button type="dashed" onClick={() => add({})} block icon={<PlusOutlined />}>
                      Thêm dòng lô
                    </Button>
                  </Form.Item>
                </>
              )}
            </Form.List>

            <Form.Item>
              <Button type="primary" htmlType="submit" loading={submitting} size="large">
                Gửi đơn đặt hàng
              </Button>
            </Form.Item>
          </>
        )}
      </Form>
    </Card>
  );
};

export default CreatePurchaseOrderForm;
