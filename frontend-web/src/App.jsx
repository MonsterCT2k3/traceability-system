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
import MobileOnlyNotice from './pages/common/MobileOnlyNotice';
import {
  isWebAllowedRole,
  MOBILE_ONLY_ROLES,
  normalizeRole,
} from './constants/platformRoles';

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
  const normalized = normalizeRole(userRole);

  if (!token) {
    return <Navigate to="/login" replace />;
  }

  /** Retailer / Transporter không dùng web — chỉ mobile. */
  if (!isWebAllowedRole(normalized)) {
    if (MOBILE_ONLY_ROLES.includes(normalized)) {
      return <Navigate to="/mobile-only" replace />;
    }
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('userRole');
    return <Navigate to="/login" replace />;
  }

  if (allowedRoles && !allowedRoles.includes(normalized)) {
    return <Navigate to="/unauthorized" replace />;
  }

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
          <Route path="/mobile-only" element={<MobileOnlyNotice />} />

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
            element={<PrivateRoute element={<UserDashboard />} allowedRoles={['USER']} />}
          />
        </Routes>
      </BrowserRouter>
    </QueryClientProvider>
  );
}

export default App;