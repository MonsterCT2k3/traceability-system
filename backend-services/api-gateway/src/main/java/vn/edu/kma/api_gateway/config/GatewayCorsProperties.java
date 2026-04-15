package vn.edu.kma.api_gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * CORS cho browser gọi API qua gateway (dev: localhost; prod: set qua env / yaml).
 */
@Data
@ConfigurationProperties(prefix = "app.cors")
public class GatewayCorsProperties {

    /**
     * Ví dụ: {@code http://localhost:*}, {@code https://yourdomain.com}
     */
    private List<String> allowedOriginPatterns = new ArrayList<>(List.of(
            "http://localhost:*",
            "http://127.0.0.1:*"
    ));

    private List<String> allowedMethods = new ArrayList<>(List.of(
            "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
    ));

    private List<String> allowedHeaders = new ArrayList<>(List.of("*"));
}
