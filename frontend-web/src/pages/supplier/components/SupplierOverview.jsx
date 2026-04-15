import React, { useState } from 'react';
import {
  Button,
  Typography,
  Upload,
  message,
  Divider,
  Descriptions,
  Form,
  Input,
  Tag,
  Space,
  Avatar,
} from 'antd';
import { UserOutlined, UploadOutlined } from '@ant-design/icons';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '../../../lib/api';

const { Title, Text } = Typography;

const SupplierOverview = () => {
  const queryClient = useQueryClient();
  const [isEditing, setIsEditing] = useState(false);
  const [locationLoading, setLocationLoading] = useState(false);
  const [form] = Form.useForm();

  const { data: profile, isLoading } = useQuery({
    queryKey: ['userProfile'],
    queryFn: async () => {
      const response = await api.get('/identity/api/v1/users/profile');
      return response.data.result;
    },
    staleTime: 5 * 60 * 1000,
  });

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
      onSuccess: () => onSuccess('Ok'),
      onError: (err) => onError({ error: err }),
    });
  };

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
    },
  });

  const onUpdateProfile = (values) => {
    profileMutation.mutate(values);
  };

  const handleGetLocation = () => {
    setLocationLoading(true);
    if ('geolocation' in navigator) {
      navigator.geolocation.getCurrentPosition(
        async (position) => {
          const { latitude, longitude } = position.coords;
          try {
            const response = await fetch(
              `https://nominatim.openstreetmap.org/reverse?format=json&lat=${latitude}&lon=${longitude}&accept-language=vi`
            );
            const data = await response.json();

            if (data && data.display_name) {
              form.setFieldsValue({ location: data.display_name });
              message.success('Đã lấy được địa chỉ hiện tại');
            } else {
              form.setFieldsValue({ location: `${latitude}, ${longitude}` });
              message.success('Đã lấy được toạ độ hiện tại');
            }
          } catch (error) {
            console.error('Geocoding error:', error);
            form.setFieldsValue({ location: `${latitude}, ${longitude}` });
            message.warning('Chỉ lấy được toạ độ do lỗi kết nối dịch vụ bản đồ.');
          } finally {
            setLocationLoading(false);
          }
        },
        () => {
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
      <Title level={4}>Tổng quan Nhà cung cấp</Title>
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
              <Button icon={<UploadOutlined />} loading={avatarMutation.isPending}>
                Đổi ảnh đại diện
              </Button>
            </Upload>
            <div style={{ marginTop: '10px', color: '#888', fontSize: '12px' }}>Định dạng: JPG, PNG</div>
          </div>
          <div style={{ flex: 1 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
              <Title level={5}>Chi tiết tài khoản</Title>
              {!isEditing && (
                <Button type="primary" onClick={handleEditClick}>
                  Chỉnh sửa thông tin
                </Button>
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
                  <Button type="primary" htmlType="submit" loading={profileMutation.isPending}>
                    Lưu thay đổi
                  </Button>
                  <Button onClick={() => setIsEditing(false)}>Hủy</Button>
                </Space>
              </Form>
            ) : (
              <Descriptions bordered column={1}>
                <Descriptions.Item label="Tài khoản">{profile.username}</Descriptions.Item>
                <Descriptions.Item label="Họ và tên">{profile.fullName}</Descriptions.Item>
                <Descriptions.Item label="Email">{profile.email || 'Chưa cập nhật'}</Descriptions.Item>
                <Descriptions.Item label="Số điện thoại">{profile.phone || 'Chưa cập nhật'}</Descriptions.Item>
                <Descriptions.Item label="Vai trò">
                  <Tag color="blue">{profile.role}</Tag>
                </Descriptions.Item>
                <Descriptions.Item label="Mô tả doanh nghiệp">{profile.description || 'Chưa có mô tả'}</Descriptions.Item>
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

export default SupplierOverview;
