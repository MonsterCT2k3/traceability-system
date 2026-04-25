import React, { useState } from 'react';
import {
  Typography,
  Button,
  Table,
  Modal,
  Form,
  Input,
  InputNumber,
  Upload,
  message,
  Image,
  Space,
} from 'antd';
import { PlusOutlined, PictureOutlined } from '@ant-design/icons';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '../../../../lib/api';

const { Title, Text } = Typography;

const ProductManagement = () => {
  const queryClient = useQueryClient();
  const [modalOpen, setModalOpen] = useState(false);
  const [form] = Form.useForm();
  const [fileList, setFileList] = useState([]);

  const { data: products, isLoading } = useQuery({
    queryKey: ['manufacturerProducts'],
    queryFn: async () => {
      const res = await api.get('/product/api/v1/products');
      return res.data?.result ?? [];
    },
  });

  const createMutation = useMutation({
    mutationFn: async (payload) => {
      if (payload.kind === 'multipart') {
        const { formData } = payload;
        return api.post('/product/api/v1/products', formData, {
          headers: { 'Content-Type': 'multipart/form-data' },
        });
      }
      return api.post('/product/api/v1/products', payload.body, {
        headers: { 'Content-Type': 'application/json' },
      });
    },
    onSuccess: () => {
      message.success('Tạo sản phẩm thành công');
      queryClient.invalidateQueries({ queryKey: ['manufacturerProducts'] });
      setModalOpen(false);
      form.resetFields();
      setFileList([]);
    },
    onError: (err) => {
      message.error(err.response?.data?.message || 'Không tạo được sản phẩm');
    },
  });

  const onFinish = (values) => {
    const file = fileList[0]?.originFileObj;

    if (file) {
      const fd = new FormData();
      fd.append('name', values.name.trim());
      if (values.description) fd.append('description', values.description);
      fd.append('price', String(values.price));
      fd.append('image', file);
      createMutation.mutate({ kind: 'multipart', formData: fd });
      return;
    }

    createMutation.mutate({
      kind: 'json',
      body: {
        name: values.name.trim(),
        description: values.description || undefined,
        price: values.price,
      },
    });
  };

  const columns = [
    {
      title: 'Ảnh',
      dataIndex: 'imageUrl',
      key: 'imageUrl',
      width: 88,
      render: (url) =>
        url ? (
          <Image src={url} alt="" width={56} height={56} style={{ objectFit: 'cover', borderRadius: 8 }} />
        ) : (
          <PictureOutlined style={{ fontSize: 28, color: '#bfbfbf' }} />
        ),
    },
    {
      title: 'Tên sản phẩm',
      dataIndex: 'name',
      key: 'name',
      render: (t) => <strong>{t}</strong>,
    },
    {
      title: 'Mô tả',
      dataIndex: 'description',
      key: 'description',
      ellipsis: true,
      render: (t) => t || '—',
    },
    {
      title: 'Giá',
      dataIndex: 'price',
      key: 'price',
      width: 140,
      render: (p) =>
        p != null ? `${new Intl.NumberFormat('vi-VN').format(p)} đ` : '—',
    },
  ];

  const uploadProps = {
    listType: 'picture-card',
    fileList,
    maxCount: 1,
    beforeUpload: (file) => {
      const isImg = file.type?.startsWith('image/');
      if (!isImg) {
        message.error('Chỉ chọn file ảnh');
        return Upload.LIST_IGNORE;
      }
      return false;
    },
    onChange: ({ fileList: fl }) => setFileList(fl.slice(-1)),
    onRemove: () => setFileList([]),
  };

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <div>
          <Title level={4} style={{ margin: 0 }}>
            Sản phẩm (catalog)
          </Title>
          <Text type="secondary">Tạo mặt hàng catalog; có thể kèm ảnh (upload lên Cloudinary).</Text>
        </div>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => setModalOpen(true)}>
          Thêm sản phẩm
        </Button>
      </div>

      <Table
        rowKey="id"
        loading={isLoading}
        dataSource={products || []}
        columns={columns}
        pagination={{ pageSize: 8, showSizeChanger: true }}
        locale={{ emptyText: 'Chưa có sản phẩm nào' }}
      />

      <Modal
        title="Thêm sản phẩm mới"
        open={modalOpen}
        onCancel={() => {
          setModalOpen(false);
          form.resetFields();
          setFileList([]);
        }}
        footer={null}
        destroyOnClose
        width={520}
      >
        <Form form={form} layout="vertical" onFinish={onFinish}>
          <Form.Item label="Tên sản phẩm" name="name" rules={[{ required: true, message: 'Nhập tên sản phẩm' }]}>
            <Input placeholder="VD: Sữa tươi tiệt trùng 1L" />
          </Form.Item>
          <Form.Item label="Mô tả" name="description">
            <Input.TextArea rows={3} placeholder="Mô tả ngắn (tuỳ chọn)" />
          </Form.Item>
          <Form.Item
            label="Giá (VNĐ)"
            name="price"
            rules={[{ required: true, message: 'Nhập giá' }]}
          >
            <InputNumber min={0} style={{ width: '100%' }} placeholder="VD: 25000" addonAfter="đ" />
          </Form.Item>
          <Form.Item label="Ảnh sản phẩm" extra="Tuỳ chọn — chọn ảnh để lưu lên Cloudinary.">
            <Upload {...uploadProps}>
              {fileList.length < 1 && (
                <div>
                  <PlusOutlined />
                  <div style={{ marginTop: 8 }}>Chọn ảnh</div>
                </div>
              )}
            </Upload>
          </Form.Item>
          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit" loading={createMutation.isPending}>
                Lưu
              </Button>
              <Button
                onClick={() => {
                  setModalOpen(false);
                  form.resetFields();
                  setFileList([]);
                }}
              >
                Hủy
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default ProductManagement;
