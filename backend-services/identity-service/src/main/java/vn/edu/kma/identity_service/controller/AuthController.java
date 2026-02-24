package vn.edu.kma.identity_service.controller;

import com.nimbusds.jose.JOSEException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.kma.common.dto.request.AuthenticationRequest;
import vn.edu.kma.common.dto.request.IntrospectRequest;
import vn.edu.kma.common.dto.request.LogoutRequest;
import vn.edu.kma.common.dto.request.RefreshRequest;
import vn.edu.kma.common.dto.response.ApiResponse;
import vn.edu.kma.common.dto.response.AuthenticationResponse;
import vn.edu.kma.common.dto.response.IntrospectResponse;
import vn.edu.kma.identity_service.dto.response.UserResponse;
import vn.edu.kma.identity_service.entity.User;
import vn.edu.kma.identity_service.service.AuthenticationService;

import java.text.ParseException;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthenticationService authenticationService;

    @Value("${jwt.signerKey}")
    private String SECRET_KEY;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(@RequestBody User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<UserResponse>builder()
                        .code(201)
                        .message("Đăng ký thành công")
                        .result(authenticationService.register(user))
                        .build()
        );
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthenticationResponse>> login(@RequestBody AuthenticationRequest request) {
        AuthenticationResponse authenticationResponse = authenticationService.login(request);
        HttpStatus status = HttpStatus.OK; // 200

        return ResponseEntity.status(status).body(
                ApiResponse.<AuthenticationResponse>builder()
                        .code(status.value()) // Lấy số 200 dán vào đây
                        .message("Đăng nhập thành công")
                        .result(new AuthenticationResponse(authenticationResponse.getAccessToken(),
                                authenticationResponse.getRefreshToken()))
                        .build()
        );
    }


    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthenticationResponse>> refresh(@RequestBody RefreshRequest request) {
        AuthenticationResponse result = authenticationService.refresh(request);
        HttpStatus status = HttpStatus.OK; // 200

        return ResponseEntity.status(status).body(
                ApiResponse.<AuthenticationResponse>builder()
                        .code(200)
                        .message("Làm mới Token thành công")
                        .result(result)
                        .build()
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestBody LogoutRequest request)
            throws ParseException, JOSEException {
        authenticationService.logout(request);
        HttpStatus status = HttpStatus.OK; // 200
        return ResponseEntity.status(status).body(
                ApiResponse.<Void>builder()
                        .code(200)
                        .message("Đăng xuất thành công")
                        .build()
        );
    }

    @PostMapping("/introspect")
    public ResponseEntity<ApiResponse<IntrospectResponse>> introspect(@RequestBody IntrospectRequest request) {
        var result = authenticationService.introspect(request);
        HttpStatus status = HttpStatus.OK; // 200
        return ResponseEntity.status(status).body(
                ApiResponse.<IntrospectResponse>builder()
                        .code(200)
                        .result(result)
                        .build()
        );
    }
}