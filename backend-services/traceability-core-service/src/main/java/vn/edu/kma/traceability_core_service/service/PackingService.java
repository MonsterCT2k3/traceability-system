package vn.edu.kma.traceability_core_service.service;

import vn.edu.kma.traceability_core_service.dto.response.ProductPackingManifestResponse;
import vn.edu.kma.traceability_core_service.dto.response.ProductPackingSummaryResponse;

import java.util.List;

public interface PackingService {
    List<ProductPackingSummaryResponse> getMyPackingSummary(String tokenHeader);
    List<ProductPackingManifestResponse> getMyPackingManifest(String tokenHeader);
}

