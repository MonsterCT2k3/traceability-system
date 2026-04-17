import React, { useCallback, useEffect, useRef, useState } from 'react';
import { Select, Spin } from 'antd';
import api from '../../../../lib/api';

/**
 * Tìm NCC theo từ khóa (tên, username, mô tả) — gọi identity directory API.
 */
const SupplierSearchSelect = ({ value, onChange, onClear }) => {
  const [options, setOptions] = useState([]);
  const [loading, setLoading] = useState(false);
  const timerRef = useRef(null);

  const fetchSuppliers = useCallback(async (q) => {
    setLoading(true);
    try {
      const res = await api.get('/identity/api/v1/users/directory/suppliers', {
        params: q ? { q } : {},
      });
      const list = res.data?.result ?? [];
      setOptions(
        list.map((s) => ({
          value: s.id,
          label: formatSupplierLabel(s),
          title: s.descriptionPreview || undefined,
        }))
      );
    } catch {
      setOptions([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchSuppliers('');
  }, [fetchSuppliers]);

  const onSearch = (text) => {
    if (timerRef.current) {
      clearTimeout(timerRef.current);
    }
    timerRef.current = setTimeout(() => {
      fetchSuppliers((text || '').trim());
    }, 320);
  };

  return (
    <Select
      showSearch
      allowClear
      filterOption={false}
      placeholder="Gõ tên NCC, tài khoản hoặc từ khóa (vd: sữa)…"
      value={value || undefined}
      onChange={(id) => {
        if (!id) {
          onClear?.();
          onChange?.(null);
          fetchSuppliers('');
          return;
        }
        onChange?.(id);
      }}
      onSearch={onSearch}
      loading={loading}
      notFoundContent={loading ? <Spin size="small" /> : 'Không tìm thấy nhà cung cấp'}
      options={options}
      style={{ width: '100%', maxWidth: 560 }}
      optionLabelProp="label"
    />
  );
};

function formatSupplierLabel(s) {
  const name = s.fullName?.trim();
  const user = s.username?.trim();
  if (name && user) return `${name} (@${user})`;
  return name || user || s.id;
}

export default SupplierSearchSelect;
