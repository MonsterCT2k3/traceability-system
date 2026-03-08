package vn.edu.kma.blockchain_service.service;

import vn.edu.kma.blockchain_service.dto.response.BlockchainHistoryResponse;

import java.util.List;

public interface TraceabilityService {
    /**
     * Deploy Smart Contract mới lên mạng Blockchain.
     * @return Địa chỉ Contract (Address)
     */
    String deployContract() throws Exception;

    /**
     * Ghi nhật ký truy xuất nguồn gốc lên Blockchain.
     * @param productId ID sản phẩm
     * @param action Hành động (CREATE, HARVEST, ...)
     * @param description Mô tả chi tiết
     * @return Transaction Hash (Mã giao dịch)
     */
    String addHistory(String productId, String action, String description) throws Exception;

    // Thêm 2 hàm mới
    int getHistoryCount(String productId) throws Exception;
    List<BlockchainHistoryResponse> getHistoryByProductId(String productId) throws Exception;
}
