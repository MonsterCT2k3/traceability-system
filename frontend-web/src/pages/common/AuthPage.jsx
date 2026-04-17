import React, { useState } from 'react';
import { Form, Input, Button, Card, message, Tabs, Typography } from 'antd';
import { UserOutlined, LockOutlined, IdcardOutlined, MailOutlined, PhoneOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { jwtDecode } from 'jwt-decode';
import api from '../../lib/api';
import {
  isWebAllowedRole,
  MOBILE_ONLY_ROLES,
  normalizeRole,
} from '../../constants/platformRoles';
import './Auth.css';

const { Title, Text } = Typography;

const AuthPage = () => {
  const [activeTab, setActiveTab] = useState('login');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const onLogin = async (values) => {
    setLoading(true);
    try {
      const response = await api.post('/identity/api/v1/auth/login', {
        username: values.username,
        password: values.password,
      });

      const { accessToken, refreshToken } = response.data.result;
      const decoded = jwtDecode(accessToken);
      const role = normalizeRole(decoded.role);

      if (!isWebAllowedRole(role)) {
        if (MOBILE_ONLY_ROLES.includes(role)) {
          /** Không lưu token web; chuyển sang màn riêng rồi mới thông báo (xem MobileOnlyNotice). */
          navigate('/mobile-only', { replace: true, state: { fromLogin: true } });
          return;
        }
        message.error('Phiên bản web không hỗ trợ vai trò này.');
        return;
      }

      localStorage.setItem('accessToken', accessToken);
      localStorage.setItem('refreshToken', refreshToken);
      localStorage.setItem('userRole', role);

      message.success(`Đăng nhập thành công! Vai trò: ${role}`);

      if (role === 'ADMIN') {
        navigate('/admin');
      } else if (role === 'MANUFACTURER') {
        navigate('/manufacture');
      } else if (role === 'SUPPLIER') {
        navigate('/supplier');
      } else {
        navigate('/user');
      }
    } catch (error) {
      const errorMsg = error.response?.data?.message || "Tài khoản hoặc mật khẩu không đúng!";
      message.error(errorMsg);
    } finally {
      setLoading(false);
    }
  };

  const onRegister = async (values) => {
    setLoading(true);
    try {
      await api.post('/identity/api/v1/auth/register', {
        username: values.username,
        password: values.password,
        fullName: values.fullName,
        email: values.email,
        phone: values.phone,
      });
      
      message.success('Đăng ký thành công! Vui lòng đăng nhập.');
      setActiveTab('login'); // Chuyển về tab đăng nhập
    } catch (error) {
      const errorMsg = error.response?.data?.message || "Đăng ký thất bại!";
      message.error(errorMsg);
    } finally {
      setLoading(false);
    }
  };

  const items = [
    {
      key: 'login',
      label: 'Đăng nhập',
      children: (
        <Form name="login_form" onFinish={onLogin} layout="vertical" size="large">
          <Form.Item name="username" rules={[{ required: true, message: 'Vui lòng nhập tài khoản!' }]}>
            <Input prefix={<UserOutlined className="site-form-item-icon" />} placeholder="Tài khoản" />
          </Form.Item>
          <Form.Item name="password" rules={[{ required: true, message: 'Vui lòng nhập mật khẩu!' }]}>
            <Input.Password prefix={<LockOutlined className="site-form-item-icon" />} placeholder="Mật khẩu" />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" block loading={loading} className="auth-btn">
              ĐĂNG NHẬP
            </Button>
          </Form.Item>
        </Form>
      ),
    },
    {
      key: 'register',
      label: 'Đăng ký',
      children: (
        <Form name="register_form" onFinish={onRegister} layout="vertical" size="large">
          <Form.Item name="fullName" rules={[{ required: true, message: 'Vui lòng nhập họ tên!' }]}>
            <Input prefix={<IdcardOutlined className="site-form-item-icon" />} placeholder="Họ và tên" />
          </Form.Item>
          <Form.Item name="email" rules={[{ required: true, type: 'email', message: 'Vui lòng nhập email hợp lệ!' }]}>
            <Input prefix={<MailOutlined className="site-form-item-icon" />} placeholder="Email" />
          </Form.Item>
          <Form.Item name="phone" rules={[{ required: true, message: 'Vui lòng nhập số điện thoại!' }]}>
            <Input prefix={<PhoneOutlined className="site-form-item-icon" />} placeholder="Số điện thoại" />
          </Form.Item>
          <Form.Item name="username" rules={[{ required: true, message: 'Vui lòng nhập tài khoản!' }]}>
            <Input prefix={<UserOutlined className="site-form-item-icon" />} placeholder="Tài khoản" />
          </Form.Item>
          <Form.Item name="password" rules={[{ required: true, message: 'Vui lòng nhập mật khẩu!' }]}>
            <Input.Password prefix={<LockOutlined className="site-form-item-icon" />} placeholder="Mật khẩu" />
          </Form.Item>
          <Form.Item 
            name="confirmPassword" 
            dependencies={['password']}
            rules={[
              { required: true, message: 'Vui lòng xác nhận mật khẩu!' },
              ({ getFieldValue }) => ({
                validator(_, value) {
                  if (!value || getFieldValue('password') === value) {
                    return Promise.resolve();
                  }
                  return Promise.reject(new Error('Mật khẩu xác nhận không khớp!'));
                },
              }),
            ]}
          >
            <Input.Password prefix={<LockOutlined className="site-form-item-icon" />} placeholder="Xác nhận mật khẩu" />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" block loading={loading} className="auth-btn">
              ĐĂNG KÝ
            </Button>
          </Form.Item>
        </Form>
      ),
    },
  ];

  return (
    <div className="auth-container">
      <div className="auth-background"></div>
      <Card className="auth-card" bordered={false}>
        <div className="auth-header">
          <Title level={3} className="auth-title">Traceability System</Title>
          <Text type="secondary">Hệ thống truy xuất nguồn gốc</Text>
        </div>
        <Tabs 
          activeKey={activeTab} 
          onChange={setActiveTab} 
          centered 
          items={items} 
          className="auth-tabs"
        />
      </Card>
    </div>
  );
};

export default AuthPage;
