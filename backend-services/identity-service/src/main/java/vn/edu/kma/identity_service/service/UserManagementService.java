package vn.edu.kma.identity_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.kma.common.exception.AppException;
import vn.edu.kma.common.exception.ErrorCode;
import vn.edu.kma.common.security.UserRole;
import vn.edu.kma.identity_service.dto.request.UpdateProfileRequest;
import vn.edu.kma.identity_service.dto.response.SupplierDirectoryResponse;
import vn.edu.kma.identity_service.dto.response.UserResponse;
import vn.edu.kma.identity_service.entity.User;
import vn.edu.kma.identity_service.repository.UserRepository;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserManagementService {

    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;

    /**
     * NSX / ADMIN tìm nhà cung cấp theo từ khóa (tên, username, mô tả) để đặt hàng.
     */
    @Transactional(readOnly = true)
    public List<SupplierDirectoryResponse> searchSuppliersDirectory(String qRaw) {
        assertManufacturerOrAdmin();

        String q = qRaw == null ? "" : qRaw.trim();
        var pageable = PageRequest.of(0, 25);
        List<User> users;
        if (q.isEmpty()) {
            users = userRepository.findByRoleOrderByFullNameAsc(UserRole.SUPPLIER.name(), pageable).getContent();
        } else {
            String like = "%" + q + "%";
            users = userRepository.searchUsersByRoleAndLike(like, UserRole.SUPPLIER.name(), pageable);
        }
        return users.stream().map(this::toSupplierDirectory).collect(Collectors.toList());
    }

    /**
     * NCC / NSX / ADMIN tìm đơn vị vận chuyển để gán cho đơn hàng.
     */
    @Transactional(readOnly = true)
    public List<SupplierDirectoryResponse> searchTransportersDirectory(String qRaw) {
        assertSupplierManufacturerOrAdmin();

        String q = qRaw == null ? "" : qRaw.trim();
        var pageable = PageRequest.of(0, 25);
        List<User> users;
        if (q.isEmpty()) {
            users = userRepository.findByRoleOrderByFullNameAsc(UserRole.TRANSPORTER.name(), pageable).getContent();
        } else {
            String like = "%" + q + "%";
            users = userRepository.searchUsersByRoleAndLike(like, UserRole.TRANSPORTER.name(), pageable);
        }
        return users.stream().map(this::toSupplierDirectory).collect(Collectors.toList());
    }

    /**
     * Tra cứu tối thiểu user theo id (hiển thị tên trong chi tiết đơn hàng).
     * Cho phép các vai trò tham gia luồng đặt hàng / vận chuyển.
     */
    @Transactional(readOnly = true)
    public SupplierDirectoryResponse getDirectoryUserById(String userIdRaw) {
        assertOrderParticipantDirectoryLookup();
        if (userIdRaw == null || userIdRaw.isBlank()) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }
        String userId = userIdRaw.trim();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return toSupplierDirectory(user);
    }

    private static void assertOrderParticipantDirectoryLookup() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        boolean ok = auth.getAuthorities().stream().anyMatch(a -> {
            String x = a.getAuthority();
            return "ROLE_MANUFACTURER".equals(x)
                    || "ROLE_SUPPLIER".equals(x)
                    || "ROLE_TRANSPORTER".equals(x)
                    || "ROLE_RETAILER".equals(x)
                    || "ROLE_ADMIN".equals(x);
        });
        if (!ok) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
    }

    private static void assertSupplierManufacturerOrAdmin() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        boolean ok = auth.getAuthorities().stream().anyMatch(a ->
                "ROLE_SUPPLIER".equals(a.getAuthority())
                        || "ROLE_MANUFACTURER".equals(a.getAuthority())
                        || "ROLE_ADMIN".equals(a.getAuthority()));
        if (!ok) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
    }

    private static void assertManufacturerOrAdmin() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        boolean ok = auth.getAuthorities().stream().anyMatch(a ->
                "ROLE_MANUFACTURER".equals(a.getAuthority()) || "ROLE_ADMIN".equals(a.getAuthority()));
        if (!ok) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
    }

    private SupplierDirectoryResponse toSupplierDirectory(User u) {
        return SupplierDirectoryResponse.builder()
                .id(u.getId())
                .username(u.getUsername())
                .fullName(u.getFullName())
                .descriptionPreview(truncateDescription(u.getDescription(), 160))
                .build();
    }

    private static String truncateDescription(String desc, int max) {
        if (desc == null || desc.isBlank()) {
            return null;
        }
        String t = desc.trim();
        return t.length() <= max ? t : t.substring(0, max) + "…";
    }

    @Transactional(readOnly = true)
    public UserResponse getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return mapToResponse(user);
    }

    @Transactional
    public UserResponse updateProfile(String username, UpdateProfileRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (request.getFullName() != null) user.setFullName(request.getFullName());
        if (request.getEmail() != null) user.setEmail(request.getEmail());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getDescription() != null) user.setDescription(request.getDescription());
        if (request.getLocation() != null) user.setLocation(request.getLocation());

        return mapToResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse updateAvatar(String username, MultipartFile file) throws IOException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Upload to Cloudinary
        String avatarUrl = cloudinaryService.uploadImage(file);
        user.setAvatarUrl(avatarUrl);

        return mapToResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse updateUserRole(String userId, String roleRaw) {
        return updateUserRoleAndDescription(userId, roleRaw, null);
    }

    @Transactional
    public UserResponse updateUserRoleAndDescription(String userId, String roleRaw, String description) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        try {
            UserRole newRole = UserRole.parseRequired(roleRaw);
            user.setRole(newRole.name());
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.ROLE_INVALID);
        }

        if (description != null) {
            user.setDescription(description);
        }

        return mapToResponse(userRepository.save(user));
    }

    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .description(user.getDescription())
                .location(user.getLocation())
                .build();
    }
}
