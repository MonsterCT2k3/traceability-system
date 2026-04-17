import React, { useEffect, useState } from 'react';
import { Result, Button, message } from 'antd';
import { useLocation, useNavigate } from 'react-router-dom';
import { AndroidOutlined } from '@ant-design/icons';
import './MobileOnlyNotice.css';

const SHOW_DELAY_MS = 450;

/**
 * Retailer / Transporter trên web.
 * Nếu vào từ đăng nhập (state.fromLogin): màn trống trước, sau đó mới toast + nội dung.
 * Trường hợp khác (token cũ, redirect): hiển thị nội dung ngay.
 */
const MobileOnlyNotice = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const fromLogin = location.state?.fromLogin === true;
  const [showPanel, setShowPanel] = useState(!fromLogin);

  useEffect(() => {
    if (!fromLogin) {
      return undefined;
    }
    const timer = window.setTimeout(() => {
      message.warning({
        content: 'Vui lòng đăng nhập bằng ứng dụng di động (mobile app).',
        duration: 6,
      });
      setShowPanel(true);
    }, SHOW_DELAY_MS);
    return () => window.clearTimeout(timer);
  }, [fromLogin]);

  const clearAndGoLogin = () => {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('userRole');
    navigate('/login', { replace: true });
  };

  if (!showPanel) {
    return <div className="mobile-only-notice" aria-hidden />;
  }

  return (
    <div className="mobile-only-notice">
      <Result
        icon={<AndroidOutlined style={{ fontSize: 72, color: '#1677ff' }} />}
        title="Vui lòng dùng ứng dụng di động"
        subTitle="Vai trò của bạn (Retailer / Transporter) chỉ hỗ trợ trên mobile app. Hãy đăng nhập bằng ứng dụng Traceability trên điện thoại."
        extra={
          <Button type="primary" size="large" onClick={clearAndGoLogin}>
            Quay lại đăng nhập
          </Button>
        }
      />
    </div>
  );
};

export default MobileOnlyNotice;
