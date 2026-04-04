package vn.edu.kma.product_service.service;

import vn.edu.kma.product_service.dto.request.TransferInitRequest;
import vn.edu.kma.product_service.entity.TransferRecord;

import java.util.List;

public interface TransferService {
    TransferRecord initiateTransfer(TransferInitRequest request, String token);
    TransferRecord respondTransfer(String transferId, boolean accept, String token);
    List<TransferRecord> getPendingTransfers(String token);
}
