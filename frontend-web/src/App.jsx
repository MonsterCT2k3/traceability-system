import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import AuthPage from './pages/common/AuthPage';
// Import các trang sẽ tạo ở bước sau
import AdminDashboard from './pages/admin/AdminDashboard';
import ManufactureDashboard from './pages/manufacture/ManufactureDashboard';
import SupplierDashboard from './pages/supplier/SupplierDashboard';
import UserDashboard from './pages/user/UserDashboard';
import Unauthorized from './pages/common/Unauthorized';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false, // Tắt tự động gọi lại API khi focus lại cửa sổ (tuỳ chọn)
      retry: 1, // Chỉ thử lại 1 lần nếu lỗi
    },
  },
});

// 1. Tạo một "Lính canh" (Route Guard) để kiểm tra đăng nhập và Role
const PrivateRoute = ({ element, allowedRoles }) => {
  const token = localStorage.getItem('accessToken');
  const userRole = localStorage.getItem('userRole');

  // Chưa có token -> đá về trang Login
  if (!token) {
    return <Navigate to="/login" replace />;
  }

  // Nếu route yêu cầu Role cụ thể, mà userRole không nằm trong danh sách cho phép -> đá sang trang Lỗi
  if (allowedRoles && !allowedRoles.includes(userRole)) {
    return <Navigate to="/unauthorized" replace />;
  }

  // Token và Role hợp lệ -> cho phép xem
  return element;
};

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <Routes>
          {/* Đường dẫn mặc định (/) tạm thời chuyển thẳng đến Login */}
          <Route path="/" element={<Navigate to="/login" replace />} />

          <Route path="/login" element={<AuthPage />} />
          <Route path="/unauthorized" element={<Unauthorized />} />

          {/* Các trang yêu cầu đăng nhập */}
          <Route
            path="/admin"
            element={<PrivateRoute element={<AdminDashboard />} allowedRoles={['ADMIN']} />}
          />

          <Route
            path="/manufacture"
            element={<PrivateRoute element={<ManufactureDashboard />} allowedRoles={['ADMIN', 'MANUFACTURER']} />}
          />

          <Route
            path="/supplier"
            element={<PrivateRoute element={<SupplierDashboard />} allowedRoles={['ADMIN', 'SUPPLIER']} />}
          />

          <Route
            path="/user"
            element={<PrivateRoute element={<UserDashboard />} allowedRoles={['USER', 'RETAILER', 'TRANSPORTER']} />}
          />
        </Routes>
      </BrowserRouter>
    </QueryClientProvider>
  );
}

export default App;