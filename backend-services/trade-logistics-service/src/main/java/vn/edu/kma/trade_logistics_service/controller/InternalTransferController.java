package vn.edu.kma.trade_logistics_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.kma.common.dto.response.ApiResponse;
import vn.edu.kma.trade_logistics_service.entity.TransferRecord;
import vn.edu.kma.trade_logistics_service.repository.TransferRecordRepository;

import java.util.List;

@RestController
@RequestMapping("/api/v1/internal/transfers")
@RequiredArgsConstructor
public class InternalTransferController {

    private final TransferRecordRepository transferRecordRepository;

    @GetMapping("/target/{targetType}/{targetId}")
    public ResponseEntity<ApiResponse<List<TransferRecord>>> getTransfersByTarget(
            @PathVariable("targetType") String targetType,
            @PathVariable("targetId") String targetId
    ) {
        List<TransferRecord> records = transferRecordRepository.findByTargetTypeAndTargetIdOrderByCreatedAtAsc(targetType, targetId);
        return ResponseEntity.ok(ApiResponse.<List<TransferRecord>>builder()
                .code(200)
                .result(records)
                .build());
    }
}
