package vn.edu.kma.product_service.service.impl;

import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.kma.product_service.repository.BanknoteSerialRepository;
import vn.edu.kma.product_service.dto.request.CartonCreateRequest;
import vn.edu.kma.product_service.dto.request.PalletBulkPackingRequest;
import vn.edu.kma.product_service.dto.request.ProductUnitGenerateRequest;
import vn.edu.kma.product_service.dto.response.PalletBulkCartonResult;
import vn.edu.kma.product_service.dto.response.PalletBulkPackingResponse;
import vn.edu.kma.product_service.dto.response.PackingUnitSerialItem;
import vn.edu.kma.product_service.dto.response.ProductUnitGenerateResponse;
import vn.edu.kma.product_service.entity.Carton;
import vn.edu.kma.product_service.entity.Pallet;
import vn.edu.kma.product_service.repository.PalletRepository;
import vn.edu.kma.product_service.service.CartonService;
import vn.edu.kma.product_service.service.PalletPackingService;
import vn.edu.kma.product_service.service.ProductUnitService;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
    private final BanknoteSerialRepository banknoteSerialRepository;

    @Override
    @Transactional
    public PalletBulkPackingResponse bulkPack(String palletId, PalletBulkPackingRequest request, String tokenHeader) {
        String userId = extractUserIdFromToken(tokenHeader);
        Pallet pallet = palletRepository.findById(palletId)
                .orElseThrow(() -> new RuntimeException("Pallet không tồn tại"));

        List<List<String>> serialBatches = normalizeSerialBatches(request.getSerialBatches());
        boolean useSerialBatches = !serialBatches.isEmpty();

        int n;
        int totalPlannedUnits = 0;
        if (useSerialBatches) {
            n = serialBatches.size();
            if (n > MAX_CARTONS) {
                throw new RuntimeException("Số carton (serialBatches) tối đa " + MAX_CARTONS + " mỗi request");
            }
            for (List<String> one : serialBatches) {
                if (one.isEmpty()) {
                    throw new RuntimeException("Mỗi carton trong serialBatches phải có ít nhất 1 serial");
                }
                if (one.size() > MAX_UNITS_PER_CARTON) {
                    throw new RuntimeException("Số serial mỗi carton tối đa " + MAX_UNITS_PER_CARTON);
                }
                totalPlannedUnits += one.size();
            }
        } else {
            if (request.getCartonCount() == null || request.getCartonCount() <= 0) {
                throw new RuntimeException("cartonCount phải là số dương");
            }
            if (request.getUnitsPerCarton() == null || request.getUnitsPerCarton() <= 0) {
                throw new RuntimeException("unitsPerCarton phải là số dương");
            }
            n = request.getCartonCount();
            int m = request.getUnitsPerCarton();
            if (n > MAX_CARTONS) {
                throw new RuntimeException("cartonCount tối đa " + MAX_CARTONS + " mỗi request");
            }
            if (m > MAX_UNITS_PER_CARTON) {
                throw new RuntimeException("unitsPerCarton tối đa " + MAX_UNITS_PER_CARTON);
            }
            totalPlannedUnits = n * m;
            List<String> allocated = banknoteSerialRepository.findAvailableSerialValuesForUser(userId, totalPlannedUnits);
            if (allocated.size() < totalPlannedUnits) {
                throw new RuntimeException("Không đủ seri khả dụng để đóng gói. Cần " + totalPlannedUnits + ", còn " + allocated.size());
            }
            serialBatches = splitIntoBatches(allocated, m);
            useSerialBatches = true;
        }
        if (totalPlannedUnits > MAX_TOTAL_UNITS) {
            throw new RuntimeException("Tổng số unit tối đa " + MAX_TOTAL_UNITS + " mỗi request");
        }

        List<PalletBulkCartonResult> cartonResults = new ArrayList<>(n);
        int totalUnits = 0;

        for (int i = 0; i < n; i++) {
            int planned = useSerialBatches ? serialBatches.get(i).size() : request.getUnitsPerCarton();
            CartonCreateRequest creq = new CartonCreateRequest();
            creq.setPlannedUnitCount(planned);
            creq.setUnitLabel(trimToNull(request.getUnitLabel()));
            creq.setNote(trimToNull(request.getNote()));

            Carton carton = cartonService.createCarton(palletId, creq, tokenHeader);

            ProductUnitGenerateRequest greq = new ProductUnitGenerateRequest();
            if (useSerialBatches) {
                greq.setSerials(serialBatches.get(i));
            } else {
                greq.setCount(request.getUnitsPerCarton());
            }
            ProductUnitGenerateResponse gen = productUnitService.generateUnits(carton.getId(), greq, tokenHeader);
            List<PackingUnitSerialItem> unitSerialItems = gen.getUnits().stream()
                    .map(u -> PackingUnitSerialItem.builder().unitSerial(u.getUnitSerial()).build())
                    .toList();

            totalUnits += unitSerialItems.size();
            cartonResults.add(PalletBulkCartonResult.builder()
                    .cartonCode(carton.getCartonCode())
                    .units(unitSerialItems)
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

    private static List<List<String>> normalizeSerialBatches(List<List<String>> raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        List<List<String>> out = new ArrayList<>();
        for (List<String> batch : raw) {
            if (batch == null) {
                throw new RuntimeException("serialBatches chứa carton null");
            }
            List<String> cleaned = batch.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
            out.add(cleaned);
        }
        return out;
    }

    private static List<List<String>> splitIntoBatches(List<String> serials, int unitsPerCarton) {
        List<List<String>> out = new ArrayList<>();
        for (int i = 0; i < serials.size(); i += unitsPerCarton) {
            int end = Math.min(i + unitsPerCarton, serials.size());
            out.add(new ArrayList<>(serials.subList(i, end)));
        }
        return out;
    }

    private static String extractUserIdFromToken(String tokenHeader) {
        try {
            String token = tokenHeader;
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            SignedJWT signedJWT = SignedJWT.parse(token);
            return signedJWT.getJWTClaimsSet().getStringClaim("userId");
        } catch (Exception e) {
            throw new RuntimeException("Token không hợp lệ: " + e.getMessage());
        }
    }
}
