# SK Enterprise Framework

🚀 Enterprise-grade Spring Boot Framework for SK Communications

## 개요

SK Enterprise Framework는 SK Communications의 엔터프라이즈급 애플리케이션 개발을 위한 Spring Boot 기반 프레임워크입니다. SOLID 원칙을 기반으로 설계되었으며, 높은 품질과 유지보수성, 그리고 뛰어난 성능을 제공합니다.

## 주요 기능

### 🔐 Security Module
- JWT 기반 인증/인가
- Role 기반 권한 관리
- API 레벨 보안 설정
- 자동 보안 검증

### 💾 Data Module
- JPA 기반 데이터 접근
- 자동 CRUD API 생성
- 동적 쿼리 지원
- 트랜잭션 관리

### 🌐 Web Module
- RESTful API 지원
- 자동 API 문서 생성
- 요청/응답 처리
- 예외 처리

### ⚡ Cache Module
- Redis 기반 캐싱
- 어노테이션 기반 캐시 관리
- 분산 캐시 지원
- 캐시 전략 최적화

### 📊 Logging Module
- 구조화된 로깅
- 성능 모니터링
- 감사 로그
- 분산 추적

## 프로젝트 구조

```
sk-enterprise-framework/
├── framework-core/              # 핵심 프레임워크
│   ├── framework-security/      # 보안 모듈
│   ├── framework-data/          # 데이터 접근 모듈
│   ├── framework-web/           # 웹 모듈
│   ├── framework-cache/         # 캐시 모듈
│   └── framework-logging/       # 로깅 모듈
├── framework-starters/          # Spring Boot Starters
├── framework-samples/           # 샘플 프로젝트
├── framework-docs/              # 문서
└── framework-tools/             # 개발 도구
```

## 빠른 시작

### 1. 의존성 추가

```xml
<dependency>
    <groupId>com.sk</groupId>
    <artifactId>sk-framework-starter-all</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. 애플리케이션 설정

```java
@SpringBootApplication
@EnableSkFramework
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### 3. 설정 파일

```yaml
sk:
  framework:
    security:
      enabled: true
      jwt:
        secret-key: your-secret-key
        expiration: 3600
    cache:
      enabled: true
      type: redis
      redis:
        host: localhost
        port: 6379
    logging:
      enabled: true
      level: INFO
```

## 문서

- [개발 가이드](framework-docs/development-guide.md)
- [API 문서](framework-docs/api-reference.md)
- [샘플 프로젝트](framework-samples/)
- [마이그레이션 가이드](framework-docs/migration-guide.md)

## 라이센스

MIT License

## 기여

SK Communications의 프레임워크 개발 가이드를 참고하여 기여해주세요.

## 지원

- 이슈 등록: [GitHub Issues](https://github.com/sk-comm/sk-enterprise-framework/issues)
- 문의: framework-team@sk.com
