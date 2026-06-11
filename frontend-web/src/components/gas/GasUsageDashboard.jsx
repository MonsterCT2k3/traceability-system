import React, { useMemo, useState } from 'react';
import { Alert, Card, Col, DatePicker, Row, Select, Space, Statistic, Table, Tag, Typography } from 'antd';
import { ThunderboltOutlined, CheckCircleOutlined, CloseCircleOutlined, ClockCircleOutlined } from '@ant-design/icons';
import { useQuery } from '@tanstack/react-query';
import api from '../../lib/api';

const { RangePicker } = DatePicker;
const { Title, Text } = Typography;

const STATUS_META = {
  SUCCESS: { color: 'success', label: 'Thành công' },
  FAILED_ON_CHAIN: { color: 'error', label: 'Thất bại trên chain' },
  SUBMISSION_FAILED: { color: 'warning', label: 'Lỗi trước khi lên chain' },
  RECEIPT_UNKNOWN: { color: 'processing', label: 'Đang đối soát' },
  PENDING: { color: 'default', label: 'Đang xử lý' },
};

const OPERATION_LABELS = {
  RECORD_BATCH: 'Ghi lô nguyên liệu',
  RECORD_TRANSFORMED_BATCH: 'Ghi lô sản xuất',
  OWNERSHIP_CHANGE: 'Chuyển quyền sở hữu',
};

const weiToEth = (value) => {
  if (!value) return '0';
  const wei = BigInt(value);
  const whole = wei / 1000000000000000000n;
  const fraction = (wei % 1000000000000000000n).toString().padStart(18, '0').slice(0, 8);
  return `${whole}.${fraction}`.replace(/\.?0+$/, '');
};

const shortHash = (hash) => {
  if (!hash) return '-';
  return hash.length <= 16 ? hash : `${hash.slice(0, 10)}...${hash.slice(-8)}`;
};

const GasUsageDashboard = ({ accent = '#1677ff' }) => {
  const [range, setRange] = useState(null);
  const [operation, setOperation] = useState();
  const [status, setStatus] = useState();
  const [page, setPage] = useState(1);

  const params = useMemo(() => {
    const query = {};
    if (range?.[0]) query.from = range[0].startOf('day').toISOString();
    if (range?.[1]) query.to = range[1].endOf('day').toISOString();
    return query;
  }, [range]);

  const summaryQuery = useQuery({
    queryKey: ['gasUsageSummary', params],
    queryFn: async () => {
      const response = await api.get('/blockchain/api/v1/gas-usage/my/summary', { params });
      return response.data.result;
    },
  });

  const transactionsQuery = useQuery({
    queryKey: ['gasUsageTransactions', params, operation, status, page],
    queryFn: async () => {
      const response = await api.get('/blockchain/api/v1/gas-usage/my/transactions', {
        params: {
          ...params,
          operation,
          status,
          page: page - 1,
          size: 10,
        },
      });
      return response.data.result;
    },
    keepPreviousData: true,
  });

  const summary = summaryQuery.data || {};
  const rows = transactionsQuery.data?.content || [];

  const totalByOperation = useMemo(() => {
    const items = summary.breakdown || [];
    const grouped = items.reduce((acc, item) => {
      const key = item.operation;
      acc[key] = (acc[key] || 0n) + BigInt(item.feeWei || '0');
      return acc;
    }, {});
    const max = Object.values(grouped).reduce((m, v) => (v > m ? v : m), 0n);
    return Object.entries(grouped).map(([key, fee]) => ({
      key,
      label: OPERATION_LABELS[key] || key,
      fee,
      percent: max === 0n ? 0 : Number((fee * 100n) / max),
    }));
  }, [summary.breakdown]);

  const columns = [
    {
      title: 'Thời gian',
      dataIndex: 'createdAt',
      render: (value) => value ? new Date(value).toLocaleString('vi-VN') : '-',
      width: 180,
    },
    {
      title: 'Nghiệp vụ',
      dataIndex: 'operation',
      render: (value) => OPERATION_LABELS[value] || value,
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      render: (value) => <Tag color={STATUS_META[value]?.color || 'default'}>{STATUS_META[value]?.label || value}</Tag>,
    },
    {
      title: 'Gas used',
      dataIndex: 'gasUsed',
      align: 'right',
      render: (value) => value || '-',
    },
    {
      title: 'Phí',
      dataIndex: 'feeWei',
      align: 'right',
      render: (value) => value ? `${weiToEth(value)} ETH` : '-',
    },
    {
      title: 'Tx hash',
      dataIndex: 'txHash',
      render: (value) => <Text code>{shortHash(value)}</Text>,
    },
  ];

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', gap: 16, flexWrap: 'wrap', marginBottom: 20 }}>
        <div>
          <Title level={3} style={{ marginBottom: 4 }}>Chi phí blockchain</Title>
          <Text type="secondary">Chi phí gas được phân bổ nội bộ cho các thao tác ghi lên blockchain.</Text>
        </div>
        <RangePicker onChange={setRange} />
      </div>

      <Alert
        showIcon
        type="info"
        style={{ marginBottom: 20 }}
        message="Hệ thống đang dùng system wallet để ghi blockchain; các số liệu này là chi phí được phân bổ cho vai trò, không phải số dư ví riêng."
      />

      <Row gutter={[16, 16]}>
        <Col xs={24} md={12} xl={6}>
          <Card bordered={false} style={{ background: `linear-gradient(135deg, ${accent}, #113c33)`, color: 'white' }}>
            <Statistic
              title={<span style={{ color: 'rgba(255,255,255,.78)' }}>Tổng phí thực tế</span>}
              value={`${weiToEth(summary.actualFeeWei)} ETH`}
              valueStyle={{ color: 'white', fontSize: 24 }}
              prefix={<ThunderboltOutlined />}
            />
          </Card>
        </Col>
        <Col xs={24} md={12} xl={6}>
          <Card bordered={false}>
            <Statistic
              title="Phí giao dịch thành công"
              value={`${weiToEth(summary.successFeeWei)} ETH`}
              prefix={<CheckCircleOutlined style={{ color: '#16a34a' }} />}
            />
          </Card>
        </Col>
        <Col xs={24} md={12} xl={6}>
          <Card bordered={false}>
            <Statistic
              title="Phí thất bại trên chain"
              value={`${weiToEth(summary.failedOnChainFeeWei)} ETH`}
              prefix={<CloseCircleOutlined style={{ color: '#dc2626' }} />}
            />
          </Card>
        </Col>
        <Col xs={24} md={12} xl={6}>
          <Card bordered={false}>
            <Statistic
              title="Chờ đối soát"
              value={summary.receiptUnknownCount || 0}
              suffix="giao dịch"
              prefix={<ClockCircleOutlined style={{ color: '#d97706' }} />}
            />
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col xs={24} lg={8}>
          <Card title="Phân bổ theo nghiệp vụ" bordered={false} style={{ height: '100%' }}>
            <Space direction="vertical" size={16} style={{ width: '100%' }}>
              {totalByOperation.length === 0 && <Text type="secondary">Chưa có dữ liệu.</Text>}
              {totalByOperation.map((item) => (
                <div key={item.key}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 6 }}>
                    <Text>{item.label}</Text>
                    <Text strong>{weiToEth(item.fee.toString())} ETH</Text>
                  </div>
                  <div style={{ height: 8, background: '#eef2f7', borderRadius: 999 }}>
                    <div
                      style={{
                        width: `${Math.max(item.percent, item.fee > 0n ? 6 : 0)}%`,
                        height: 8,
                        background: accent,
                        borderRadius: 999,
                      }}
                    />
                  </div>
                </div>
              ))}
            </Space>
          </Card>
        </Col>
        <Col xs={24} lg={16}>
          <Card
            title="Lịch sử giao dịch gas"
            bordered={false}
            extra={
              <Space>
                <Select
                  allowClear
                  placeholder="Nghiệp vụ"
                  style={{ width: 210 }}
                  value={operation}
                  onChange={(value) => {
                    setOperation(value);
                    setPage(1);
                  }}
                  options={Object.entries(OPERATION_LABELS).map(([value, label]) => ({ value, label }))}
                />
                <Select
                  allowClear
                  placeholder="Trạng thái"
                  style={{ width: 190 }}
                  value={status}
                  onChange={(value) => {
                    setStatus(value);
                    setPage(1);
                  }}
                  options={Object.entries(STATUS_META).map(([value, meta]) => ({ value, label: meta.label }))}
                />
              </Space>
            }
          >
            <Table
              rowKey="id"
              columns={columns}
              dataSource={rows}
              loading={summaryQuery.isLoading || transactionsQuery.isLoading}
              pagination={{
                current: page,
                pageSize: 10,
                total: transactionsQuery.data?.totalElements || 0,
                onChange: setPage,
                showSizeChanger: false,
              }}
              scroll={{ x: 900 }}
            />
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default GasUsageDashboard;
