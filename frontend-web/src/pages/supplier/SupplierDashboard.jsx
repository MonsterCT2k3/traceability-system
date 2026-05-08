import React, { useState, useEffect } from 'react';
import { Layout, Menu, Button, Typography, theme, Avatar, Dropdown, Space } from 'antd';
import {
  DashboardOutlined,
  GoldOutlined,
  SwapOutlined,
  ShoppingCartOutlined,
  LogoutOutlined,
  UserOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { jwtDecode } from 'jwt-decode';
import api from '../../lib/api';
import SupplierOverview from './components/SupplierOverview';
import SupplierRawBatchManagement from './components/SupplierRawBatchManagement';
import SupplierOrderManagement from './components/orders/SupplierOrderManagement';

const { Header, Sider, Content } = Layout;
const { Title, Text } = Typography;

const SupplierDashboard = () => {
  const [collapsed, setCollapsed] = useState(false);
  const [selectedKey, setSelectedKey] = useState('dashboard');
  const navigate = useNavigate();
  const {
    token: { colorBgContainer, borderRadiusLG },
  } = theme.useToken();

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

  const [avatarUrl, setAvatarUrl] = useState(null);

  useEffect(() => {
    const fetchProfile = async () => {
      try {
        const response = await api.get('/identity/api/v1/users/profile');
        if (response.data?.result?.avatarUrl) {
          setAvatarUrl(response.data.result.avatarUrl);
        }
      } catch (error) {
        console.error('Lỗi khi lấy thông tin profile:', error);
      }
    };
    if (token) {
      fetchProfile();
    }
  }, [token]);

  const handleLogout = () => {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('userRole');
    navigate('/login');
  };

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
      label: 'Đơn đặt hàng',
    },
  ];

  const renderContent = () => {
    switch (selectedKey) {
      case 'dashboard':
        return <SupplierOverview />;
      case 'raw-batches':
        return <SupplierRawBatchManagement />;
      case 'orders':
        return <SupplierOrderManagement />;
      default:
        return <SupplierOverview />;
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
    <>
      <style>{`
        ::-webkit-scrollbar {
          display: none !important;
          width: 0 !important;
          background: transparent !important;
        }
        * {
          -ms-overflow-style: none !important;
          scrollbar-width: none !important;
        }
      `}</style>
      <Layout style={{ height: '100vh', overflow: 'hidden' }}>
      <Sider trigger={null} collapsible collapsed={collapsed} theme="light">
        <div
          style={{
            height: 64,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            borderBottom: '1px solid #f0f0f0',
          }}
        >
          <Title level={4} style={{ margin: 0, color: '#52c41a', transition: 'all 0.3s' }}>
            {collapsed ? 'TS' : 'Supplier'}
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
      <Layout style={{ display: 'flex', flexDirection: 'column' }}>
        <Header
          style={{
            padding: '0 24px 0 0',
            background: colorBgContainer,
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            boxShadow: '0 1px 4px rgba(0,21,41,.08)',
            zIndex: 1,
            flexShrink: 0, // Prevent header from shrinking
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
              <Avatar src={avatarUrl} style={{ backgroundColor: '#52c41a' }}>
                {!avatarUrl && (username ? username.charAt(0).toUpperCase() : <UserOutlined />)}
              </Avatar>
              <Text strong>{username}</Text>
            </Space>
          </Dropdown>
        </Header>
        <Content
          className="hide-scrollbar"
          style={{
            margin: '24px 16px',
            padding: 24,
            background: colorBgContainer,
            borderRadius: borderRadiusLG,
            overflowY: 'auto', // This makes only the content area scrollable
            flex: 1, // Take up remaining height
          }}
        >
          {renderContent()}
        </Content>
      </Layout>
    </Layout>
    </>
  );
};

export default SupplierDashboard;
