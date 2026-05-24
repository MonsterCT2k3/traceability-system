package vn.edu.kma.api_gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;
import static org.springframework.web.servlet.function.RouterFunctions.route;

@SpringBootApplication
@EnableFeignClients
public class ApiGatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiGatewayApplication.class, args);
	}

	@Bean
	public RouterFunction<ServerResponse> fallbackRoute() {
		return route()
				.GET("/fallback", request -> ServerResponse.status(503)
						.header("Content-Type", "application/json")
						.body("{\"code\":503,\"message\":\"Hệ thống đang quá tải hoặc tạm thời gián đoạn. Vui lòng thử lại sau.\",\"result\":null}"))
				.POST("/fallback", request -> ServerResponse.status(503)
						.header("Content-Type", "application/json")
						.body("{\"code\":503,\"message\":\"Hệ thống đang quá tải hoặc tạm thời gián đoạn. Vui lòng thử lại sau.\",\"result\":null}"))
                .OPTIONS("/fallback", request -> ServerResponse.status(503)
                        .header("Content-Type", "application/json")
                        .body("{\"code\":503,\"message\":\"Hệ thống đang quá tải hoặc tạm thời gián đoạn. Vui lòng thử lại sau.\",\"result\":null}"))
				.build();
	}

}
