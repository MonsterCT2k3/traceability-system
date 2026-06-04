package vn.edu.kma.traceability_core_service.service;

import vn.edu.kma.traceability_core_service.dto.request.PalletAnchorRequest;
import vn.edu.kma.traceability_core_service.dto.response.PalletSummaryResponse;
import vn.edu.kma.traceability_core_service.dto.response.DirectTraceResponse;

import java.util.List;
import java.util.Map;

public interface PalletService {
    Map<String, String> anchorPallet(String productId, PalletAnchorRequest request, String tokenHeader);
    List<PalletSummaryResponse> getMyPallets(String tokenHeader);
    List<PalletSummaryResponse> getPalletsByOwner(String ownerId);
    DirectTraceResponse getDirectTrace(String palletId);
    DirectTraceResponse verifyDirectTrace(String palletId);
}


