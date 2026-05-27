import React, { createContext, useContext, useEffect, useState } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { message, notification } from 'antd';
import { useQueryClient } from '@tanstack/react-query';
import { AUTH_SESSION_CHANGED_EVENT } from '../../lib/authSession';

const WebSocketContext = createContext(null);
const RAW_BATCH_CREATE_MESSAGE_KEY = 'raw-batch-create';
const PENDING_RAW_BATCH_CREATION_KEY = 'pendingRawBatchCreationId';

export const WebSocketProvider = ({ children }) => {
  const [client, setClient] = useState(null);
  const [authVersion, setAuthVersion] = useState(0);
  const queryClient = useQueryClient();

  useEffect(() => {
    const refreshAuthSession = () => setAuthVersion((version) => version + 1);
    const handleStorageChange = (event) => {
      if (event.key === 'accessToken') {
        refreshAuthSession();
      }
    };

    window.addEventListener(AUTH_SESSION_CHANGED_EVENT, refreshAuthSession);
    window.addEventListener('storage', handleStorageChange);
    return () => {
      window.removeEventListener(AUTH_SESSION_CHANGED_EVENT, refreshAuthSession);
      window.removeEventListener('storage', handleStorageChange);
    };
  }, []);

  useEffect(() => {
    const token = localStorage.getItem('accessToken');
    if (!token) {
      setClient(null);
      return undefined;
    }

    // Avoid keeping long-lived SockJS requests on the MVC gateway used for REST APIs.
    const productWsUrl = import.meta.env.VITE_PRODUCT_WS_URL || 'http://localhost:8082/ws';
    const stompClient = new Client({
      webSocketFactory: () => new SockJS(productWsUrl),
      connectHeaders: {
        Authorization: `Bearer ${token}`,
      },
      reconnectDelay: 5000,
      debug: (str) => {
        // console.log(str);
      },
      onConnect: () => {
        console.log('Connected to WebSocket!');
        stompClient.subscribe('/user/queue/blockchain-updates', (stompMessage) => {
          if (stompMessage.body) {
            const data = JSON.parse(stompMessage.body);
            console.log('Received WebSocket message:', data);

            const pendingRawBatchId = sessionStorage.getItem(PENDING_RAW_BATCH_CREATION_KEY);
            const isPendingRawBatchCreation = data.type === 'RAW_BATCH'
              && (pendingRawBatchId === 'PENDING_REQUEST' || pendingRawBatchId === data.entityId);

            if (isPendingRawBatchCreation) {
              sessionStorage.removeItem(PENDING_RAW_BATCH_CREATION_KEY);
              if (data.success) {
                message.success({
                  key: RAW_BATCH_CREATE_MESSAGE_KEY,
                  content: `Tạo lô nguyên liệu thành công! TxHash: ${data.txHash}`,
                  duration: 6,
                });
              } else {
                message.error({
                  key: RAW_BATCH_CREATE_MESSAGE_KEY,
                  content: `Tạo lô nguyên liệu thất bại: ${data.error}`,
                  duration: 6,
                });
              }
              queryClient.invalidateQueries({ queryKey: ['myRawBatches'] });
              return;
            }

            if (data.success) {
              notification.success({
                message: 'Blockchain Xác Nhận Thành Công!',
                description: `Mã băm (Hash): ${data.txHash}`,
                duration: 5,
              });
              // Refetch lại dữ liệu để giao diện tự cập nhật mã hash
              queryClient.invalidateQueries({ queryKey: ['myRawBatches'] });
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

    // ==========================================
    // 2. STOMP Client for Trade Logistics Service
    // ==========================================
    const tradeWsUrl = import.meta.env.VITE_TRADE_WS_URL || 'http://localhost:8085/ws';
    const tradeStompClient = new Client({
      webSocketFactory: () => new SockJS(tradeWsUrl),
      connectHeaders: {
        Authorization: `Bearer ${token}`,
      },
      reconnectDelay: 5000,
      debug: (str) => {
        // console.log(str);
      },
      onConnect: () => {
        console.log('Connected to Trade WebSocket!');
        tradeStompClient.subscribe('/user/queue/orders', (stompMessage) => {
          if (stompMessage.body) {
            const data = JSON.parse(stompMessage.body);
            console.log('Received Trade WebSocket message:', data);

            notification.info({
              message: 'Cập nhật đơn hàng',
              description: `Đơn hàng ${data.orderId} vừa thay đổi trạng thái.`,
              duration: 3,
            });

            // Refetch orders
            queryClient.invalidateQueries({ queryKey: ['manufactureRetailIncomingOrders'] });
            queryClient.invalidateQueries({ queryKey: ['supplierIncomingOrders'] });
            // Add other query keys if needed
          }
        });
      },
      onStompError: (frame) => {
        console.error('Trade Broker reported error: ' + frame.headers['message']);
      },
    });

    tradeStompClient.activate();

    return () => {
      setClient(null);
      stompClient.deactivate();
      tradeStompClient.deactivate();
    };
  }, [queryClient, authVersion]);

  return (
    <WebSocketContext.Provider value={client}>
      {children}
    </WebSocketContext.Provider>
  );
};

export const useWebSocket = () => {
  return useContext(WebSocketContext);
};
