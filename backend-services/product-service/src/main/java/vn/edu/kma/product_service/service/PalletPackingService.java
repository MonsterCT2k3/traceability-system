package vn.edu.kma.product_service.service;

import vn.edu.kma.product_service.dto.request.PalletBulkPackingRequest;
import vn.edu.kma.product_service.dto.response.PalletBulkPackingResponse;

public interface PalletPackingService {

    PalletBulkPackingResponse bulkPack(String palletId, PalletBulkPackingRequest request, String tokenHeader);
}
