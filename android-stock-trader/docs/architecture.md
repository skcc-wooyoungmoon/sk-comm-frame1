# 아키텍처

## 1. 모듈 구성

```
AutoTrader/
├── core-trading/     순수 Kotlin (JVM). 안드로이드 의존 없음
│   ├── model/        Candle, Signal, Position, Order, Account
│   ├── indicator/    SMA, EMA, RSI, Bollinger, ATR, MACD
│   ├── strategy/     Strategy 인터페이스 + 4종 구현 + 레지스트리
│   ├── risk/         RiskPolicy(한도 설정) + RiskEngine(사이징·청산 판정)
│   ├── engine/       TradingEngine — 전략 신호 + 리스크를 합쳐 주문 의도 생성
│   ├── backtest/     Backtester + CostModel + 성과 지표
│   └── market/       MarketSession — KRX 장 시간 판단
│
└── app/              안드로이드
    ├── data/remote/  KIS Open API (Retrofit) + 인터셉터 + 토큰 관리
    ├── data/local/   암호화 저장소, Room DB, 설정 저장소
    ├── data/repository/  시세·거래 저장소 + AutoTradeRunner(1사이클 실행)
    ├── service/      TradingService(포그라운드) + 알림
    ├── ui/           Jetpack Compose 화면 5종
    └── di/           AppContainer (수동 DI)
```

### 왜 `core-trading`을 분리했는가

1. **에뮬레이터 없이 검증할 수 있다.** 매매 로직 단위 테스트 60건이 JVM에서 몇 초 만에 돕니다.
   지표 한 줄이 틀리면 조용히 돈을 잃는 영역이라, 빠른 회귀 검증이 필수입니다.
2. **서버로 옮길 수 있다.** 무중단 운용이 필요해지면 이 모듈을 그대로 JVM 서버에 올리면 됩니다.
3. **안드로이드 API가 매매 판단에 섞이지 않는다.** `Context`나 `Log`가 전략 코드에 들어오는 순간
   테스트가 어려워집니다.

---

## 2. 판단 흐름

```
TradingService (주기 루프)
   └─> AutoTradeRunner.runCycle()
         ├─ 1. 장 시간인가?                      MarketSession
         ├─ 2. 계좌 스냅샷 조회                   TradingRepository
         ├─ 3. 킬 스위치 판정 (계좌 단위 1회)      RiskEngine
         └─ 4. 종목별 반복
               ├─ 일봉 이력 + 현재가 → 봉 배열     MarketDataRepository
               ├─ TradingEngine.evaluate()
               │     ├─ (1) 손절/익절/트레일링     ← 전략보다 우선
               │     ├─ (2) 전략 신호             Strategy
               │     └─ (3) 리스크 한도 검사       RiskEngine
               ├─ dry-run이면 기록만
               └─ 아니면 주문 전송 + 쿨다운 기록    TradingRepository
```

### 순서가 곧 안전 설계다

`TradingEngine`의 평가 순서는 임의로 정한 것이 아닙니다.

**손절이 전략보다 먼저 평가됩니다.** 전략이 "더 사라"고 외치는 중이라도,
평단 대비 -3%에 닿으면 청산 주문이 먼저 나갑니다. 이 순서가 뒤집히면
"전략이 강세 신호를 내는 동안 손절이 무시되는" 최악의 시나리오가 생깁니다.

**킬 스위치는 매수만 막고 매도는 막지 않습니다.** 일일 손실 한도에 걸렸는데
청산까지 막히면 손실이 더 커집니다.

---

## 3. 관심사 분리

| 질문 | 답하는 클래스 |
|---|---|
| 사야 하나 팔아야 하나? | `Strategy` |
| 사도 되나? 얼마나? | `RiskEngine` |
| 그래서 무슨 주문을 낼까? | `TradingEngine` |
| 어떻게 보내지? | `TradingRepository` |
| 언제 돌지? | `TradingService` |

`Strategy`는 수량·현금·한도를 **전혀 모릅니다**. 그래서 전략을 새로 추가해도
손절과 한도 로직이 그대로 유지됩니다. 반대로 `RiskEngine`은 어떤 지표를 썼는지 모릅니다.

---

## 4. 백테스트와 실거래의 일치

백테스트가 실거래와 다른 코드로 돌면, 백테스트에서 검증한 내용이 아무 의미가 없습니다.
그래서 `Backtester`는 `TradingEngine`을 **그대로** 사용합니다.
전략도, 손절도, 종목당 비중 한도도 실거래와 동일한 코드가 평가합니다.

미래 참조(look-ahead) 방지:

- i번째 봉 **종가**로 판단
- 체결은 i+1번째 봉 **시가**로

종가에 판단해 종가에 체결시키는 백테스트는 실거래에서 재현되지 않는 수익을 만들어냅니다.
`BacktesterTest`의 "판단은 종가로 하고 체결은 다음 봉 시가로 이루어진다" 테스트가 이 규칙을 고정합니다.

---

## 5. 보안 설계

| 위협 | 대응 |
|---|---|
| 소스코드/APK에서 API 키 추출 | 키를 소스에 두지 않음. 앱 실행 후 입력받아 저장 |
| 기기 저장소에서 키 유출 | `EncryptedSharedPreferences` (Android Keystore) |
| 백업 파일에서 키 유출 | `allowBackup=false` + 데이터 추출 규칙에서 전 도메인 제외 |
| 로그에서 토큰·계좌번호 유출 | HTTP 본문 로깅은 디버그 빌드에서만, 레벨도 BASIC |
| 모드 전환 후 이전 서버로 주문 | `BaseUrlInterceptor`가 요청 시점에 호스트 결정, 전환 시 토큰 폐기 |

---

## 6. 오류 처리 원칙

- **KIS는 HTTP 200과 함께 본문 `rt_cd`로 실패를 알립니다.** HTTP 상태만 보면
  실패한 주문을 성공으로 착각합니다. `requireSuccess()`가 이를 강제합니다.
- **숫자 파싱 실패를 0으로 덮지 않습니다.** 현재가가 0으로 해석되면 엉뚱한 수량이 계산됩니다.
  `kisDouble()`은 해석 불가 시 예외를 던집니다.
- **한 종목의 오류가 전체를 멈추지 않습니다.** 종목 루프 안에서 잡아 매매일지에 남기고 다음 종목으로 갑니다.
- **거절 사유를 버리지 않습니다.** `Decision.blockedReasons`가 "왜 안 샀는지"를 끝까지 전달합니다.

---

## 7. 확장 지점

| 하고 싶은 것 | 손댈 곳 |
|---|---|
| 전략 추가 | `core-trading/strategy/`에 `Strategy` 구현 + `StrategyRegistry`에 등록 |
| 다른 증권사 지원 | `KisApi`를 브로커 인터페이스로 추상화, `TradingRepository` 구현 분리 |
| 실시간 시세(WebSocket) | `MarketDataRepository`에 스트림 추가, `AutoTradeRunner`를 이벤트 구동으로 전환 |
| 종목별 다른 전략 | `WatchItemEntity`에 전략 설정 컬럼 추가(마이그레이션 필요) |
| 서버 실행 | `core-trading` 모듈을 JVM 서버로 이식, `AutoTradeRunner`에 해당하는 스케줄러 작성 |
