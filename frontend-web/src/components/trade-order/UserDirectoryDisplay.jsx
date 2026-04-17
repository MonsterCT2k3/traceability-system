import React, { useEffect, useState } from 'react';
import { Typography, Spin } from 'antd';
import api from '../../lib/api';

const { Text } = Typography;

function formatNameThenId(u, fallbackId) {
  if (!u) return fallbackId;
  const name = (u.fullName && u.fullName.trim()) || (u.username && u.username.trim());
  const id = u.id || fallbackId;
  return name ? `${name} (${id})` : `(${id})`;
}

/**
 * Hiển thị người dùng dạng "Tên (uuid)" — gọi identity directory by-id.
 * Sao chép mặc định là chuỗi hiển thị đầy đủ.
 */
const UserDirectoryDisplay = ({ userId }) => {
  const [line, setLine] = useState(null);
  const [loading, setLoading] = useState(!!userId);

  useEffect(() => {
    if (!userId) {
      setLine(null);
      setLoading(false);
      return;
    }
    let cancelled = false;
    setLoading(true);
    api
      .get(`/identity/api/v1/users/directory/by-id/${encodeURIComponent(userId)}`)
      .then((res) => {
        const u = res.data?.result;
        if (!cancelled) {
          setLine(formatNameThenId(u, userId));
        }
      })
      .catch(() => {
        if (!cancelled) {
          setLine(userId);
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [userId]);

  if (!userId) {
    return '—';
  }
  if (loading) {
    return <Spin size="small" />;
  }
  return <Text copyable={{ text: line || userId }}>{line || userId}</Text>;
};

export default UserDirectoryDisplay;
