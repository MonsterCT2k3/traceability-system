package vn.edu.kma.product_service.service;

import vn.edu.kma.product_service.dto.request.BanknoteSerialBulkRequest;
import vn.edu.kma.product_service.dto.response.BanknoteSerialBulkSaveResponse;
import vn.edu.kma.product_service.dto.response.BanknoteSerialSummaryResponse;

public interface BanknoteSerialService {

    BanknoteSerialBulkSaveResponse bulkRegister(BanknoteSerialBulkRequest request, String authorizationHeader) throws Exception;

    BanknoteSerialSummaryResponse getSummary(String authorizationHeader) throws Exception;
}
