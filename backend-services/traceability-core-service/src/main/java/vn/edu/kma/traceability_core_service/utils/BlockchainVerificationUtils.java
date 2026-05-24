package vn.edu.kma.traceability_core_service.utils;

import org.web3j.crypto.Hash;
import org.web3j.utils.Numeric;
import vn.edu.kma.traceability_core_service.entity.Pallet;
import vn.edu.kma.traceability_core_service.entity.RawBatch;

import java.nio.charset.StandardCharsets;

/**
 * Utility để tính toán lại mã Hash của dữ liệu nhằm đối chiếu với Blockchain.
 * Các payload xây dựng ở đây PHẢI khớp hoàn toàn với logic lúc ghi dữ liệu.
 */
public class BlockchainVerificationUtils {

    public static String calculateRawBatchHash(RawBatch b) {
        String payload = normalizeString(b.getRawBatchCode()) + "|" +
                normalizeString(b.getMaterialType()) + "|" +
                normalizeString(b.getMaterialName()) + "|" +
                normalizeString(b.getActorId()) + "|" +
                normalizeString(b.getHarvestedAt()) + "|" +
                normalizeString(b.getQuantity()) + "|" +
                normalizeString(b.getUnit()) + "|" +
                normalizeString(b.getLocation()) + "|" +
                normalizeString(b.getNote()) + "|" +
                normalizeString(b.getSchemaVersion());
        return keccak256HexUtf8(payload);
    }

    public static String calculatePalletHash(Pallet p) {
        String payload = normalizeString(p.getPalletCode()) + "|" +
                normalizeString(p.getProductId()) + "|" +
                normalizeString(p.getPalletName()) + "|" +
                normalizeString(p.getBatchNo()) + "|" +
                normalizeString(p.getActorId()) + "|" +
                normalizeString(p.getManufacturedAt()) + "|" +
                normalizeString(p.getExpiryAt()) + "|" +
                normalizeString(p.getQuantity()) + "|" +
                normalizeString(p.getUnit()) + "|" +
                normalizeString(p.getPackagingType()) + "|" +
                normalizeString(p.getProcessingMethod()) + "|" +
                normalizeString(p.getLocation()) + "|" +
                normalizeString(p.getNote()) + "|" +
                normalizeString(p.getSchemaVersion());
        return keccak256HexUtf8(payload);
    }

    private static String keccak256HexUtf8(String payload) {
        byte[] hash = Hash.sha3(payload.getBytes(StandardCharsets.UTF_8));
        return "0x" + Numeric.toHexStringNoPrefix(hash);
    }

    private static String normalizeString(String s) {
        return s == null ? "" : s.trim();
    }
}

