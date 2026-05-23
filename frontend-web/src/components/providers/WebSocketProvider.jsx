import React, { createContext, useContext, useEffect, useState } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { notification } from 'antd';
import { jwtDecode } from 'jwt-decode';
import { useQueryClient } from '@tanstack/react-query';

const WebSocketContext = createContext(null);

export const WebSocketProvider = ({ children }) => {
  const [client, setClient] = useState(null);
  const queryClient = useQueryClient();

  useEffect(() => {
    const token = localStorage.getItem('accessToken');
    if (!token) return;

    let userId = null;
    try {
      const decoded = jwtDecode(token);
      userId = decoded.userId;
    } catch (e) {
      console.error('Invalid token', e);
      return;
    }

    if (!userId) return;

    // Khởi tạo STOMP Client (kết nối qua API Gateway trên port 8080)
    // Product Service đang lắng nghe ở path /ws, qua Gateway: /product/ws (Gateway sẽ strip /product)
    const socket = new SockJS('http://localhost:8080/product/ws');
    const stompClient = new Client({
      webSocketFactory: () => socket,
      reconnectDelay: 5000,
      debug: (str) => {
        // console.log(str);
      },
      onConnect: () => {
        console.log('Connected to WebSocket!');
        // Subscribe vào topic dành riêng cho user này
        stompClient.subscribe(`/topic/blockchain-updates/${userId}`, (message) => {
          if (message.body) {
            const data = JSON.parse(message.body);
            console.log('Received WebSocket message:', data);

            if (data.success) {
              notification.success({
                message: 'Blockchain Xác Nhận Thành Công!',
                description: `Mã băm (Hash): ${data.txHash}`,
                duration: 5,
              });
              // Refetch lại dữ liệu để giao diện tự cập nhật mã hash
              queryClient.invalidateQueries({ queryKey: ['rawBatches'] });
              queryClient.invalidateQueries({ queryKey: ['pallets'] });
            } else {
              notification.error({
                message: 'Ghi Blockchain Thất Bại',
                description: `Lỗi: ${data.error}`,
                duration: 0, // Không tự tắt
              });
            }
          }
        });
      },
      onStompError: (frame) => {
        console.error('Broker reported error: ' + frame.headers['message']);
        console.error('Additional details: ' + frame.body);
      },
    });

    stompClient.activate();
    setClient(stompClient);

    return () => {
      stompClient.deactivate();
    };
  }, [queryClient]);

  return (
    <WebSocketContext.Provider value={client}>
      {children}
    </WebSocketContext.Provider>
  );
};

export const useWebSocket = () => {
  return useContext(WebSocketContext);
};
