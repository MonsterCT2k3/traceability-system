import axios from 'axios';

// Tạo một "bản sao" của axios với URL mặc định lấy từ file .env
const api = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL,
    headers: {
        'Content-Type': 'application/json',
    },
});

// Interceptor: Trước khi gửi bất kỳ request nào đi, nó sẽ chạy đoạn này
api.interceptors.request.use(
    (config) => {
        // Lấy token từ LocalStorage (nơi mình sẽ lưu khi đăng nhập thành công)
        const token = localStorage.getItem('accessToken');

        // Nếu có token, nhét nó vào header Authorization
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

// Interceptor: Khi nhận response về, nếu lỗi 401 (hết hạn token) thì xử lý
api.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error.response && error.response.status === 401) {
            // Tạm thời: Nếu lỗi 401 thì xóa token và bắt đăng nhập lại
            // (Sau này mình sẽ làm tính năng tự động refresh token ở đây)
            console.log("Token hết hạn hoặc không hợp lệ");
            localStorage.removeItem('accessToken');
            localStorage.removeItem('refreshToken');
            // window.location.href = '/login'; // Bật dòng này sau khi có router
        }
        return Promise.reject(error);
    }
);

export default api;