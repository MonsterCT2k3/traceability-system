package vn.edu.kma.blockchain_service.service;

import vn.edu.kma.blockchain_service.dto.response.BatchRecordResponse;
import vn.edu.kma.blockchain_service.dto.response.TransformedBatchRecordResponse;

import java.util.List;

public interface TraceabilityService {
    /**
     * Deploy Smart Contract mới lên mạng Blockchain.
     * @return Địa chỉ Contract (Address)
     */
    String deployContract() throws Exception;

    /** Ghi lô gốc (RAW) — chỉ ví system. */
    String recordBatch(String batchIdHex, String dataHashHex) throws Exception;

    /** Ghi lô chế biến / Pallet — chỉ ví system. */
    String recordTransformedBatch(String batchIdHex, String dataHashHex, List<String> parentHashesHex) throws Exception;

    /**
     * Ghi audit chuyển quyền (userId) lên chain — emit OwnershipChanged, trả về txHash.
     */
    String logOwnershipChange(String batchIdHex, String fromUserId, String toUserId) throws Exception;
    // Đọc thông tin lô gốc
    BatchRecordResponse getBatchRecord(String batchIdHex) throws Exception;
    // Đọc thông tin lô chế biến / Pallet
    TransformedBatchRecordResponse getTransformedBatchRecord(String batchIdHex) throws Exception;
    boolean hasBatch(String batchIdHex) throws Exception;
    boolean hasTransformedBatch(String batchIdHex) throws Exception;
}
