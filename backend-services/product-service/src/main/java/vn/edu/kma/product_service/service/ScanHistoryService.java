package vn.edu.kma.product_service.service;

import org.springframework.data.domain.Page;
import vn.edu.kma.product_service.dto.request.ScanHistoryRequest;
import vn.edu.kma.product_service.dto.response.ScanHistoryResponse;

public interface ScanHistoryService {
    void recordScan(ScanHistoryRequest request, String tokenHeader);
    Page<ScanHistoryResponse> getScanHistory(String tokenHeader, int page, int size);
}
