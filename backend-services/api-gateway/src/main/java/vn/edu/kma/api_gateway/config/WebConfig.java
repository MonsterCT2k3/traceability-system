package vn.edu.kma.api_gateway.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(GatewayCorsProperties.class)
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final GatewayCorsProperties corsProperties;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        var registration = registry.addMapping("/**");
        var patterns = corsProperties.getAllowedOriginPatterns();
        if (patterns == null || patterns.isEmpty()) {
            registration.allowedOriginPatterns("*");
        } else {
            registration.allowedOriginPatterns(patterns.toArray(new String[0]));
        }
        var methods = corsProperties.getAllowedMethods();
        if (methods != null && !methods.isEmpty()) {
            registration.allowedMethods(methods.toArray(new String[0]));
        }
        var headers = corsProperties.getAllowedHeaders();
        if (headers != null && !headers.isEmpty()) {
            registration.allowedHeaders(headers.toArray(new String[0]));
        }
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
