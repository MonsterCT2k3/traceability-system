package vn.edu.kma.product_service.service.impl;

import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.kma.product_service.dto.request.BanknoteSerialBulkRequest;
import vn.edu.kma.product_service.dto.response.BanknoteSerialBulkSaveResponse;
import vn.edu.kma.product_service.dto.response.BanknoteSerialSummaryResponse;
import vn.edu.kma.product_service.entity.BanknoteSerial;
import vn.edu.kma.product_service.repository.BanknoteSerialRepository;
import vn.edu.kma.product_service.service.BanknoteSerialService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BanknoteSerialServiceImpl implements BanknoteSerialService {

    private static final int MAX_BULK = 2000;
    private static final Pattern VALID = Pattern.compile("^[A-Za-z0-9\\-]{4,32}$");

    private final BanknoteSerialRepository banknoteSerialRepository;

    @Override
    @Transactional
    public BanknoteSerialBulkSaveResponse bulkRegister(BanknoteSerialBulkRequest request, String authorizationHeader) throws Exception {
        String userId = extractUserIdFromToken(authorizationHeader);
        if (request.getSerials() == null || request.getSerials().isEmpty()) {
            throw new IllegalArgumentException("Danh sách serials không được để trống");
        }
        if (request.getSerials().size() > MAX_BULK) {
            throw new IllegalArgumentException("Tối đa " + MAX_BULK + " seri mỗi lần gửi");
        }

        int skippedInvalid = 0;
        LinkedHashSet<String> normalizedUnique = new LinkedHashSet<>();
        for (String raw : request.getSerials()) {
            String n = normalize(raw);
            if (n == null) {
                skippedInvalid++;
                continue;
            }
            normalizedUnique.add(n);
        }

        if (normalizedUnique.isEmpty()) {
            return BanknoteSerialBulkSaveResponse.builder()
                    .inserted(0)
                    .skippedDuplicate(0)
                    .skippedInvalid(skippedInvalid)
                    .build();
        }

        List<String> toCheck = new ArrayList<>(normalizedUnique);
        Set<String> alreadyInDb = banknoteSerialRepository.findBySerialValueIn(toCheck).stream()
                .map(BanknoteSerial::getSerialValue)
                .map(s -> s.toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());

        int skippedDuplicate = 0;
        List<BanknoteSerial> toSave = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (String serial : normalizedUnique) {
            if (alreadyInDb.contains(serial)) {
                skippedDuplicate++;
                continue;
            }
            toSave.add(BanknoteSerial.builder()
                    .serialValue(serial)
                    .registeredByUserId(userId)
                    .createdAt(now)
                    .build());
        }

        if (!toSave.isEmpty()) {
            banknoteSerialRepository.saveAll(toSave);
        }

        log.info("bulkRegister banknote serials: userId={}, inserted={}, skippedDup={}, skippedInvalid={}",
                userId, toSave.size(), skippedDuplicate, skippedInvalid);

        return BanknoteSerialBulkSaveResponse.builder()
                .inserted(toSave.size())
                .skippedDuplicate(skippedDuplicate)
                .skippedInvalid(skippedInvalid)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public BanknoteSerialSummaryResponse getSummary(String authorizationHeader) throws Exception {
        String userId = extractUserIdFromToken(authorizationHeader);
        long mySerials = banknoteSerialRepository.countByRegisteredByUserId(userId);
        long usedSerials = banknoteSerialRepository.countUsedByRegisteredByUserId(userId);
        long availableSerials = Math.max(0L, mySerials - usedSerials);
        return BanknoteSerialSummaryResponse.builder()
                .totalSerials(banknoteSerialRepository.count())
                .mySerials(mySerials)
                .usedSerials(usedSerials)
                .availableSerials(availableSerials)
                .build();
    }

    private static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        String t = raw.trim();
        if (!VALID.matcher(t).matches()) {
            return null;
        }
        return t.toUpperCase(Locale.ROOT);
    }

    private static String extractUserIdFromToken(String tokenHeader) throws Exception {
        String token = tokenHeader;
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        SignedJWT signedJWT = SignedJWT.parse(token);
        return signedJWT.getJWTClaimsSet().getStringClaim("userId");
    }
}
