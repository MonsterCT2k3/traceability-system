package vn.edu.kma.identity_service.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.kma.common.exception.AppException;
import vn.edu.kma.common.exception.ErrorCode;
import vn.edu.kma.common.security.UserRole;
import vn.edu.kma.identity_service.dto.request.CreateRoleRequestDto;
import vn.edu.kma.identity_service.dto.response.RoleRequestResponse;
import vn.edu.kma.identity_service.entity.RoleRequest;
import vn.edu.kma.identity_service.entity.User;
import vn.edu.kma.identity_service.repository.RoleRequestRepository;
import vn.edu.kma.identity_service.repository.UserRepository;
import vn.edu.kma.identity_service.service.RoleRequestService;
import vn.edu.kma.identity_service.service.UserManagementService;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleRequestServiceImpl implements RoleRequestService {

    private final RoleRequestRepository roleRequestRepository;
    private final UserRepository userRepository;
    private final UserManagementService userManagementService;

    @Override
    @Transactional
    public RoleRequestResponse createRequest(String userId, CreateRoleRequestDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Kiểm tra xem role yêu cầu có hợp lệ không
        UserRole requestedRole;
        try {
            requestedRole = UserRole.parseRequired(dto.getRequestedRole());
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.ROLE_INVALID);
        }

        // Kiểm tra xem user đã có đơn PENDING nào chưa
        if (roleRequestRepository.findByUserIdAndStatus(userId, "PENDING").isPresent()) {
            throw new RuntimeException("Bạn đang có một yêu cầu chờ duyệt. Vui lòng đợi!");
        }

        // Kiểm tra xem user có đang xin đúng role hiện tại không
        if (user.getRole().equals(requestedRole.name())) {
            throw new RuntimeException("Bạn đang giữ vai trò này rồi!");
        }

        RoleRequest request = RoleRequest.builder()
                .userId(userId)
                .requestedRole(requestedRole.name())
                .description(dto.getDescription())
                .status("PENDING")
                .build();

        RoleRequest saved = roleRequestRepository.save(request);
        return mapToResponse(saved, user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleRequestResponse> getMyRequests(String userId) {
        return roleRequestRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(req -> {
                    User u = userRepository.findById(req.getUserId()).orElse(null);
                    return mapToResponse(req, u);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleRequestResponse> getPendingRequests() {
        return roleRequestRepository.findByStatusOrderByCreatedAtDesc("PENDING").stream()
                .map(req -> {
                    User u = userRepository.findById(req.getUserId()).orElse(null);
                    return mapToResponse(req, u);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public RoleRequestResponse approveRequest(String requestId) {
        RoleRequest request = roleRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu"));

        if (!"PENDING".equals(request.getStatus())) {
            throw new RuntimeException("Yêu cầu này đã được xử lý");
        }

        // Đổi role user và lưu description
        userManagementService.updateUserRoleAndDescription(request.getUserId(), request.getRequestedRole(), request.getDescription());

        // Cập nhật trạng thái đơn
        request.setStatus("APPROVED");
        RoleRequest saved = roleRequestRepository.save(request);

        User user = userRepository.findById(saved.getUserId()).orElse(null);
        return mapToResponse(saved, user);
    }

    @Override
    @Transactional
    public RoleRequestResponse rejectRequest(String requestId) {
        RoleRequest request = roleRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu"));

        if (!"PENDING".equals(request.getStatus())) {
            throw new RuntimeException("Yêu cầu này đã được xử lý");
        }

        request.setStatus("REJECTED");
        RoleRequest saved = roleRequestRepository.save(request);

        User user = userRepository.findById(saved.getUserId()).orElse(null);
        return mapToResponse(saved, user);
    }

    private RoleRequestResponse mapToResponse(RoleRequest req, User user) {
        return RoleRequestResponse.builder()
                .id(req.getId())
                .userId(req.getUserId())
                .username(user != null ? user.getUsername() : "Unknown")
                .fullName(user != null ? user.getFullName() : "Unknown")
                .requestedRole(req.getRequestedRole())
                .description(req.getDescription())
                .status(req.getStatus())
                .createdAt(req.getCreatedAt())
                .updatedAt(req.getUpdatedAt())
                .build();
    }
}
