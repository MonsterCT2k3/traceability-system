import React, { useState, useEffect } from 'react';
import { Layout, Menu, Button, Typography, theme, Avatar, Dropdown, Space, Upload, message, Divider, Descriptions, Form, Input, Tag, DatePicker, Select, Card, Table } from 'antd';
import {
  DashboardOutlined,
  AppstoreAddOutlined,
  GoldOutlined,
  InboxOutlined,
  SwapOutlined,
  ShoppingCartOutlined,
  LogoutOutlined,
  UserOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  UploadOutlined
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { jwtDecode } from 'jwt-decode';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '../../lib/api';
import ManufactureOrderManagement from './components/orders/ManufactureOrderManagement';
import ManufactureRetailOrdersManagement from './components/orders/ManufactureRetailOrdersManagement';
import ProductManagement from './components/products/ProductManagement';
import ManufactureProductionManagement from './components/production/ManufactureProductionManagement';
import GoodsManagement from './components/goods/GoodsManagement';

const { Header, Sider, Content } = Layout;
const { Title, Text } = Typography;

// --- Các Component con ---
const Overview = () => {
  const queryClient = useQueryClient();
  const [isEditing, setIsEditing] = useState(false);
  const [locationLoading, setLocationLoading] = useState(false);
  const [form] = Form.useForm();

  // 1. Dùng useQuery để lấy dữ liệu profile
  const { data: profile, isLoading } = useQuery({
    queryKey: ['userProfile'],
    queryFn: async () => {
      const response = await api.get('/identity/api/v1/users/profile');
      return response.data.result;
    },
    staleTime: 5 * 60 * 1000, // Dữ liệu được coi là "mới" trong 5 phút, không cần gọi lại API
  });

  // 2. Dùng useMutation để update avatar
  const avatarMutation = useMutation({
    mutationFn: async (file) => {
      const formData = new FormData();
      formData.append('file', file);
      await api.post('/identity/api/v1/users/profile/avatar', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });
    },
    onSuccess: () => {
      message.success('Cập nhật ảnh đại diện thành công!');
      queryClient.invalidateQueries({ queryKey: ['userProfile'] });
    },
    onError: (error) => {
      message.error(error.response?.data?.message || 'Lỗi khi tải ảnh lên');
    },
  });

  const handleAvatarUpload = ({ file, onSuccess, onError }) => {
    avatarMutation.mutate(file, {
      onSuccess: () => onSuccess("Ok"),
      onError: (err) => onError({ error: err })
    });
  };

  // 3. Dùng useMutation để update profile
  const profileMutation = useMutation({
    mutationFn: async (values) => {
      await api.put('/identity/api/v1/users/profile', values);
    },
    onSuccess: () => {
      message.success('Cập nhật thông tin thành công!');
      setIsEditing(false);
      queryClient.invalidateQueries({ queryKey: ['userProfile'] });
    },
    onError: (error) => {
      message.error(error.response?.data?.message || 'Lỗi khi cập nhật thông tin');
    }
  });

  const onUpdateProfile = (values) => {
    profileMutation.mutate(values);
  };

  const handleGetLocation = () => {
    setLocationLoading(true);
    if ("geolocation" in navigator) {
      navigator.geolocation.getCurrentPosition(
        async (position) => {
          const { latitude, longitude } = position.coords;
          try {
            const response = await fetch(`https://nominatim.openstreetmap.org/reverse?format=json&lat=${latitude}&lon=${longitude}&accept-language=vi`);
            const data = await response.json();
            
            if (data && data.display_name) {
              form.setFieldsValue({ location: data.display_name });
              message.success('Đã lấy được địa chỉ hiện tại');
            } else {
              form.setFieldsValue({ location: `${latitude}, ${longitude}` });
              message.success('Đã lấy được toạ độ hiện tại');
            }
          } catch (error) {
            console.error("Geocoding error:", error);
            form.setFieldsValue({ location: `${latitude}, ${longitude}` });
            message.warning('Chỉ lấy được toạ độ do lỗi kết nối dịch vụ bản đồ.');
          } finally {
            setLocationLoading(false);
          }
        },
        (error) => {
          setLocationLoading(false);
          message.error('Không thể lấy vị trí. Vui lòng cấp quyền hoặc bật định vị.');
        },
        { enableHighAccuracy: true, timeout: 10000, maximumAge: 0 }
      );
    } else {
      setLocationLoading(false);
      message.error('Trình duyệt của bạn không hỗ trợ định vị.');
    }
  };

  const handleEditClick = () => {
    if (profile) {
      form.setFieldsValue({
        email: profile.email,
        phone: profile.phone,
        description: profile.description,
        location: profile.location,
      });
    }
    setIsEditing(true);
  };

  if (isLoading) {
    return <Text>Đang tải thông tin...</Text>;
  }

  return (
    <div>
      <Title level={4}>Tổng quan Nhà sản xuất</Title>
      <Text>Thông tin tài khoản của bạn.</Text>
      <Divider />
      {profile ? (
        <div style={{ display: 'flex', gap: '40px', flexWrap: 'wrap' }}>
          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', minWidth: '200px' }}>
            <Avatar 
              size={120} 
              src={profile.avatarUrl} 
              icon={!profile.avatarUrl && <UserOutlined />} 
              style={{ marginBottom: '20px', border: '2px solid #f0f0f0' }}
            />
            <Upload customRequest={handleAvatarUpload} showUploadList={false} accept="image/*">
              <Button icon={<UploadOutlined />} loading={avatarMutation.isPending}>Đổi ảnh đại diện</Button>
            </Upload>
            <div style={{ marginTop: '10px', color: '#888', fontSize: '12px' }}>
              Định dạng: JPG, PNG
            </div>
          </div>
          <div style={{ flex: 1 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
              <Title level={5}>Chi tiết tài khoản</Title>
              {!isEditing && (
                <Button type="primary" onClick={handleEditClick}>Chỉnh sửa thông tin</Button>
              )}
            </div>
            
            {isEditing ? (
              <Form form={form} layout="vertical" onFinish={onUpdateProfile}>
                <Form.Item label="Email" name="email" rules={[{ type: 'email', message: 'Email không hợp lệ' }]}>
                  <Input placeholder="Nhập email" />
                </Form.Item>
                <Form.Item label="Số điện thoại" name="phone">
                  <Input placeholder="Nhập số điện thoại" />
                </Form.Item>
                <Form.Item label="Mô tả doanh nghiệp" name="description">
                  <Input.TextArea rows={4} placeholder="Mô tả về doanh nghiệp của bạn" />
                </Form.Item>
                <Form.Item label="Địa điểm (Vị trí mặc định)" name="location">
                  <Input 
                    placeholder="VD: Xã An Khánh, Hoài Đức, Hà Nội" 
                    addonAfter={
                      <Button 
                        type="link" 
                        size="small" 
                        loading={locationLoading} 
                        onClick={handleGetLocation}
                        style={{ margin: '-4px -11px' }}
                      >
                        Lấy vị trí
                      </Button>
                    }
                  />
                </Form.Item>
                <Space>
                  <Button type="primary" htmlType="submit" loading={profileMutation.isPending}>Lưu thay đổi</Button>
                  <Button onClick={() => setIsEditing(false)}>Hủy</Button>
                </Space>
              </Form>
            ) : (
              <Descriptions bordered column={1}>
                <Descriptions.Item label="Tài khoản">{profile.username}</Descriptions.Item>
                <Descriptions.Item label="Họ và tên">{profile.fullName}</Descriptions.Item>
                <Descriptions.Item label="Email">{profile.email || 'Chưa cập nhật'}</Descriptions.Item>
                <Descriptions.Item label="Số điện thoại">{profile.phone || 'Chưa cập nhật'}</Descriptions.Item>
                <Descriptions.Item label="Vai trò"><Tag color="blue">{profile.role}</Tag></Descriptions.Item>
                <Descriptions.Item label="Mô tả doanh nghiệp">
                  {profile.description || 'Chưa có mô tả'}
                </Descriptions.Item>
                <Descriptions.Item label="Địa điểm (Vị trí mặc định)">
                  {profile.location || 'Chưa cập nhật'}
                </Descriptions.Item>
              </Descriptions>
            )}
          </div>
        </div>
      ) : (
        <Text>Không có dữ liệu</Text>
      )}
    </div>
  );
};

const { Option } = Select;

const RawBatchManagement = () => {
  const [form] = Form.useForm();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isCreating, setIsCreating] = useState(false);
  const queryClient = useQueryClient();

  // 1. Fetch danh sách Lô nguyên liệu
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

  const onFinish = async (values) => {
    setIsSubmitting(true);
    try {
      const harvestedAt = values.harvestedAt ? values.harvestedAt.format('YYYY-MM-DD') : '';
      
      const payload = {
        ...values,
        harvestedAt
      };
      
      const response = await api.post('/product/api/v1/raw-batches', payload);
      message.success('Tạo lô nguyên liệu thành công! (TxHash: ' + response.data.result.txHash + ')');
      form.resetFields();
      setIsCreating(false);
      queryClient.invalidateQueries({ queryKey: ['myRawBatches'] });
    } catch (error) {
      message.error(error.response?.data?.message || 'Lỗi khi tạo lô nguyên liệu');
    } finally {
      setIsSubmitting(false);
    }
  };

  const columns = [
    {
      title: 'Mã lô',
      dataIndex: 'rawBatchCode',
      key: 'rawBatchCode',
      render: (text) => <strong>{text}</strong>,
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
          <Title level={4} style={{ margin: 0 }}>Tạo mới Lô nguyên liệu</Title>
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
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <div>
          <Title level={4} style={{ margin: 0 }}>Quản lý Lô nguyên liệu</Title>
          <Text>Danh sách các lô nguyên liệu bạn đã tạo trên hệ thống.</Text>
        </div>
        <Button type="primary" onClick={() => setIsCreating(true)}>+ Tạo lô nguyên liệu</Button>
      </div>
      <Divider style={{ marginTop: 8 }} />
      
      <Card>
        <Table 
          columns={columns} 
          dataSource={rawBatches} 
          rowKey="id" 
          loading={isLoading}
          pagination={{ pageSize: 10 }}
        />
      </Card>
    </div>
  );
};

const PackagingManagement = () => (
  <div>
    <Title level={4}>Quản lý Đóng gói</Title>
    <Text>Giao diện đóng gói thùng (Carton) và Pallet.</Text>
  </div>
);

const TransferManagement = () => (
  <div>
    <Title level={4}>Quản lý Vận chuyển</Title>
    <Text>Giao diện xuất/nhập kho và chuyển giao quyền sở hữu (Transfer).</Text>
  </div>
);
// -----------------------------------------------------------------

const ManufactureDashboard = () => {
  const [collapsed, setCollapsed] = useState(false);
  const [selectedKey, setSelectedKey] = useState('dashboard');
  const navigate = useNavigate();
  const {
    token: { colorBgContainer, borderRadiusLG },
  } = theme.useToken();

  // Lấy thông tin user từ token
  const token = localStorage.getItem('accessToken');
  let userRole = '';
  let username = 'User';
  if (token) {
    try {
      const decoded = jwtDecode(token);
      userRole = decoded.role;
      username = decoded.sub;
    } catch (e) {
      console.error(e);
    }
  }

  const handleLogout = () => {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('userRole');
    navigate('/login');
  };

  // Cấu hình Menu dựa trên Role (có thể ẩn/hiện tuỳ role nếu cần)
  const menuItems = [
    {
      key: 'dashboard',
      icon: <DashboardOutlined />,
      label: 'Tổng quan',
    },
    {
      key: 'raw-batches',
      icon: <GoldOutlined />,
      label: 'Lô nguyên liệu',
    },
    {
      key: 'orders',
      icon: <ShoppingCartOutlined />,
      label: 'Đặt hàng NCC',
    },
    {
      key: 'retail-orders',
      icon: <ShoppingCartOutlined />,
      label: 'Đơn hàng',
    },
    {
      key: 'products',
      icon: <AppstoreAddOutlined />,
      label: 'Sản phẩm',
    },
    {
      key: 'production',
      icon: <InboxOutlined />,
      label: 'Sản xuất',
    },
    {
      key: 'packaging',
      icon: <InboxOutlined />,
      label: 'Đóng gói',
    },
    {
      key: 'goods',
      icon: <InboxOutlined />,
      label: 'Quản lý hàng hóa',
    },
    {
      key: 'transfers',
      icon: <SwapOutlined />,
      label: 'Vận chuyển',
    },
  ];

  // Render Component tương ứng với menu được chọn
  const renderContent = () => {
    switch (selectedKey) {
      case 'dashboard': return <Overview />;
      case 'raw-batches': return <RawBatchManagement />;
      case 'orders': return <ManufactureOrderManagement />;
      case 'retail-orders': return <ManufactureRetailOrdersManagement />;
      case 'products': return <ProductManagement />;
      case 'production': return <ManufactureProductionManagement />;
      case 'packaging': return <PackagingManagement />;
      case 'goods': return <GoodsManagement />;
      case 'transfers': return <TransferManagement />;
      default: return <Overview />;
    }
  };

  const userDropdownItems = [
    {
      key: 'info',
      label: <Text disabled>Vai trò: {userRole}</Text>,
    },
    {
      type: 'divider',
    },
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: 'Đăng xuất',
      danger: true,
      onClick: handleLogout,
    },
  ];

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider trigger={null} collapsible collapsed={collapsed} theme="light">
        <div style={{ height: 64, display: 'flex', alignItems: 'center', justifyContent: 'center', borderBottom: '1px solid #f0f0f0' }}>
          <Title level={4} style={{ margin: 0, color: '#1677ff', transition: 'all 0.3s' }}>
            {collapsed ? 'TS' : 'Traceability'}
          </Title>
        </div>
        <Menu
          theme="light"
          mode="inline"
          selectedKeys={[selectedKey]}
          onClick={({ key }) => setSelectedKey(key)}
          items={menuItems}
          style={{ borderRight: 0 }}
        />
      </Sider>
      <Layout>
        <Header
          style={{
            padding: '0 24px 0 0',
            background: colorBgContainer,
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            boxShadow: '0 1px 4px rgba(0,21,41,.08)',
            zIndex: 1,
          }}
        >
          <Button
            type="text"
            icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
            onClick={() => setCollapsed(!collapsed)}
            style={{
              fontSize: '16px',
              width: 64,
              height: 64,
            }}
          />
          
          <Dropdown menu={{ items: userDropdownItems }} placement="bottomRight">
            <Space style={{ cursor: 'pointer' }}>
              <Avatar icon={<UserOutlined />} style={{ backgroundColor: '#1677ff' }} />
              <Text strong>{username}</Text>
            </Space>
          </Dropdown>
        </Header>
        <Content
          style={{
            margin: '24px 16px',
            padding: 24,
            minHeight: 280,
            background: colorBgContainer,
            borderRadius: borderRadiusLG,
          }}
        >
          {renderContent()}
        </Content>
      </Layout>
    </Layout>
  );
};

export default ManufactureDashboard;
