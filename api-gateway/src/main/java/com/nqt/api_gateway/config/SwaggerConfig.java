package com.nqt.api_gateway.config;

import org.springdoc.core.properties.SwaggerUiConfigParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionWriter;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Configuration
@EnableScheduling
public class SwaggerConfig {
    @Autowired
    private DiscoveryClient discoveryClient;

    @Autowired
    private SwaggerUiConfigParameters swaggerUiConfigParameters;

    @Autowired
    private RouteDefinitionWriter routeDefinitionWriter;

    @Autowired
    private ApplicationEventPublisher publisher;

    private final Set<String> registeredServices = new HashSet<>();

    @Scheduled(fixedRate = 30000)
    public void refreshSwagger() {
        List<String> services = discoveryClient.getServices();

        for (String service : services) {
            String serviceName = service.toLowerCase();
            String shortName = serviceName.replace("-service", "");
            // Bỏ qua bản thân api-gateway và các service đã đăng ký
            if (!serviceName.equalsIgnoreCase("api-gateway") && !registeredServices.contains(serviceName)) {

                // 1. Đăng ký vào Swagger UI Group
                swaggerUiConfigParameters.addGroup(serviceName, "/v3/api-docs/" + serviceName);

                // 2. Định nghĩa Route cho Swagger JSON (Chuyển tiếp docs)
                RouteDefinition swaggerRoute = new RouteDefinition();
                swaggerRoute.setId("docs_" + serviceName);
                swaggerRoute.setUri(URI.create("lb://" + serviceName));
                swaggerRoute.setPredicates(List.of(new PredicateDefinition("Path=/v3/api-docs/" + serviceName)));
                swaggerRoute.setFilters(List.of(new FilterDefinition("RewritePath=/v3/api-docs/" + serviceName + ", /v3/api-docs")));

                // 3. ĐỊNH NGHĨA ROUTE API (Ví dụ: /api/user/** -> /user/**)
                RouteDefinition apiRoute = new RouteDefinition();
                apiRoute.setId("api_" + serviceName);
                apiRoute.setUri(URI.create("lb://" + serviceName));

                // Khớp các request bắt đầu bằng /api/ten-service/
                apiRoute.setPredicates(List.of(new PredicateDefinition("Path=/api/" + shortName + "/**")));

                // Quan trọng: Loại bỏ prefix /api/shortName trước khi gửi đến service con
                // Ví dụ: Gateway nhận /api/user/profile -> Service con nhận /profile
                apiRoute.setFilters(List.of(new FilterDefinition("RewritePath=/api/" + shortName + "/(?<remaining>.*), /${remaining}")));

                // 4. Lưu và Refresh
                routeDefinitionWriter.save(Mono.just(swaggerRoute)).subscribe();
                routeDefinitionWriter.save(Mono.just(apiRoute)).subscribe();

                registeredServices.add(serviceName);
                System.out.println("✅ Đã nạp Route và Swagger cho: " + serviceName);
                // Cực kỳ quan trọng: Refresh lại bảng định tuyến của Gateway
                publisher.publishEvent(new RefreshRoutesEvent(this));
            }
        }
    }
}
