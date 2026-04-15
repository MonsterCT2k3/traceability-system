import React, { useState } from 'react';
import {
  Button,
  Typography,
  message,
  Divider,
  Form,
  Input,
  Tag,
  DatePicker,
  Select,
  Card,
  Table,
  Modal,
  Alert,
  Space,
} from 'antd';
import { MergeCellsOutlined } from '@ant-design/icons';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '../../../lib/api';

const { Title, Text } = Typography;
const { Option } = Select;

const SupplierRawBatchManagement = () => {
  const [form] = Form.useForm();
  const [mergeForm] = Form.useForm();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isCreating, setIsCreating] = useState(false);
  const [mergeMode, setMergeMode] = useState(false);
  const [selectedRowKeys, setSelectedRowKeys] = useState([]);
  const [mergeModalOpen, setMergeModalOpen] = useState(false);
  const queryClient = useQueryClient();

  const { data: rawBatches, isLoading } = useQuery({
    queryKey: ['myRawBatches'],
    queryFn: async () => {
      const response = await api.get('/product/api/v1/raw-batches/my');
      return response.data.result;
    },
  });

  const handleGetLocationFromProfile = () => {
    const profile = queryClient.getQueryData(['userProfile']);
    if (profile && profile.location) {
      form.setFieldsValue({ location: profile.location });
      message.success('Đã lấy vị trí gốc từ tài khoản');
    } else {
      message.warning('Chưa có vị trí mặc định. Vui lòng cập nhật trong phần Tổng quan.');
    }
  };

  const handleMergeGetLocationFromProfile = () => {
    const profile = queryClient.getQueryData(['userProfile']);
    if (profile && profile.location) {
      mergeForm.setFieldsValue({ location: profile.location });
      message.success('Đã lấy vị trí gốc từ tài khoản');
    } else {
      message.warning('Chưa có vị trí mặc định. Vui lòng cập nhật trong phần Tổng quan.');
    }
  };

  const onFinish = async (values) => {
    setIsSubmitting(true);
    try {
      const harvestedAt = values.harvestedAt ? values.harvestedAt.format('YYYY-MM-DD') : '';

      const payload = {
        ...values,
        harvestedAt,
      };

      const response = await api.post('/product/api/v1/raw-batches', payload);
      const r = response.data.result || {};
      message.success('Tạo lô nguyên liệu thành công! (Tx: ' + (r.anchorTxHash || r.txHash || '—') + ')');
      form.resetFields();
      setIsCreating(false);
      queryClient.invalidateQueries({ queryKey: ['myRawBatches'] });
    } catch (error) {
      message.error(error.response?.data?.message || 'Lỗi khi tạo lô nguyên liệu');
    } finally {
      setIsSubmitting(false);
    }
  };

  const mergeMutation = useMutation({
    mutationFn: async ({ sourceRawBatchIds, note, location }) => {
      const res = await api.post('/product/api/v1/raw-batches/merge', {
        sourceRawBatchIds,
        note: note || undefined,
        location: location || undefined,
      });
      return res.data.result;
    },
    onSuccess: (result) => {
      message.success(
        `Gộp lô thành công — mã lô mới: ${result?.rawBatchCode || '—'} (Tx: ${result?.anchorTxHash || '—'})`
      );
      setMergeModalOpen(false);
      mergeForm.resetFields();
      setMergeMode(false);
      setSelectedRowKeys([]);
      queryClient.invalidateQueries({ queryKey: ['myRawBatches'] });
    },
    onError: (error) => {
      message.error(error.response?.data?.message || 'Gộp lô thất bại');
    },
  });

  const selectedRows = (rawBatches || []).filter((r) => selectedRowKeys.includes(r.id));

  const validateMergeSelection = () => {
    if (selectedRowKeys.length < 2) {
      message.warning('Chọn ít nhất 2 lô để gộp.');
      return false;
    }
    const first = selectedRows[0];
    const sameType = selectedRows.every(
      (r) => r.materialType === first.materialType && r.materialName === first.materialName
    );
    if (!sameType) {
      message.error('Chỉ gộp được các lô cùng loại nguyên liệu và cùng tên (materialType + materialName).');
      return false;
    }
    return true;
  };

  const openMergeModal = () => {
    if (!validateMergeSelection()) return;
    mergeForm.resetFields();
    setMergeModalOpen(true);
  };

  const submitMerge = async () => {
    try {
      const values = await mergeForm.validateFields();
      mergeMutation.mutate({
        sourceRawBatchIds: selectedRowKeys,
        note: values.note,
        location: values.location,
      });
    } catch {
      /* form validation */
    }
  };

  const rowSelection = mergeMode
    ? {
        selectedRowKeys,
        onChange: (keys) => setSelectedRowKeys(keys),
        getCheckboxProps: (record) => ({
          disabled: record.status === 'SHIPPED',
        }),
      }
    : undefined;

  const columns = [
    {
      title: 'Mã lô',
      dataIndex: 'rawBatchCode',
      key: 'rawBatchCode',
      render: (text) => <strong>{text}</strong>,
    },
    {
      title: 'Loại',
      dataIndex: 'materialType',
      key: 'materialType',
      width: 120,
    },
    {
      title: 'Tên nguyên liệu',
      dataIndex: 'materialName',
      key: 'materialName',
    },
    {
      title: 'Số lượng',
      key: 'quantity',
      render: (_, record) => `${record.quantity} ${record.unit}`,
    },
    {
      title: 'Ngày thu hoạch',
      dataIndex: 'harvestedAt',
      key: 'harvestedAt',
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      render: (status) => {
        let color = 'default';
        let label = status;
        if (status === 'NOT_SHIPPED') {
          color = 'default';
          label = 'Chưa vận chuyển';
        } else if (status === 'PENDING_SHIPMENT') {
          color = 'warning';
          label = 'Đang chờ vận chuyển';
        } else if (status === 'SHIPPED') {
          color = 'success';
          label = 'Đã vận chuyển';
        }
        return <Tag color={color}>{label}</Tag>;
      },
    },
    {
      title: 'Ngày tạo',
      dataIndex: 'createdAt',
      key: 'createdAt',
      render: (date) => new Date(date).toLocaleString('vi-VN'),
    },
  ];

  if (isCreating) {
    return (
      <div>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
          <Title level={4} style={{ margin: 0 }}>
            Tạo mới Lô nguyên liệu
          </Title>
          <Button onClick={() => setIsCreating(false)}>Quay lại danh sách</Button>
        </div>
        <Text>Tạo mới lô nguyên liệu (Raw Batch) và ghi lên Blockchain.</Text>
        <Divider />

        <Card style={{ maxWidth: 800 }}>
          <Form form={form} layout="vertical" onFinish={onFinish}>
            <div style={{ display: 'flex', gap: '16px', flexWrap: 'wrap' }}>
              <Form.Item
                name="materialType"
                label="Loại nguyên liệu"
                rules={[{ required: true, message: 'Vui lòng chọn loại nguyên liệu!' }]}
                style={{ flex: 1, minWidth: '250px' }}
              >
                <Select placeholder="Chọn loại nguyên liệu">
                  <Option value="Sữa tươi">Sữa tươi</Option>
                  <Option value="Rau củ">Rau củ</Option>
                  <Option value="Cà phê">Cà phê</Option>
                  <Option value="Khác">Khác</Option>
                </Select>
              </Form.Item>

              <Form.Item
                name="materialName"
                label="Tên nguyên liệu"
                rules={[{ required: true, message: 'Vui lòng nhập tên nguyên liệu!' }]}
                style={{ flex: 1, minWidth: '250px' }}
              >
                <Input placeholder="VD: Sữa bò tươi nguyên liệu" />
              </Form.Item>
            </div>

            <div style={{ display: 'flex', gap: '16px', flexWrap: 'wrap' }}>
              <Form.Item
                name="quantity"
                label="Số lượng"
                rules={[{ required: true, message: 'Vui lòng nhập số lượng!' }]}
                style={{ flex: 1, minWidth: '250px' }}
              >
                <Input type="number" placeholder="VD: 1000" />
              </Form.Item>

              <Form.Item
                name="unit"
                label="Đơn vị tính"
                rules={[{ required: true, message: 'Vui lòng chọn đơn vị tính!' }]}
                style={{ flex: 1, minWidth: '250px' }}
              >
                <Select placeholder="Chọn đơn vị (vd: kg, litre)">
                  <Option value="kg">Kilogram (kg)</Option>
                  <Option value="litre">Lít (litre)</Option>
                  <Option value="ton">Tấn (ton)</Option>
                  <Option value="box">Thùng/Hộp (box)</Option>
                </Select>
              </Form.Item>
            </div>

            <div style={{ display: 'flex', gap: '16px', flexWrap: 'wrap' }}>
              <Form.Item
                name="harvestedAt"
                label="Ngày thu hoạch/sản xuất"
                rules={[{ required: true, message: 'Vui lòng chọn ngày thu hoạch!' }]}
                style={{ flex: 1, minWidth: '250px' }}
              >
                <DatePicker style={{ width: '100%' }} format="YYYY-MM-DD" placeholder="Chọn ngày" />
              </Form.Item>

              <Form.Item
                name="location"
                label="Nguồn gốc/Vị trí"
                rules={[{ required: true, message: 'Vui lòng lấy vị trí hiện tại!' }]}
                style={{ flex: 1, minWidth: '250px' }}
              >
                <Input
                  readOnly
                  placeholder="Nhấn để lấy vị trí gốc..."
                  addonAfter={
                    <Button
                      type="link"
                      size="small"
                      onClick={handleGetLocationFromProfile}
                      style={{ margin: '-4px -11px' }}
                    >
                      Lấy vị trí gốc
                    </Button>
                  }
                />
              </Form.Item>
            </div>

            <Form.Item name="note" label="Ghi chú thêm">
              <Input.TextArea rows={3} placeholder="Các thông tin bổ sung (nếu có)" />
            </Form.Item>

            <Form.Item>
              <Button type="primary" htmlType="submit" loading={isSubmitting}>
                Tạo và ghi lên Blockchain
              </Button>
            </Form.Item>
          </Form>
        </Card>
      </div>
    );
  }

  return (
    <div>
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: 16,
          flexWrap: 'wrap',
          gap: 12,
        }}
      >
        <div>
          <Title level={4} style={{ margin: 0 }}>
            Quản lý Lô nguyên liệu
          </Title>
          <Text>Danh sách các lô nguyên liệu bạn đã tạo trên hệ thống.</Text>
        </div>
        <Space wrap>
          {!mergeMode ? (
            <>
              <Button type="primary" onClick={() => setIsCreating(true)}>
                + Tạo lô nguyên liệu
              </Button>
              <Button
                icon={<MergeCellsOutlined />}
                onClick={() => {
                  setMergeMode(true);
                  setSelectedRowKeys([]);
                }}
              >
                Gộp lô
              </Button>
            </>
          ) : (
            <>
              <Button
                type="primary"
                icon={<MergeCellsOutlined />}
                disabled={selectedRowKeys.length < 2}
                loading={mergeMutation.isPending}
                onClick={openMergeModal}
              >
                Gộp đã chọn ({selectedRowKeys.length})
              </Button>
              <Button
                onClick={() => {
                  setMergeMode(false);
                  setSelectedRowKeys([]);
                }}
              >
                Hủy chế độ gộp
              </Button>
            </>
          )}
        </Space>
      </div>
      {mergeMode && (
        <Alert
          style={{ marginBottom: 16 }}
          type="info"
          showIcon
          message="Chọn từ 2 lô trở lên (cùng loại và cùng tên nguyên liệu). Lô đã vận chuyển không thể chọn. Các lô nguồn sẽ bị xóa sau khi gộp thành công."
        />
      )}
      <Divider style={{ marginTop: 8 }} />

      <Card>
        <Table
          columns={columns}
          dataSource={rawBatches}
          rowKey="id"
          loading={isLoading}
          rowSelection={rowSelection}
          pagination={{ pageSize: 10 }}
        />
      </Card>

      <Modal
        title="Xác nhận gộp lô"
        open={mergeModalOpen}
        onCancel={() => !mergeMutation.isPending && setMergeModalOpen(false)}
        onOk={submitMerge}
        confirmLoading={mergeMutation.isPending}
        okText="Gộp và ghi blockchain"
        destroyOnClose
      >
        <Text type="secondary" style={{ display: 'block', marginBottom: 12 }}>
          Sẽ tạo một lô mới với tổng số lượng; các lô đã chọn sẽ bị xóa khỏi hệ thống sau khi thành công.
        </Text>
        {selectedRows.length > 0 && (
          <div style={{ marginBottom: 16, padding: 12, background: '#fafafa', borderRadius: 8 }}>
            <Text strong>{selectedRows[0].materialType}</Text> — <Text>{selectedRows[0].materialName}</Text>
            <div style={{ marginTop: 8 }}>
              {selectedRows.map((r) => (
                <div key={r.id}>
                  <Text code>{r.rawBatchCode}</Text>: {r.quantity} {r.unit}
                </div>
              ))}
            </div>
          </div>
        )}
        <Form form={mergeForm} layout="vertical">
          <Form.Item name="location" label="Vị trí / kho cho lô mới (tùy chọn)">
            <Input
              placeholder="Để trống sẽ lấy từ lô nguồn đầu tiên"
              addonAfter={
                <Button
                  type="link"
                  size="small"
                  onClick={handleMergeGetLocationFromProfile}
                  style={{ margin: '-4px -11px' }}
                >
                  Lấy vị trí gốc
                </Button>
              }
            />
          </Form.Item>
          <Form.Item name="note" label="Ghi chú (tùy chọn)">
            <Input.TextArea rows={2} placeholder="Ghi chú cho lô sau gộp" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default SupplierRawBatchManagement;
