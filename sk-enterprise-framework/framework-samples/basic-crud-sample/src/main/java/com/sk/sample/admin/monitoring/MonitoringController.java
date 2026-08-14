package com.sk.sample.admin.monitoring;

import com.sk.framework.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.actuate.metrics.MetricsEndpoint;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @className    : MonitoringController
 * @description  : 운영 모니터링 API. Actuator Health/Metrics 엔드포인트를 서버 사이드에서 읽어
 *                 관리자 콘솔이 쓰기 좋은 요약 형태로 제공한다.(Actuator를 CORS로 노출하지 않아도 됨)
 * @modification : 2026.08.14(프레임워크팀) 최초생성
 * @author       : SK Framework Team
 * @date         : 2026.08.14
 * @version      : 1.0
 */
@Tag(name = "관리자 - 모니터링", description = "헬스/메트릭 요약 API")
@RestController
@RequestMapping("/api/admin/monitoring")
@RequiredArgsConstructor
public class MonitoringController {

    private final HealthEndpoint healthEndpoint;
    private final MetricsEndpoint metricsEndpoint;

    @Operation(summary = "모니터링 요약", description = "헬스 상태와 핵심 JVM/시스템 메트릭을 요약합니다.")
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> summary() {
        Map<String, Object> result = new LinkedHashMap<>();

        Status status = healthEndpoint.health().getStatus();
        result.put("status", status.getCode());

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("jvmMemoryUsed", metric("jvm.memory.used"));
        metrics.put("jvmMemoryMax", metric("jvm.memory.max"));
        metrics.put("cpuUsage", metric("system.cpu.usage"));
        metrics.put("processCpuUsage", metric("process.cpu.usage"));
        metrics.put("uptimeSeconds", metric("process.uptime"));
        metrics.put("liveThreads", metric("jvm.threads.live"));
        metrics.put("httpRequestCount", metric("http.server.requests", "COUNT"));
        metrics.put("dbConnectionsActive", metric("hikaricp.connections.active"));
        result.put("metrics", metrics);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    private Double metric(String name) {
        return metric(name, "VALUE");
    }

    /** 지정 통계(statistic)의 측정값을 합산하여 반환한다. 없으면 null. */
    private Double metric(String name, String statistic) {
        try {
            MetricsEndpoint.MetricDescriptor response = metricsEndpoint.metric(name, null);
            if (response == null || response.getMeasurements() == null) {
                return null;
            }
            List<MetricsEndpoint.Sample> samples = response.getMeasurements();
            return samples.stream()
                    .filter(s -> statistic == null || s.getStatistic().name().equalsIgnoreCase(statistic))
                    .mapToDouble(MetricsEndpoint.Sample::getValue)
                    .sum();
        } catch (Exception e) {
            return null;
        }
    }
}
