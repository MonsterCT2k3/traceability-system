package vn.edu.kma.identity_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.kma.common.exception.AppException;
import vn.edu.kma.common.exception.ErrorCode;
import vn.edu.kma.common.security.UserRole;
import vn.edu.kma.identity_service.dto.response.UserResponse;
import vn.edu.kma.identity_service.entity.User;
import vn.edu.kma.identity_service.repository.UserRepository;

import vn.edu.kma.identity_service.dto.request.UpdateProfileRequest;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

@Service
@RequiredArgsConstructor
public class UserManagementService {

    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;

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
