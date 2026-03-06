package vn.edu.kma.blockchain_service.service;

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
}
