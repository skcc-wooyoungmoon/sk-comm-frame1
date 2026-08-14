package com.sk.sample.admin.log;

import com.sk.framework.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @className    : LogController
 * @description  : 로그 뷰어 API. 인메모리로 캡처된 최근 애플리케이션 로그를 조회한다.
 * @modification : 2026.08.14(프레임워크팀) 최초생성
 * @author       : SK Framework Team
 * @date         : 2026.08.14
 * @version      : 1.0
 */
@Tag(name = "관리자 - 로그", description = "애플리케이션 로그 뷰어 API")
@RestController
@RequestMapping("/api/admin/logs")
@RequiredArgsConstructor
public class LogController {

    private final LogCaptureService logCaptureService;

    @Operation(summary = "최근 로그 조회", description = "level(ERROR/WARN/INFO/DEBUG)과 limit로 필터링 가능")
    @GetMapping
    public ResponseEntity<ApiResponse<List<LogEntry>>> recent(
            @RequestParam(required = false) String level,
            @RequestParam(defaultValue = "100") int limit) {
        return ResponseEntity.ok(ApiResponse.success(logCaptureService.recent(level, limit)));
    }
}
