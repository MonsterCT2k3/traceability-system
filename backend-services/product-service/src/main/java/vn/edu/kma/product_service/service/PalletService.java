package vn.edu.kma.product_service.service;

import vn.edu.kma.product_service.dto.request.PalletAnchorRequest;

import java.util.Map;

public interface PalletService {
    Map<String, String> anchorPallet(String productId, PalletAnchorRequest request, String tokenHeader);
}

