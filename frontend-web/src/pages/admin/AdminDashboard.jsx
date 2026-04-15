import React, { useState, useEffect } from 'react';
import { Card, Button, Table, Tag, message, Typography, Space, Popconfirm } from 'antd';
import { useNavigate } from 'react-router-dom';
import api from '../../lib/api';

const { Title } = Typography;

const AdminDashboard = () => {
  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const fetchPendingRequests = async () => {
    setLoading(true);
    try {
      const response = await api.get('/identity/api/v1/admin/role-requests/pending');
      setRequests(response.data.result || []);
    } catch (error) {
      message.error('Lỗi khi tải danh sách yêu cầu');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchPendingRequests();
  }, []);

  const handleApprove = async (id) => {
    try {
      await api.post(`/identity/api/v1/admin/role-requests/${id}/approve`);
      message.success('Đã duyệt yêu cầu thành công');
      fetchPendingRequests();
    } catch (error) {
      message.error(error.response?.data?.message || 'Lỗi khi duyệt yêu cầu');
    }
  };

  const handleReject = async (id) => {
    try {
      await api.post(`/identity/api/v1/admin/role-requests/${id}/reject`);
      message.success('Đã từ chối yêu cầu');
      fetchPendingRequests();
    } catch (error) {
      message.error(error.response?.data?.message || 'Lỗi khi từ chối yêu cầu');
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
      title: 'Tài khoản',
      dataIndex: 'username',
      key: 'username',
    },
    {
      title: 'Họ và tên',
      dataIndex: 'fullName',
      key: 'fullName',
    },
    {
      title: 'Vai trò xin cấp',
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
      title: 'Ngày gửi',
      dataIndex: 'createdAt',
      key: 'createdAt',
      render: (date) => new Date(date).toLocaleString('vi-VN'),
    },
    {
      title: 'Thao tác',
      key: 'action',
      render: (_, record) => (
        <Space size="middle">
          <Popconfirm
            title="Bạn có chắc chắn muốn duyệt đơn này?"
            onConfirm={() => handleApprove(record.id)}
            okText="Đồng ý"
            cancelText="Hủy"
          >
            <Button type="primary" style={{ backgroundColor: '#52c41a' }}>Duyệt</Button>
          </Popconfirm>
          <Popconfirm
            title="Bạn có chắc chắn muốn từ chối đơn này?"
            onConfirm={() => handleReject(record.id)}
            okText="Đồng ý"
            cancelText="Hủy"
          >
            <Button type="primary" danger>Từ chối</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div style={{ padding: '24px', maxWidth: '1200px', margin: '0 auto' }}>
      <Card style={{ marginBottom: '24px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Title level={3}>Admin Dashboard</Title>
          <Button type="primary" danger onClick={handleLogout}>Đăng xuất</Button>
        </div>
      </Card>

      <Card title="Danh sách yêu cầu cấp quyền đang chờ duyệt">
        <Table
          dataSource={requests}
          columns={columns}
          rowKey="id"
          loading={loading}
          pagination={{ pageSize: 10 }}
        />
      </Card>
    </div>
  );
};

export default AdminDashboard;
