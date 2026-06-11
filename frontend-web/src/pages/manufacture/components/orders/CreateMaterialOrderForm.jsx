import React, { useEffect, useMemo, useState } from 'react';
import {
  Alert,
  Button,
  Card,
  Divider,
  Form,
  Input,
  Radio,
  Select,
  Space,
  Spin,
  Typography,
  message,
} from 'antd';
import { BankOutlined, MinusCircleOutlined, PlusOutlined, ShopOutlined } from '@ant-design/icons';
import api from '../../../../lib/api';
import { ORDER_TYPE } from '../../constants/tradeOrderConstants';
import SupplierSearchSelect from './SupplierSearchSelect';

const { Text } = Typography;

const SOURCE = {
  SUPPLIER: 'SUPPLIER',
  MANUFACTURER: 'MANUFACTURER',
};

const CreateMaterialOrderForm = ({ submitting, onSubmit }) => {
  const [form] = Form.useForm();
  const [sourceType, setSourceType] = useState(SOURCE.SUPPLIER);
  const [sellerId, setSellerId] = useState(null);
  const [rawBatches, setRawBatches] = useState([]);
  const [rawBatchesLoading, setRawBatchesLoading] = useState(false);
  const [manufacturers, setManufacturers] = useState([]);
  const [pallets, setPallets] = useState([]);
  const [palletsLoading, setPalletsLoading] = useState(false);

  const watchedManufacturerId = Form.useWatch('manufacturerSellerId', form);

  const rawBatchOptions = useMemo(
    () =>
      rawBatches.map((batch) => ({
        value: batch.id,
        label: `${batch.rawBatchCode} · ${batch.materialName} - tồn ${batch.quantity} ${batch.unit}`,
      })),
    [rawBatches]
  );

  const palletOptions = useMemo(
    () =>
      pallets.map((pallet) => ({
        value: pallet.id,
        label: `${pallet.palletCode} · ${pallet.palletName || 'Lô sản xuất'}`,
      })),
    [pallets]
  );

  useEffect(() => {
    api
      .get('/identity/api/v1/users/directory/manufacturers')
      .then((res) => setManufacturers(res.data?.result ?? []))
      .catch(() => setManufacturers([]));
  }, []);

  useEffect(() => {
    if (sourceType !== SOURCE.SUPPLIER || !sellerId) {
      setRawBatches([]);
      return;
    }

    let cancelled = false;
    setRawBatchesLoading(true);
    api
      .get(`/product/api/v1/raw-batches/by-owner/${encodeURIComponent(sellerId)}`)
      .then((res) => {
        if (!cancelled) {
          setRawBatches(res.data?.result ?? []);
          form.setFieldsValue({ lines: [{}] });
        }
      })
      .catch(() => {
        if (!cancelled) setRawBatches([]);
      })
      .finally(() => {
        if (!cancelled) setRawBatchesLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [sellerId, sourceType, form]);

  useEffect(() => {
    if (sourceType !== SOURCE.MANUFACTURER || !watchedManufacturerId) {
      setPallets([]);
      return;
    }

    let cancelled = false;
    setPalletsLoading(true);
    api
      .get(`/product/api/v1/products/pallets/by-owner/${encodeURIComponent(watchedManufacturerId)}`)
      .then((res) => {
        if (!cancelled) setPallets(res.data?.result ?? []);
      })
      .catch(() => {
        if (!cancelled) setPallets([]);
      })
      .finally(() => {
        if (!cancelled) setPalletsLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [watchedManufacturerId, sourceType]);

  const changeSourceType = (nextType) => {
    setSourceType(nextType);
    setSellerId(null);
    setRawBatches([]);
    setPallets([]);
    form.resetFields();
    form.setFieldsValue({ lines: [{}] });
  };

  const submit = (values) => {
    if (sourceType === SOURCE.SUPPLIER) {
      submitSupplierOrder(values);
      return;
    }
    submitManufacturerOrder(values);
  };

  const submitSupplierOrder = (values) => {
    if (!sellerId) {
      message.warning('Vui lòng chọn nhà cung cấp');
      return;
    }

    const lines = (values.lines || []).map((line) => {
      const batchId = line.batchId?.trim();
      const batch = rawBatches.find((item) => item.id === batchId);
      return {
        targetRawBatchId: batchId,
        quantityRequested: String(line.quantityRequested ?? '').trim(),
        unit: batch?.unit ?? '',
      };
    });

    const batchIds = lines.map((line) => line.targetRawBatchId).filter(Boolean);
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

  const submitManufacturerOrder = (values) => {
    const palletIds = values.palletIds || [];
    if (!values.manufacturerSellerId) {
      message.warning('Vui lòng chọn nhà sản xuất bán');
      return;
    }
    if (palletIds.length === 0) {
      message.warning('Chọn ít nhất một pallet');
      return;
    }

    onSubmit({
      orderType: ORDER_TYPE.MANUFACTURER_TO_MANUFACTURER,
      sellerId: values.manufacturerSellerId,
      note: values.note?.trim() || undefined,
      lines: palletIds.map((targetPalletId) => ({ targetPalletId })),
    });
  };

  return (
    <Form form={form} layout="vertical" onFinish={submit} initialValues={{ lines: [{}] }}>
      <Card
        bordered={false}
        style={{
          marginBottom: 18,
          background: 'linear-gradient(135deg, #f7fbff 0%, #eef7f2 100%)',
          border: '1px solid #d9edf7',
        }}
      >
        <Form.Item label="Nguồn mua" style={{ marginBottom: 0 }}>
          <Radio.Group
            value={sourceType}
            onChange={(event) => changeSourceType(event.target.value)}
            optionType="button"
            buttonStyle="solid"
            size="large"
          >
            <Radio.Button value={SOURCE.SUPPLIER}>
              <Space>
                <ShopOutlined />
                Nhà cung cấp
              </Space>
            </Radio.Button>
            <Radio.Button value={SOURCE.MANUFACTURER}>
              <Space>
                <BankOutlined />
                Nhà sản xuất
              </Space>
            </Radio.Button>
          </Radio.Group>
        </Form.Item>
      </Card>

      {sourceType === SOURCE.SUPPLIER ? (
        <SupplierOrderFields
          form={form}
          sellerId={sellerId}
          setSellerId={setSellerId}
          rawBatches={rawBatches}
          rawBatchesLoading={rawBatchesLoading}
          rawBatchOptions={rawBatchOptions}
        />
      ) : (
        <ManufacturerOrderFields
          manufacturers={manufacturers}
          pallets={pallets}
          palletsLoading={palletsLoading}
          palletOptions={palletOptions}
        />
      )}

      <Form.Item label="Ghi chú đơn hàng" name="note">
        <Input.TextArea rows={3} placeholder="Ví dụ: thời gian giao mong muốn, yêu cầu vận chuyển..." />
      </Form.Item>

      <Button type="primary" htmlType="submit" loading={submitting} size="large" block>
        Gửi đơn đặt hàng
      </Button>
    </Form>
  );
};

const SupplierOrderFields = ({ form, sellerId, setSellerId, rawBatches, rawBatchesLoading, rawBatchOptions }) => (
  <>
    <Alert
      type="info"
      showIcon
      style={{ marginBottom: 16 }}
      message="Mua nguyên liệu gốc từ nhà cung cấp. Chọn nhà cung cấp, sau đó chọn lô và số lượng cần đặt."
    />

    <Form.Item label="Nhà cung cấp" required>
      <SupplierSearchSelect
        value={sellerId}
        onChange={(id) => setSellerId(id)}
        onClear={() => setSellerId(null)}
      />
    </Form.Item>

    {!sellerId ? (
      <Text type="secondary">Chọn nhà cung cấp để xem các lô nguyên liệu có thể đặt.</Text>
    ) : rawBatchesLoading ? (
      <Spin tip="Đang tải lô nguyên liệu..." />
    ) : rawBatches.length === 0 ? (
      <Alert type="warning" showIcon message="Nhà cung cấp này chưa có lô nguyên liệu nào khả dụng." />
    ) : (
      <>
        <Text type="secondary" style={{ display: 'block', marginBottom: 12 }}>
          Có <strong>{rawBatches.length}</strong> lô khả dụng. Số lượng đặt không được vượt tồn lô.
        </Text>
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
                    style={{ flex: 1, minWidth: 320 }}
                  >
                    <Select
                      placeholder="Chọn lô nguyên liệu"
                      options={rawBatchOptions}
                      showSearch
                      optionFilterProp="label"
                      popupMatchSelectWidth={false}
                    />
                  </Form.Item>
                  <Form.Item
                    {...restField}
                    name={[name, 'quantityRequested']}
                    rules={[{ required: true, message: 'Nhập số lượng' }]}
                    style={{ width: 150 }}
                  >
                    <Input placeholder="Số lượng" />
                  </Form.Item>
                  <Form.Item shouldUpdate noStyle>
                    {() => {
                      const batchId = form.getFieldValue(['lines', name, 'batchId']);
                      const batch = rawBatches.find((item) => item.id === batchId);
                      return batch ? (
                        <div style={{ paddingTop: 5 }}>
                          <Text type="secondary">
                            Đơn vị: <strong>{batch.unit}</strong> · Tồn: <strong>{batch.quantity}</strong>
                          </Text>
                        </div>
                      ) : null;
                    }}
                  </Form.Item>
                  <Button type="text" danger icon={<MinusCircleOutlined />} onClick={() => remove(name)} disabled={fields.length <= 1}>
                    Xóa
                  </Button>
                </Space>
              ))}
              <Button type="dashed" onClick={() => add({})} block icon={<PlusOutlined />} style={{ marginBottom: 18 }}>
                Thêm lô nguyên liệu
              </Button>
            </>
          )}
        </Form.List>
      </>
    )}
  </>
);

const ManufacturerOrderFields = ({ manufacturers, pallets, palletsLoading, palletOptions }) => (
  <>
    <Alert
      type="info"
      showIcon
      style={{ marginBottom: 16 }}
      message="Mua pallet từ nhà sản xuất khác để dùng làm nguyên liệu đầu vào trực tiếp."
    />

    <Form.Item
      name="manufacturerSellerId"
      label="Nhà sản xuất bán"
      rules={[{ required: true, message: 'Chọn nhà sản xuất bán' }]}
    >
      <Select
        showSearch
        optionFilterProp="label"
        placeholder="Chọn nhà sản xuất"
        options={manufacturers.map((item) => ({
          value: item.id,
          label: item.fullName || item.username || item.id,
        }))}
      />
    </Form.Item>

    <Form.Item name="palletIds" label="Pallet đặt mua" rules={[{ required: true, message: 'Chọn pallet' }]}>
      <Select
        mode="multiple"
        loading={palletsLoading}
        placeholder="Chọn một hoặc nhiều pallet"
        optionFilterProp="label"
        notFoundContent={palletsLoading ? <Spin size="small" /> : 'Không có pallet khả dụng'}
        options={palletOptions}
      />
    </Form.Item>
  </>
);

export default CreateMaterialOrderForm;
