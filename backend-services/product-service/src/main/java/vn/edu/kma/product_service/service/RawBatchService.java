package vn.edu.kma.product_service.service;

import vn.edu.kma.product_service.dto.request.RawBatchCreateRequest;

import java.util.Map;

public interface RawBatchService {
    Map<String, String> createRawBatch(RawBatchCreateRequest request, String token);
}

