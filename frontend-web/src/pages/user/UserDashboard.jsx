import React, { useState, useEffect } from 'react';
import { Card, Button, Form, Select, Table, Tag, message, Typography, Space, Input } from 'antd';
import { useNavigate } from 'react-router-dom';
import { jwtDecode } from 'jwt-decode';
import api from '../../lib/api';

const { Title, Text } = Typography;
const { Option } = Select;

const UserDashboard = () => {
  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm();
  const navigate = useNavigate();

  // Lấy thông tin user từ token
  const token = localStorage.getItem('accessToken');
  let userRole = 'USER';
  let username = 'Unknown';
  if (token) {
    try {
      const decoded = jwtDecode(token);
      userRole = decoded.role;
      username = decoded.sub; // sub thường lưu username
    } catch (e) {
      console.error(e);
    }
  }

  const fetchRequests = async () => {
    setLoading(true);
    try {
      const response = await api.get('/identity/api/v1/users/role-requests');
      setRequests(response.data.result || []);
    } catch (error) {
      message.error('Lỗi khi tải danh sách yêu cầu');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchRequests();
  }, []);

  const onFinish = async (values) => {
    setSubmitting(true);
    try {
      await api.post('/identity/api/v1/users/role-requests', {
        requestedRole: values.requestedRole,
        description: values.description,
      });
      message.success('Đã gửi yêu cầu thành công!');
      form.resetFields();
      fetchRequests(); // Tải lại danh sách
    } catch (error) {
      const errorMsg = error.response?.data?.message || 'Lỗi khi gửi yêu cầu';
      message.error(errorMsg);
    } finally {
      setSubmitting(false);
    }
  };

  const handleLogout = () => {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('userRole');
    navigate('/login');
  };

  const columns = [
    {
      title: 'Vai trò yêu cầu',
      dataIndex: 'requestedRole',
      key: 'requestedRole',
      render: (role) => <Tag color="blue">{role}</Tag>,
    },
    {
      title: 'Mô tả',
      dataIndex: 'description',
      key: 'description',
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      render: (status) => {
        let color = 'default';
        if (status === 'PENDING') color = 'warning';
        if (status === 'APPROVED') color = 'success';
        if (status === 'REJECTED') color = 'error';
        return <Tag color={color}>{status}</Tag>;
      },
    },
    {
      title: 'Ngày gửi',
      dataIndex: 'createdAt',
      key: 'createdAt',
      render: (date) => new Date(date).toLocaleString('vi-VN'),
    },
    {
      title: 'Ngày cập nhật',
      dataIndex: 'updatedAt',
      key: 'updatedAt',
      render: (date) => new Date(date).toLocaleString('vi-VN'),
    },
  ];

  return (
    <div style={{ padding: '24px', maxWidth: '1000px', margin: '0 auto' }}>
      <Card style={{ marginBottom: '24px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div>
            <Title level={3}>Xin chào, {username}!</Title>
            <Text>Vai trò hiện tại của bạn: <Tag color="green">{userRole}</Tag></Text>
          </div>
          <Button type="primary" danger onClick={handleLogout}>Đăng xuất</Button>
        </div>
      </Card>

      <Card title="Gửi yêu cầu nâng cấp tài khoản" style={{ marginBottom: '24px' }}>
        <Form form={form} layout="inline" onFinish={onFinish}>
          <Form.Item
            name="requestedRole"
            rules={[{ required: true, message: 'Vui lòng chọn vai trò muốn đăng ký!' }]}
          >
            <Select placeholder="Chọn vai trò..." style={{ width: 200 }}>
              <Option value="MANUFACTURER">Nhà sản xuất (MANUFACTURER)</Option>
              <Option value="SUPPLIER">Nhà cung cấp (SUPPLIER)</Option>
            </Select>
          </Form.Item>
          <Form.Item
            name="description"
            rules={[{ required: true, message: 'Vui lòng nhập mô tả chi tiết!' }]}
          >
            <Input.TextArea placeholder="Mô tả chi tiết về doanh nghiệp của bạn..." style={{ width: 300 }} />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" loading={submitting}>
              Gửi yêu cầu
            </Button>
          </Form.Item>
        </Form>
      </Card>

      <Card title="Lịch sử yêu cầu của bạn">
        <Table
          dataSource={requests}
          columns={columns}
          rowKey="id"
          loading={loading}
          pagination={{ pageSize: 5 }}
        />
      </Card>
    </div>
  );
};

export default UserDashboard;
