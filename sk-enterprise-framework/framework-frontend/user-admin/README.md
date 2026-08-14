# SK Framework · Admin Console (프론트엔드)

SK Enterprise Framework의 **관리자 콘솔**입니다.
React + Vite + TypeScript로 작성되었으며, 백엔드 샘플(`basic-crud-sample`)의 REST API와 연동됩니다.

## 화면 / 기능

| 화면 | 경로 | 필요 권한 | 설명 |
|------|------|-----------|------|
| 로그인 | `/login` | - | 데모 인증(시드 이메일, 비밀번호 미검증) |
| 대시보드 | `/` | dashboard:view | 사용자 집계, 최근 사용자 |
| 사용자 관리 | `/users` | user:view / user:manage | 목록/검색/페이징, 등록·수정·삭제, 상태·권한 변경 |
| 상품 관리 | `/products` | product:view / product:manage | 검색(디바운스), 등록·수정·삭제, 구매(재고차감), CSV 내보내기 |
| 주문 | `/orders` | order:view | 멱등 주문 생성(재전송 테스트), 주문 조회 |
| 모니터링 | `/monitoring` | monitoring:view | Actuator 헬스/JVM·CPU·스레드·HTTP·DB 메트릭, 자동 새로고침 |
| 로그 | `/logs` | log:view | 애플리케이션 로그 뷰어(레벨 필터, 자동 새로고침) |

### 보안 / 인증
- **로그인/로그아웃**, 토큰(localStorage) 자동 첨부(`Authorization: Bearer`)
- **역할(Role) 기반 접근제어**: `ADMIN` / `MANAGER` / `USER` → 권한(Permission) 매핑(`src/auth/permissions.ts`)
- **라우트 가드**(`ProtectedRoute`) + **메뉴/버튼 게이팅**(`useAuth().can(...)`)
- 프론트 게이팅은 UX용이며 실제 권한 검증은 백엔드가 담당

### 알고리즘 / 유틸 (`src/lib`)
- `useDebounce` — 검색 입력 디바운스
- `idempotency` — UUID / 주문번호 생성(멱등키)
- 클라이언트 **재시도**(409 충돌 시 지수 백오프, `client.ts`) — 백엔드 `@RetryOnConflict` 대응
- `csv` — CSV 내보내기, `format` — 바이트/퍼센트/기간/숫자 포매팅

## 실행

```bash
# 1) 백엔드 먼저 (다른 터미널)
cd ../../framework-samples/basic-crud-sample && mvn spring-boot:run   # :8080

# 2) 프론트엔드
npm install
npm run dev      # http://localhost:5173
```

로그인: `admin@sk.com`(ADMIN) / `manager@sk.com`(MANAGER) / `user1@sk.com`(USER) — 비밀번호는 아무 값(데모).

## 빌드

```bash
npm run build    # 타입체크(tsc) + 번들(dist/)
npm run preview
```

## 구조

```
src/
├── api/          # client(토큰 첨부+재시도) + users/products/orders/auth/admin API
├── auth/         # AuthContext, ProtectedRoute, permissions
├── components/   # Layout, Toast, Modal, Badges, UserFormModal
├── lib/          # useDebounce, idempotency, csv, format
├── pages/        # Login, Dashboard, UserList, Products, Orders, Monitoring, Logs
├── types/        # 백엔드 DTO 대응 타입
├── App.tsx       # 라우터 + 가드
└── main.tsx      # 진입점
```

## 연동 백엔드 엔드포인트
- 인증: `POST /api/auth/login`, `GET /api/auth/me`
- 사용자: `/api/users` (CRUD, 상태/권한 변경)
- 상품: `/api/products` (CRUD, `POST /{id}/purchase`)
- 주문: `/api/orders` (멱등 생성/조회)
- 모니터링: `GET /api/admin/monitoring/summary`
- 로그: `GET /api/admin/logs`
