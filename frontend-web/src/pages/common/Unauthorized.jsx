import React from 'react';
import { Result, Button } from 'antd';
import { useNavigate } from 'react-router-dom';

const Unauthorized = () => {
  const navigate = useNavigate();
  return (
    <Result
      status="403"
      title="403 - Từ chối truy cập"
      subTitle="Xin lỗi, bạn không có quyền truy cập vào trang này."
      extra={<Button type="primary" onClick={() => navigate('/login')}>Quay lại đăng nhập</Button>}
    />
  );
};

export default Unauthorized;
