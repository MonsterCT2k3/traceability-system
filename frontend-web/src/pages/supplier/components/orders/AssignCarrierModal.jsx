import React, { useEffect, useState } from 'react';
import { Modal, Form, Typography, message } from 'antd';
import TransporterSearchSelect from './TransporterSearchSelect';

const { Text } = Typography;

const AssignCarrierModal = ({ open, order, submitting, onCancel, onSubmit }) => {
  const [form] = Form.useForm();
  const [carrierId, setCarrierId] = useState(null);

  useEffect(() => {
    if (open) {
      setCarrierId(null);
      form.resetFields();
    }
  }, [open, form]);

  const handleOk = async () => {
    if (!carrierId) {
      message.warning('Vui lòng chọn đơn vị vận chuyển');
      return Promise.reject();
    }
    return onSubmit({ carrierId });
  };

  return (
    <Modal
      title="Gán đơn vị vận chuyển"
      open={open}
      onCancel={onCancel}
      onOk={handleOk}
      okText="Xác nhận gán"
      cancelText="Hủy"
      confirmLoading={submitting}
      destroyOnClose
      width={520}
    >
      {order && (
        <Text type="secondary" style={{ display: 'block', marginBottom: 16 }}>
          Đơn <strong>{order.orderCode}</strong> — sau khi gán, đơn chuyển sang trạng thái &quot;Đã gán vận chuyển&quot;.
        </Text>
      )}
      <Form form={form} layout="vertical">
        <Form.Item label="Chọn đơn vị vận chuyển" required>
          <TransporterSearchSelect value={carrierId} onChange={(id) => setCarrierId(id)} disabled={submitting} />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default AssignCarrierModal;
