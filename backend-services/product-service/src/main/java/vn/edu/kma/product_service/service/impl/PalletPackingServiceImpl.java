package vn.edu.kma.product_service.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.kma.product_service.dto.request.CartonCreateRequest;
import vn.edu.kma.product_service.dto.request.PalletBulkPackingRequest;
import vn.edu.kma.product_service.dto.request.ProductUnitGenerateRequest;
import vn.edu.kma.product_service.dto.response.PalletBulkCartonResult;
import vn.edu.kma.product_service.dto.response.PalletBulkPackingResponse;
import vn.edu.kma.product_service.dto.response.ProductUnitGenerateResponse;
import vn.edu.kma.product_service.entity.Carton;
import vn.edu.kma.product_service.entity.Pallet;
import vn.edu.kma.product_service.repository.PalletRepository;
import vn.edu.kma.product_service.service.CartonService;
import vn.edu.kma.product_service.service.PalletPackingService;
import vn.edu.kma.product_service.service.ProductUnitService;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PalletPackingServiceImpl implements PalletPackingService {

    private static final int MAX_CARTONS = 200;
    private static final int MAX_UNITS_PER_CARTON = 200;
    private static final int MAX_TOTAL_UNITS = 10_000;

    private final PalletRepository palletRepository;
    private final CartonService cartonService;
    private final ProductUnitService productUnitService;

    @Override
    @Transactional
    public PalletBulkPackingResponse bulkPack(String palletId, PalletBulkPackingRequest request, String tokenHeader) {
        if (request.getCartonCount() == null || request.getCartonCount() <= 0) {
            throw new RuntimeException("cartonCount phải là số dương");
        }
        if (request.getUnitsPerCarton() == null || request.getUnitsPerCarton() <= 0) {
            throw new RuntimeException("unitsPerCarton phải là số dương");
        }
        int n = request.getCartonCount();
        int m = request.getUnitsPerCarton();
        if (n > MAX_CARTONS) {
            throw new RuntimeException("cartonCount tối đa " + MAX_CARTONS + " mỗi request");
        }
        if (m > MAX_UNITS_PER_CARTON) {
            throw new RuntimeException("unitsPerCarton tối đa " + MAX_UNITS_PER_CARTON);
        }
        if ((long) n * m > MAX_TOTAL_UNITS) {
            throw new RuntimeException("Tổng số unit (cartonCount × unitsPerCarton) tối đa " + MAX_TOTAL_UNITS);
        }

        Pallet pallet = palletRepository.findById(palletId)
                .orElseThrow(() -> new RuntimeException("Pallet không tồn tại"));

        List<PalletBulkCartonResult> cartonResults = new ArrayList<>(n);
        int totalUnits = 0;

        for (int i = 0; i < n; i++) {
            CartonCreateRequest creq = new CartonCreateRequest();
            creq.setPlannedUnitCount(m);
            creq.setUnitLabel(trimToNull(request.getUnitLabel()));
            creq.setNote(trimToNull(request.getNote()));

            Carton carton = cartonService.createCarton(palletId, creq, tokenHeader);

            ProductUnitGenerateRequest greq = new ProductUnitGenerateRequest();
            greq.setCount(m);
            ProductUnitGenerateResponse gen = productUnitService.generateUnits(carton.getId(), greq, tokenHeader);

            totalUnits += gen.getUnits().size();
            cartonResults.add(PalletBulkCartonResult.builder()
                    .cartonId(carton.getId())
                    .cartonCode(carton.getCartonCode())
                    .units(gen.getUnits())
                    .build());
        }

        return PalletBulkPackingResponse.builder()
                .palletId(pallet.getId())
                .palletCode(pallet.getPalletCode())
                .cartonsCreated(n)
                .unitsCreated(totalUnits)
                .cartons(cartonResults)
                .build();
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
