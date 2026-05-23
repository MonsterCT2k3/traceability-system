package vn.edu.kma.api_gateway.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import vn.edu.kma.common.dto.request.IntrospectRequest;
import vn.edu.kma.common.dto.response.ApiResponse;
import vn.edu.kma.common.dto.response.IntrospectResponse;

@FeignClient(name = "identity-service", url = "${app.services.identity.url:http://localhost:8081}")
public interface IdentityClient {

    @PostMapping("/api/v1/auth/introspect")
    ApiResponse<IntrospectResponse> introspectToken(@RequestBody IntrospectRequest request);
}
