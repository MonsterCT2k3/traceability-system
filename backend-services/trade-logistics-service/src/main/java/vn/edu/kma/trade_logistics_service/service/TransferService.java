package vn.edu.kma.trade_logistics_service.service;

import vn.edu.kma.trade_logistics_service.dto.request.TransferInitRequest;
import vn.edu.kma.trade_logistics_service.entity.TransferRecord;

import java.util.List;

public interface TransferService {
    TransferRecord initiateTransfer(TransferInitRequest request, String token);
    TransferRecord respondTransfer(String transferId, boolean accept, String token);
    List<TransferRecord> getPendingTransfers(String token);
}
