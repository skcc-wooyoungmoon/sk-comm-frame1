# SK Framework · User Admin (프론트엔드)

SK Enterprise Framework의 **사용자/권한 관리 콘솔**입니다.
React + Vite + TypeScript로 작성되었으며, 백엔드 샘플(`basic-crud-sample`)의 `/api/users` REST API와 연동됩니다.

## 기능

- 📊 **대시보드**: 전체/활성/정지/관리자 수 집계, 최근 사용자
- 👤 **사용자 관리**
  - 목록 조회(검색: 사용자명/이메일/상태, 페이징)
  - 등록 / 수정 / 삭제
  - 상태 변경(ACTIVE / INACTIVE / SUSPENDED)
  - 권한 변경(ADMIN / MANAGER / USER)
- 표준 응답(`ApiResponse`) 언랩 및 409(멱등/동시성 충돌)·400(검증) 에러 토스트 처리

## 실행

```bash
# 1) 백엔드 먼저 실행 (다른 터미널)
cd ../../framework-samples/basic-crud-sample && mvn spring-boot:run   # :8080

# 2) 프론트엔드
npm install
npm run dev      # http://localhost:5173
```

`/api` 요청은 `vite.config.ts` 프록시를 통해 백엔드(:8080)로 전달됩니다.
백엔드는 `WebConfig`에서 `http://localhost:5173` CORS를 허용합니다.

## 빌드

```bash
npm run build    # 타입체크(tsc) + 번들(dist/)
npm run preview  # 빌드 결과 미리보기
```

## 구조

```
src/
├── api/          # client(공통 fetch 래퍼) + usersApi
├── components/   # Layout, Toast, Modal, Badges, UserFormModal
├── pages/        # DashboardPage, UserListPage
├── types/        # 백엔드 DTO 대응 타입
├── App.tsx       # 라우터
└── main.tsx      # 진입점
```

## 확장 가이드

새 도메인 화면 추가 시:
1. `src/api/<domain>Api.ts` 에 API 함수 작성(`client.ts` 재사용)
2. `src/types/` 에 타입 추가
3. `src/pages/` 에 페이지 작성 후 `App.tsx` 라우트 + `Layout.tsx` 메뉴 등록

포인트 도메인 예시는 [`framework-docs/hands-on-tutorial.md`](../../framework-docs/hands-on-tutorial.md) 참고.
