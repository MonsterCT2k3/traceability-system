import React, { useCallback, useEffect, useRef, useState } from 'react';
import { Select, Spin } from 'antd';
import api from '../../../../lib/api';

function formatLabel(u) {
  const name = u.fullName?.trim();
  const user = u.username?.trim();
  if (name && user) return `${name} (@${user})`;
  return name || user || u.id;
}

/**
 * Tìm đơn vị vận chuyển (TRANSPORTER) — identity directory API.
 */
const TransporterSearchSelect = ({ value, onChange, disabled }) => {
  const [options, setOptions] = useState([]);
  const [loading, setLoading] = useState(false);
  const timerRef = useRef(null);

  const fetchList = useCallback(async (q) => {
    setLoading(true);
    try {
      const res = await api.get('/identity/api/v1/users/directory/transporters', {
        params: q ? { q } : {},
      });
      const list = res.data?.result ?? [];
      setOptions(
        list.map((u) => ({
          value: u.id,
          label: formatLabel(u),
          title: u.descriptionPreview || undefined,
        }))
      );
    } catch {
      setOptions([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchList('');
  }, [fetchList]);

  const onSearch = (text) => {
    if (timerRef.current) clearTimeout(timerRef.current);
    timerRef.current = setTimeout(() => fetchList((text || '').trim()), 320);
  };

  return (
    <Select
      showSearch
      filterOption={false}
      disabled={disabled}
      placeholder="Gõ tên, tài khoản đơn vị vận chuyển…"
      value={value || undefined}
      onChange={onChange}
      onSearch={onSearch}
      loading={loading}
      notFoundContent={loading ? <Spin size="small" /> : 'Không tìm thấy'}
      options={options}
      style={{ width: '100%' }}
      optionLabelProp="label"
    />
  );
};

export default TransporterSearchSelect;
