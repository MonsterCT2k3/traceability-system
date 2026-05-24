package vn.edu.kma.traceability_core_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import vn.edu.kma.common.dto.response.ApiResponse;
import vn.edu.kma.traceability_core_service.entity.Carton;
import vn.edu.kma.traceability_core_service.entity.Pallet;
import vn.edu.kma.traceability_core_service.entity.ProductUnit;
import vn.edu.kma.traceability_core_service.entity.RawBatch;
import vn.edu.kma.traceability_core_service.repository.CartonRepository;
import vn.edu.kma.traceability_core_service.repository.PalletRepository;
import vn.edu.kma.traceability_core_service.repository.ProductUnitRepository;
import vn.edu.kma.traceability_core_service.repository.RawBatchRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/internal/products")
@RequiredArgsConstructor
public class InternalTradeLogisticsController {

    private final RawBatchRepository rawBatchRepository;
    private final PalletRepository palletRepository;
    private final CartonRepository cartonRepository;
    private final ProductUnitRepository productUnitRepository;

    @GetMapping("/raw-batch/{id}")
    public ResponseEntity<ApiResponse<RawBatch>> getRawBatch(@PathVariable String id) {
        return rawBatchRepository.findById(id)
                .map(rb -> ResponseEntity.ok(ApiResponse.<RawBatch>builder().result(rb).build()))
                .orElse(ResponseEntity.ok(ApiResponse.<RawBatch>builder().result(null).build()));
    }

    @GetMapping("/pallet/{id}")
    public ResponseEntity<ApiResponse<Pallet>> getPallet(@PathVariable String id) {
        return palletRepository.findById(id)
                .map(p -> ResponseEntity.ok(ApiResponse.<Pallet>builder().result(p).build()))
                .orElse(ResponseEntity.ok(ApiResponse.<Pallet>builder().result(null).build()));
    }

    @GetMapping("/carton/{id}")
    public ResponseEntity<ApiResponse<Carton>> getCarton(@PathVariable String id) {
        return cartonRepository.findById(id)
                .map(c -> ResponseEntity.ok(ApiResponse.<Carton>builder().result(c).build()))
                .orElse(ResponseEntity.ok(ApiResponse.<Carton>builder().result(null).build()));
    }

    @GetMapping("/unit/{id}")
    public ResponseEntity<ApiResponse<ProductUnit>> getProductUnit(@PathVariable String id) {
        return productUnitRepository.findById(id)
                .map(u -> ResponseEntity.ok(ApiResponse.<ProductUnit>builder().result(u).build()))
                .orElse(ResponseEntity.ok(ApiResponse.<ProductUnit>builder().result(null).build()));
    }

    @PostMapping("/ownership/transfer")
    @Transactional
    public ResponseEntity<ApiResponse<String>> transferOwnership(
            @RequestParam String targetType,
            @RequestParam String targetId,
            @RequestParam String newOwnerId
    ) {
        String batchIdHex = null;
        if ("PALLET".equals(targetType)) {
            Pallet p = palletRepository.findById(targetId).orElseThrow();
            p.setOwnerId(newOwnerId);
            palletRepository.save(p);
            batchIdHex = p.getChainBatchIdHex();
        } else if ("RAW_BATCH".equals(targetType)) {
            RawBatch r = rawBatchRepository.findById(targetId).orElseThrow();
            r.setOwnerId(newOwnerId);
            rawBatchRepository.save(r);
            batchIdHex = r.getBatchIdHex();
        } else if ("CARTON".equals(targetType)) {
            Carton c = cartonRepository.findById(targetId).orElseThrow();
            c.setOwnerId(newOwnerId);
            cartonRepository.save(c);
            Pallet p = palletRepository.findById(c.getPalletId()).orElseThrow();
            batchIdHex = p.getChainBatchIdHex();
        } else if ("UNIT".equals(targetType)) {
            ProductUnit u = productUnitRepository.findById(targetId).orElseThrow();
            u.setOwnerId(newOwnerId);
            productUnitRepository.save(u);
            Pallet p = palletRepository.findById(u.getPalletId()).orElseThrow();
            batchIdHex = p.getChainBatchIdHex();
        }
        return ResponseEntity.ok(ApiResponse.<String>builder().result(batchIdHex).build());
    }

    @GetMapping("/trade/check-inventory")
    public ResponseEntity<ApiResponse<Boolean>> checkInventory(
            @RequestParam String sellerId,
            @RequestParam String productId,
            @RequestParam int quantity
    ) {
        long available = cartonRepository.countAvailableForShipping(productId, sellerId);
        boolean enough = available >= quantity;
        return ResponseEntity.ok(ApiResponse.<Boolean>builder().result(enough).build());
    }

    @PostMapping("/trade/ship-cartons")
    @Transactional
    public ResponseEntity<ApiResponse<List<Carton>>> shipCartons(
            @RequestParam String sellerId,
            @RequestParam String productId,
            @RequestParam int quantity
    ) {
        List<Carton> cartons = cartonRepository.findAvailableForShipping(productId, sellerId, org.springframework.data.domain.PageRequest.of(0, quantity));
        if (cartons.size() < quantity) {
            throw new RuntimeException("Không đủ số lượng thùng hàng sẵn sàng trong kho!");
        }
        for (Carton c : cartons) {
            c.setStatus("SHIPPING");
            cartonRepository.save(c);
            productUnitRepository.updateStatusByCartonId(c.getId(), "SHIPPING");
        }
        return ResponseEntity.ok(ApiResponse.<List<Carton>>builder().result(cartons).build());
    }

    @PostMapping("/trade/deliver-cartons")
    @Transactional
    public ResponseEntity<ApiResponse<List<Carton>>> deliverCartons(
            @RequestParam String sellerId,
            @RequestParam String buyerId,
            @RequestParam String productId,
            @RequestParam int quantity
    ) {
        List<Carton> cartons = cartonRepository.findAvailableForDelivery(
                productId, sellerId, org.springframework.data.domain.PageRequest.of(0, quantity));

        if (cartons.size() < quantity) {
            cartons = cartonRepository.findAvailableForShipping(
                    productId, sellerId, org.springframework.data.domain.PageRequest.of(0, quantity));
        }

        if (cartons.size() < quantity) {
            throw new RuntimeException("Sản phẩm " + productId + ": không đủ tồn kho hoặc hàng chưa sẵn sàng vận chuyển");
        }

        for (Carton c : cartons) {
            c.setStatus("DELIVERED");
            c.setOwnerId(buyerId);
            cartonRepository.save(c);
            productUnitRepository.updateOwnerAndStatusByCartonId(c.getId(), buyerId, "DELIVERED");
        }
        return ResponseEntity.ok(ApiResponse.<List<Carton>>builder().result(cartons).build());
    }
}

