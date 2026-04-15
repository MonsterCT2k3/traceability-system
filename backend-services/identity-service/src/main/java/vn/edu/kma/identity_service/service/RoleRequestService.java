package vn.edu.kma.identity_service.service;

import vn.edu.kma.identity_service.dto.request.CreateRoleRequestDto;
import vn.edu.kma.identity_service.dto.response.RoleRequestResponse;

import java.util.List;

public interface RoleRequestService {
    RoleRequestResponse createRequest(String userId, CreateRoleRequestDto dto);
    List<RoleRequestResponse> getMyRequests(String userId);
    List<RoleRequestResponse> getPendingRequests();
    RoleRequestResponse approveRequest(String requestId);
    RoleRequestResponse rejectRequest(String requestId);
}
