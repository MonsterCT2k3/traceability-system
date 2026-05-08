import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { Typography, Spin } from 'antd';
import api from '../../lib/api';

const { Text } = Typography;

const fetchProductDetails = async (productId) => {
  if (!productId) return null;
  const res = await api.get(`/product/api/v1/products/${productId}`);
  return res.data?.result;
};

const ProductNameDisplay = ({ productId, showIdOnHover = true }) => {
  const { data, isLoading, isError } = useQuery({
    queryKey: ['productDetails', productId],
    queryFn: () => fetchProductDetails(productId),
    enabled: !!productId,
    staleTime: 5 * 60 * 1000, // 5 minutes cache
  });

  if (!productId) {
    return <Text type="secondary">—</Text>;
  }

  if (isLoading) {
    return <Spin size="small" />;
  }

  if (isError || !data) {
    return <Text type="danger">{productId}</Text>;
  }

  const displayName = data.name || productId;

  if (showIdOnHover) {
    return (
      <span title={`ID: ${productId}`}>
        <Text>{displayName}</Text>
      </span>
    );
  }

  return <Text>{displayName}</Text>;
};

export default ProductNameDisplay;
