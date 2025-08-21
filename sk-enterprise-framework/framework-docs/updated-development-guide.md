# SK Enterprise Framework - 업데이트된 개발 가이드

🚀 **Enterprise급 Spring Boot 프레임워크 개발 완료 가이드**

## 📋 목차
1. [프레임워크 개요](#1-프레임워크-개요)
2. [아키텍처 및 구조](#2-아키텍처-및-구조)
3. [핵심 모듈 상세](#3-핵심-모듈-상세)
4. [사용법 가이드](#4-사용법-가이드)
5. [개발 예제](#5-개발-예제)
6. [고급 기능](#6-고급-기능)
7. [운영 가이드](#7-운영-가이드)
8. [확장 개발 가이드](#8-확장-개발-가이드)

---

## 1. 프레임워크 개요

### 1.1 SK Enterprise Framework 소개
SK Enterprise Framework는 **SOLID 원칙**을 기반으로 설계된 Enterprise급 Spring Boot 프레임워크입니다. 
고품질, 유지보수성, 성능을 중시하며 다음과 같은 특징을 가집니다:

#### 🎯 핵심 목표
- **생산성 향상**: 반복적인 개발 작업 최소화
- **품질 보장**: SOLID 원칙 기반 설계로 높은 코드 품질
- **표준화**: 일관된 개발 표준 제공
- **확장성**: 모듈형 구조로 쉬운 확장

#### 🔧 주요 기능
- **보안**: JWT 기반 인증/인가, Role 기반 권한 관리
- **데이터**: JPA 기반 데이터 접근, 동적 쿼리, 자동 CRUD
- **웹**: RESTful API, 전역 예외 처리, API 문서화
- **캐시**: Redis 기반 캐싱, 어노테이션 기반 캐시 관리
- **로깅**: 구조화된 로깅, 성능 모니터링

### 1.2 SOLID 원칙 적용

#### Single Responsibility Principle (SRP)
- 각 클래스는 하나의 책임만 가짐
- `JwtTokenProvider`: JWT 토큰 관련 기능만 담당
- `BaseService`: 기본 CRUD 서비스 로직만 담당

#### Open-Closed Principle (OCP)
- 확장에는 열려있고 수정에는 닫혀있음
- `BaseController`, `BaseService`를 상속하여 확장
- 인터페이스 기반 설계로 구현체 교체 가능

#### Liskov Substitution Principle (LSP)
- 파생 클래스는 기반 클래스를 대체 가능
- 모든 Repository는 `BaseRepository` 대체 가능

#### Interface Segregation Principle (ISP)
- 클라이언트는 자신이 사용하지 않는 메서드에 의존하지 않음
- 모듈별 독립적인 인터페이스 제공

#### Dependency Inversion Principle (DIP)
- 고수준 모듈은 저수준 모듈에 의존하지 않음
- 생성자 주입을 통한 의존성 역전

---

## 2. 아키텍처 및 구조

### 2.1 전체 프로젝트 구조
```
sk-enterprise-framework/
├── framework-core/              # 핵심 프레임워크 모듈
│   ├── framework-common/        # 공통 기능 (Base 클래스, 유틸리티)
│   ├── framework-security/      # 보안 모듈 (JWT, 권한 관리)
│   ├── framework-data/          # 데이터 접근 모듈 (JPA, QueryDSL)
│   ├── framework-web/           # 웹 모듈 (REST API, 예외 처리)
│   ├── framework-cache/         # 캐시 모듈 (Redis, 캐시 관리)
│   └── framework-logging/       # 로깅 모듈 (구조화 로깅)
├── framework-starters/          # Spring Boot Starters
│   ├── sk-framework-starter-security/
│   ├── sk-framework-starter-data/
│   ├── sk-framework-starter-web/
│   ├── sk-framework-starter-cache/
│   ├── sk-framework-starter-logging/
│   └── sk-framework-starter-all/    # 전체 스타터
├── framework-samples/           # 샘플 프로젝트
│   └── basic-crud-sample/       # 기본 CRUD 샘플
├── framework-docs/              # 문서
└── framework-tools/             # 개발 도구
```

### 2.2 계층형 아키텍처
```
┌─────────────────────────────────────┐
│         Presentation Layer          │  ← Controllers, REST APIs
├─────────────────────────────────────┤
│         Application Layer           │  ← Services, Business Logic
├─────────────────────────────────────┤
│           Domain Layer              │  ← Entities, Domain Objects
├─────────────────────────────────────┤
│        Infrastructure Layer         │  ← Repositories, External APIs
└─────────────────────────────────────┘
```

---

## 3. 핵심 모듈 상세

### 3.1 Common 모듈

#### 기본 엔티티 (BaseEntity)
```java
@Entity
@Table(name = "users")
public class User extends BaseEntity {
    @NotBlank
    @Column(name = "username", unique = true)
    private String username;
    
    @Email
    @Column(name = "email", unique = true)  
    private String email;
    
    // getters, setters...
}
```

#### 기본 컨트롤러 (BaseController)
```java
@RestController
@RequestMapping("/api/users")
public class UserController extends BaseController<User, Long> {
    
    private final UserService userService;
    
    public UserController(UserService userService) {
        this.userService = userService;
    }
    
    @Override
    protected BaseService<User, Long> getService() {
        return userService;
    }
}
```

#### 기본 서비스 (BaseService)
```java
@Service
@Transactional(readOnly = true)
public class UserService extends BaseService<User, Long> {
    
    private final UserRepository userRepository;
    
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    @Override
    protected JpaRepository<User, Long> getRepository() {
        return userRepository;
    }
    
    @Override
    protected String getEntityName() {
        return "User";
    }
}
```

### 3.2 Security 모듈

#### JWT 기반 인증
```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    private final JwtTokenProvider jwtTokenProvider;
    
    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@RequestBody LoginRequest request) {
        // 인증 로직
        Authentication authentication = authenticate(request);
        
        // JWT 토큰 생성
        String accessToken = jwtTokenProvider.generateAccessToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);
        
        TokenResponse response = new TokenResponse(accessToken, refreshToken);
        return ApiResponse.success(response, "Login successful");
    }
}
```

#### Role 기반 권한 관리
```java
@RestController
@RequestMapping("/api/admin")
@RequireRole("ADMIN")  // 클래스 레벨에서 ADMIN 역할 필요
public class AdminController {
    
    @GetMapping("/users")
    @RequireRole(value = {"SUPER_ADMIN", "USER_ADMIN"}, operation = RequireRole.LogicalOperator.OR)
    public ApiResponse<List<User>> getAllUsers() {
        // SUPER_ADMIN 또는 USER_ADMIN 역할이 필요
        return ApiResponse.success(userService.findAll());
    }
}
```

### 3.3 Data 모듈

#### 동적 쿼리 지원
```java
@Service
public class UserSearchService {
    
    @Autowired
    private UserRepository userRepository;
    
    public Page<User> searchUsers(String username, String email, Boolean active, Pageable pageable) {
        Specification<User> spec = Specification.where(null);
        
        if (username != null) {
            spec = spec.and(BaseSpecification.like("username", username));
        }
        
        if (email != null) {
            spec = spec.and(BaseSpecification.like("email", email));
        }
        
        if (active != null) {
            spec = spec.and(BaseSpecification.equal("active", active));
        }
        
        return userRepository.findAll(spec, pageable);
    }
}
```

#### 자동 CRUD 생성
```java
@Entity
@Table(name = "products")
@AutoCrud(path = "/api/products", enableDelete = false)  // DELETE API 제외
public class Product extends BaseEntity {
    
    @Column(name = "name", nullable = false)
    private String name;
    
    @Column(name = "price", nullable = false)
    private BigDecimal price;
    
    // getters, setters...
}
```

### 3.4 Cache 모듈

#### 어노테이션 기반 캐싱
```java
@Service
public class ProductService extends BaseService<Product, Long> {
    
    @AutoCache(key = "product:{#id}", expireTime = 30, timeUnit = TimeUnit.MINUTES)
    public Product findById(Long id) {
        return super.findById(id);
    }
    
    @AutoCache(key = "products:all", expireTime = 10, timeUnit = TimeUnit.MINUTES)
    public List<Product> findAllActive() {
        return repository.findByActiveTrue();
    }
    
    @AutoCache(evict = true, key = "product:{#product.id}")
    @Override
    public Product save(Product product) {
        return super.save(product);
    }
}
```

---

## 4. 사용법 가이드

### 4.1 프레임워크 시작하기

#### 1단계: 의존성 추가
```xml
<dependency>
    <groupId>com.sk</groupId>
    <artifactId>sk-framework-starter-all</artifactId>
    <version>1.0.0</version>
</dependency>
```

#### 2단계: 메인 애플리케이션 설정
```java
@SpringBootApplication
@EnableSkFramework  // SK Framework 활성화
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

#### 3단계: 설정 파일
```yaml
sk:
  framework:
    security:
      enabled: true
      jwt:
        secret-key: your-secret-key-here
        access-token-expiration: 3600
    data:
      enabled: true
    web:
      enabled: true
    cache:
      enabled: true
      redis:
        host: localhost
        port: 6379
    logging:
      enabled: true
```

### 4.2 엔티티 정의
```java
@Entity
@Table(name = "orders")
@AutoCrud(path = "/api/orders", enablePaging = true, enableSearch = true)
public class Order extends BaseEntity {
    
    @Column(name = "order_number", unique = true)
    private String orderNumber;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private OrderStatus status;
    
    @Column(name = "total_amount")
    private BigDecimal totalAmount;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    
    // constructors, getters, setters...
}
```

### 4.3 Repository 정의
```java
public interface OrderRepository extends BaseRepository<Order, Long> {
    
    List<Order> findByUserIdAndStatus(Long userId, OrderStatus status);
    
    @Query("SELECT o FROM Order o WHERE o.totalAmount >= :minAmount")
    Page<Order> findByMinAmount(@Param("minAmount") BigDecimal minAmount, Pageable pageable);
}
```

### 4.4 Service 구현
```java
@Service
@Transactional(readOnly = true)
public class OrderService extends BaseService<Order, Long> {
    
    private final OrderRepository orderRepository;
    
    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }
    
    @Override
    protected JpaRepository<Order, Long> getRepository() {
        return orderRepository;
    }
    
    @Override
    protected String getEntityName() {
        return "Order";
    }
    
    // 비즈니스 로직 추가
    @AutoCache(key = "user:{#userId}:orders", expireTime = 15, timeUnit = TimeUnit.MINUTES)
    public List<Order> findUserOrders(Long userId) {
        return orderRepository.findByUserIdAndStatus(userId, OrderStatus.COMPLETED);
    }
    
    @Transactional
    public Order processOrder(Order order) {
        // 주문 처리 비즈니스 로직
        validateOrder(order);
        order.setOrderNumber(generateOrderNumber());
        order.setStatus(OrderStatus.PROCESSING);
        
        return save(order);
    }
    
    private void validateOrder(Order order) {
        // 검증 로직
        ValidationUtils.notNull(order, "Order cannot be null");
        ValidationUtils.notNull(order.getUser(), "Order must have a user");
        ValidationUtils.positive(order.getTotalAmount().doubleValue(), "Total amount must be positive");
    }
}
```

### 4.5 Controller 구현
```java
@RestController
@RequestMapping("/api/orders")
@RequireRole("USER")  // 최소 USER 역할 필요
public class OrderController extends BaseController<Order, Long> {
    
    private final OrderService orderService;
    
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }
    
    @Override
    protected BaseService<Order, Long> getService() {
        return orderService;
    }
    
    // 커스텀 엔드포인트 추가
    @GetMapping("/user/{userId}")
    @RequireRole("ADMIN")  // ADMIN만 다른 사용자의 주문 조회 가능
    public ResponseEntity<ApiResponse<List<Order>>> getUserOrders(@PathVariable Long userId) {
        List<Order> orders = orderService.findUserOrders(userId);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }
    
    @PostMapping("/{id}/process")
    @RequireRole("ADMIN")
    public ResponseEntity<ApiResponse<Order>> processOrder(@PathVariable Long id) {
        Order order = orderService.findById(id);
        Order processedOrder = orderService.processOrder(order);
        return ResponseEntity.ok(ApiResponse.success(processedOrder, "Order processed successfully"));
    }
}
```

---

## 5. 개발 예제

### 5.1 완전한 CRUD 예제

#### User 엔티티
```java
@Entity
@Table(name = "users")
@AutoCrud(path = "/api/users", enableDelete = false)
public class User extends BaseEntity {
    
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50)
    @Column(name = "username", unique = true)
    private String username;
    
    @Email(message = "Email should be valid")
    @Column(name = "email", unique = true)
    private String email;
    
    @NotBlank(message = "Name is required")
    @Column(name = "name")
    private String name;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private UserRole role = UserRole.USER;
    
    @Column(name = "active")
    private Boolean active = true;
    
    // constructors, getters, setters...
}

enum UserRole {
    ADMIN, MANAGER, USER
}
```

#### UserRepository
```java
public interface UserRepository extends BaseRepository<User, Long> {
    
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    List<User> findByActiveTrue();
    Page<User> findByRole(UserRole role, Pageable pageable);
    
    @Query("SELECT u FROM User u WHERE u.name LIKE %:name% AND u.active = true")
    List<User> findActiveUsersByNameContaining(@Param("name") String name);
}
```

#### UserService
```java
@Service
@Transactional(readOnly = true)
public class UserService extends BaseService<User, Long> {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
    
    @Override
    protected JpaRepository<User, Long> getRepository() {
        return userRepository;
    }
    
    @Override
    protected String getEntityName() {
        return "User";
    }
    
    @AutoCache(key = "user:username:{#username}", expireTime = 30)
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    @Transactional
    @Override
    public User save(User user) {
        // 사용자명 중복 검사
        if (user.isNew() && userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username already exists: " + user.getUsername());
        }
        
        // 이메일 중복 검사
        if (user.isNew() && userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already exists: " + user.getEmail());
        }
        
        return super.save(user);
    }
    
    @AutoCache(key = "users:active", expireTime = 10)
    public List<User> findActiveUsers() {
        return userRepository.findByActiveTrue();
    }
    
    public Page<User> searchUsers(String username, String email, String name, 
                                  UserRole role, Boolean active, Pageable pageable) {
        Specification<User> spec = Specification.where(null);
        
        if (StringUtils.isNotBlank(username)) {
            spec = spec.and(BaseSpecification.like("username", username));
        }
        
        if (StringUtils.isNotBlank(email)) {
            spec = spec.and(BaseSpecification.like("email", email));
        }
        
        if (StringUtils.isNotBlank(name)) {
            spec = spec.and(BaseSpecification.like("name", name));
        }
        
        if (role != null) {
            spec = spec.and(BaseSpecification.equal("role", role));
        }
        
        if (active != null) {
            spec = spec.and(BaseSpecification.equal("active", active));
        }
        
        return userRepository.findAll(spec, pageable);
    }
}
```

#### UserController
```java
@RestController
@RequestMapping("/api/users")
@RequireRole("USER")
public class UserController extends BaseController<User, Long> {
    
    private final UserService userService;
    
    public UserController(UserService userService) {
        this.userService = userService;
    }
    
    @Override
    protected BaseService<User, Long> getService() {
        return userService;
    }
    
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<User>>> searchUsers(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) Boolean active,
            Pageable pageable) {
        
        Page<User> users = userService.searchUsers(username, email, name, role, active, pageable);
        PageResponse<User> pageResponse = PageResponse.of(users);
        return ResponseEntity.ok(ApiResponse.success(pageResponse));
    }
    
    @GetMapping("/active")
    @RequireRole("ADMIN")
    public ResponseEntity<ApiResponse<List<User>>> getActiveUsers() {
        List<User> activeUsers = userService.findActiveUsers();
        return ResponseEntity.ok(ApiResponse.success(activeUsers));
    }
    
    @PutMapping("/{id}/deactivate")
    @RequireRole("ADMIN")
    public ResponseEntity<ApiResponse<User>> deactivateUser(@PathVariable Long id) {
        User user = userService.findById(id);
        user.setActive(false);
        User updatedUser = userService.save(user);
        return ResponseEntity.ok(ApiResponse.success(updatedUser, "User deactivated successfully"));
    }
}
```

### 5.2 복잡한 비즈니스 로직 예제

#### 주문 처리 서비스
```java
@Service
@Transactional(readOnly = true)
public class OrderProcessingService {
    
    private final OrderService orderService;
    private final ProductService productService;
    private final UserService userService;
    private final EmailService emailService;
    
    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        // 사용자 검증
        User user = userService.findById(request.getUserId());
        ValidationUtils.isTrue(user.getActive(), "User is not active");
        
        // 상품 검증 및 재고 확인
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        
        for (OrderItemRequest itemRequest : request.getItems()) {
            Product product = productService.findById(itemRequest.getProductId());
            ValidationUtils.isTrue(product.getStock() >= itemRequest.getQuantity(), 
                    "Insufficient stock for product: " + product.getName());
            
            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(product);
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setUnitPrice(product.getPrice());
            orderItem.setTotalPrice(product.getPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity())));
            
            orderItems.add(orderItem);
            totalAmount = totalAmount.add(orderItem.getTotalPrice());
        }
        
        // 주문 생성
        Order order = new Order();
        order.setUser(user);
        order.setOrderNumber(generateOrderNumber());
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(totalAmount);
        order.setOrderItems(orderItems);
        
        // 재고 차감
        updateProductStock(orderItems);
        
        // 주문 저장
        Order savedOrder = orderService.save(order);
        
        // 이메일 발송
        emailService.sendOrderConfirmation(user.getEmail(), savedOrder);
        
        return savedOrder;
    }
    
    @Transactional
    @RequireRole("ADMIN")
    public Order processPayment(Long orderId, PaymentRequest paymentRequest) {
        Order order = orderService.findById(orderId);
        ValidationUtils.equals(order.getStatus(), OrderStatus.PENDING, 
                "Order must be in PENDING status");
        
        // 결제 처리 로직
        boolean paymentSuccess = processPaymentInternal(paymentRequest);
        
        if (paymentSuccess) {
            order.setStatus(OrderStatus.PAID);
            order.setPaymentDate(LocalDateTime.now());
        } else {
            order.setStatus(OrderStatus.PAYMENT_FAILED);
            // 재고 복구
            restoreProductStock(order.getOrderItems());
        }
        
        return orderService.save(order);
    }
    
    private void updateProductStock(List<OrderItem> orderItems) {
        for (OrderItem item : orderItems) {
            Product product = item.getProduct();
            product.setStock(product.getStock() - item.getQuantity());
            productService.save(product);
        }
    }
    
    private String generateOrderNumber() {
        return "ORD" + System.currentTimeMillis();
    }
}
```

---

## 6. 고급 기능

### 6.1 커스텀 어노테이션 생성

#### @AuditLog 어노테이션
```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditLog {
    String action() default "";
    String description() default "";
    boolean includeParameters() default false;
    boolean includeResult() default false;
}
```

#### AuditLogAspect
```java
@Aspect
@Component
@Slf4j
public class AuditLogAspect {
    
    @Around("@annotation(auditLog)")
    public Object logAudit(ProceedingJoinPoint joinPoint, AuditLog auditLog) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        
        // 시작 로깅
        log.info("=== Audit Log Start ===");
        log.info("Class: {}, Method: {}", className, methodName);
        log.info("Action: {}", StringUtils.isNotBlank(auditLog.action()) ? auditLog.action() : methodName);
        log.info("Description: {}", auditLog.description());
        
        if (auditLog.includeParameters()) {
            Object[] args = joinPoint.getArgs();
            log.info("Parameters: {}", Arrays.toString(args));
        }
        
        long startTime = System.currentTimeMillis();
        
        try {
            Object result = joinPoint.proceed();
            
            if (auditLog.includeResult()) {
                log.info("Result: {}", result);
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("Exception occurred: {}", e.getMessage());
            throw e;
        } finally {
            long executionTime = System.currentTimeMillis() - startTime;
            log.info("Execution time: {} ms", executionTime);
            log.info("=== Audit Log End ===");
        }
    }
}
```

#### 사용 예제
```java
@Service
public class UserService extends BaseService<User, Long> {
    
    @AuditLog(action = "CREATE_USER", description = "Create new user", includeParameters = true)
    @Override
    public User save(User user) {
        return super.save(user);
    }
    
    @AuditLog(action = "DELETE_USER", description = "Delete user by ID", includeParameters = true)
    @Override
    public void deleteById(Long id) {
        super.deleteById(id);
    }
}
```

### 6.2 동적 Query 생성기

#### QueryBuilder 클래스
```java
@Component
public class QueryBuilder<T> {
    
    private final EntityManager entityManager;
    
    public QueryBuilder(EntityManager entityManager) {
        this.entityManager = entityManager;
    }
    
    public QueryBuilderContext<T> select(Class<T> entityClass) {
        return new QueryBuilderContext<>(entityManager, entityClass);
    }
    
    public static class QueryBuilderContext<T> {
        private final EntityManager entityManager;
        private final Class<T> entityClass;
        private final List<Predicate> predicates = new ArrayList<>();
        private final List<Order> orders = new ArrayList<>();
        
        private CriteriaBuilder cb;
        private CriteriaQuery<T> query;
        private Root<T> root;
        
        public QueryBuilderContext(EntityManager entityManager, Class<T> entityClass) {
            this.entityManager = entityManager;
            this.entityClass = entityClass;
            
            this.cb = entityManager.getCriteriaBuilder();
            this.query = cb.createQuery(entityClass);
            this.root = query.from(entityClass);
        }
        
        public QueryBuilderContext<T> where(String fieldName, Object value) {
            if (value != null) {
                predicates.add(cb.equal(root.get(fieldName), value));
            }
            return this;
        }
        
        public QueryBuilderContext<T> like(String fieldName, String value) {
            if (StringUtils.isNotBlank(value)) {
                predicates.add(cb.like(cb.lower(root.get(fieldName)), "%" + value.toLowerCase() + "%"));
            }
            return this;
        }
        
        public QueryBuilderContext<T> between(String fieldName, Comparable from, Comparable to) {
            if (from != null || to != null) {
                if (from != null && to != null) {
                    predicates.add(cb.between(root.get(fieldName), from, to));
                } else if (from != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get(fieldName), from));
                } else {
                    predicates.add(cb.lessThanOrEqualTo(root.get(fieldName), to));
                }
            }
            return this;
        }
        
        public QueryBuilderContext<T> orderBy(String fieldName, boolean ascending) {
            Order order = ascending ? cb.asc(root.get(fieldName)) : cb.desc(root.get(fieldName));
            orders.add(order);
            return this;
        }
        
        public List<T> getResultList() {
            if (!predicates.isEmpty()) {
                query.where(cb.and(predicates.toArray(new Predicate[0])));
            }
            
            if (!orders.isEmpty()) {
                query.orderBy(orders);
            }
            
            return entityManager.createQuery(query).getResultList();
        }
        
        public List<T> getResultList(int maxResults) {
            if (!predicates.isEmpty()) {
                query.where(cb.and(predicates.toArray(new Predicate[0])));
            }
            
            if (!orders.isEmpty()) {
                query.orderBy(orders);
            }
            
            return entityManager.createQuery(query)
                    .setMaxResults(maxResults)
                    .getResultList();
        }
    }
}
```

#### 사용 예제
```java
@Service
public class UserSearchService {
    
    private final QueryBuilder<User> queryBuilder;
    
    public UserSearchService(QueryBuilder<User> queryBuilder) {
        this.queryBuilder = queryBuilder;
    }
    
    public List<User> searchUsers(UserSearchCriteria criteria) {
        return queryBuilder.select(User.class)
                .where("active", criteria.getActive())
                .like("username", criteria.getUsername())
                .like("email", criteria.getEmail())
                .like("name", criteria.getName())
                .between("createdAt", criteria.getFromDate(), criteria.getToDate())
                .orderBy("createdAt", false)  // 최신 순
                .getResultList(100);  // 최대 100개
    }
}
```

---

## 7. 운영 가이드

### 7.1 설정 관리

#### 환경별 설정 파일
```yaml
# application-local.yml
sk:
  framework:
    security:
      debug: true
      jwt:
        secret-key: local-development-key
    logging:
      level: DEBUG

spring:
  h2:
    console:
      enabled: true

---
# application-dev.yml  
sk:
  framework:
    security:
      debug: false
      jwt:
        secret-key: ${JWT_SECRET_KEY:dev-secret-key}
    cache:
      redis:
        host: ${REDIS_HOST:dev-redis.example.com}

---
# application-prod.yml
sk:
  framework:
    security:
      debug: false
      jwt:
        secret-key: ${JWT_SECRET_KEY}
        access-token-expiration: 1800  # 30분
    cache:
      redis:
        host: ${REDIS_HOST}
        port: ${REDIS_PORT:6379}
        password: ${REDIS_PASSWORD}
```

### 7.2 모니터링 및 헬스체크

#### HealthCheck Controller
```java
@RestController
@RequestMapping("/health")
public class HealthCheckController {
    
    private final DataSource dataSource;
    private final RedisTemplate<String, Object> redisTemplate;
    
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        
        // Database 상태 확인
        health.put("database", checkDatabase());
        
        // Redis 상태 확인  
        health.put("redis", checkRedis());
        
        // Memory 상태 확인
        health.put("memory", checkMemory());
        
        return ResponseEntity.ok(ApiResponse.success(health));
    }
    
    private Map<String, Object> checkDatabase() {
        Map<String, Object> dbHealth = new HashMap<>();
        try {
            Connection connection = dataSource.getConnection();
            dbHealth.put("status", "UP");
            dbHealth.put("database", connection.getMetaData().getDatabaseProductName());
            connection.close();
        } catch (Exception e) {
            dbHealth.put("status", "DOWN");
            dbHealth.put("error", e.getMessage());
        }
        return dbHealth;
    }
    
    private Map<String, Object> checkRedis() {
        Map<String, Object> redisHealth = new HashMap<>();
        try {
            redisTemplate.opsForValue().set("health-check", "test", Duration.ofSeconds(10));
            String value = (String) redisTemplate.opsForValue().get("health-check");
            redisHealth.put("status", "test".equals(value) ? "UP" : "DOWN");
        } catch (Exception e) {
            redisHealth.put("status", "DOWN");
            redisHealth.put("error", e.getMessage());
        }
        return redisHealth;
    }
    
    private Map<String, Object> checkMemory() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        Map<String, Object> memoryHealth = new HashMap<>();
        memoryHealth.put("total", totalMemory / 1024 / 1024 + " MB");
        memoryHealth.put("used", usedMemory / 1024 / 1024 + " MB");
        memoryHealth.put("free", freeMemory / 1024 / 1024 + " MB");
        memoryHealth.put("usage_percent", (usedMemory * 100) / totalMemory);
        
        return memoryHealth;
    }
}
```

### 7.3 성능 최적화

#### 데이터베이스 연결 풀 설정
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 20000
      idle-timeout: 300000
      max-lifetime: 1200000
      leak-detection-threshold: 60000
      
  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 25
        order_inserts: true
        order_updates: true
        batch_versioned_data: true
```

#### 캐시 전략 설정
```yaml
sk:
  framework:
    cache:
      redis:
        host: redis.example.com
        port: 6379
        timeout: 2000ms
        lettuce:
          pool:
            max-active: 8
            max-idle: 8
            min-idle: 0
```

---

## 8. 확장 개발 가이드

### 8.1 새로운 모듈 추가

#### 1. 모듈 구조 생성
```
framework-core/
└── framework-notification/
    ├── pom.xml
    └── src/
        └── main/
            ├── java/
            │   └── com/sk/framework/notification/
            │       ├── config/
            │       │   └── NotificationAutoConfiguration.java
            │       ├── service/
            │       │   ├── EmailService.java
            │       │   └── SmsService.java
            │       └── model/
            │           └── NotificationRequest.java
            └── resources/
                └── META-INF/
                    └── spring.factories
```

#### 2. AutoConfiguration 작성
```java
@AutoConfiguration
@ConditionalOnProperty(prefix = "sk.framework.notification", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(NotificationProperties.class)
@ComponentScan(basePackages = "com.sk.framework.notification")
public class NotificationAutoConfiguration {
    
    @Bean
    @ConditionalOnProperty(prefix = "sk.framework.notification.email", name = "enabled", havingValue = "true")
    public EmailService emailService(NotificationProperties properties) {
        return new EmailServiceImpl(properties);
    }
    
    @Bean
    @ConditionalOnProperty(prefix = "sk.framework.notification.sms", name = "enabled", havingValue = "true")
    public SmsService smsService(NotificationProperties properties) {
        return new SmsServiceImpl(properties);
    }
}
```

#### 3. Properties 클래스
```java
@ConfigurationProperties(prefix = "sk.framework.notification")
@Data
public class NotificationProperties {
    
    private boolean enabled = true;
    private Email email = new Email();
    private Sms sms = new Sms();
    
    @Data
    public static class Email {
        private boolean enabled = true;
        private String host;
        private int port = 587;
        private String username;
        private String password;
        private boolean ssl = true;
    }
    
    @Data
    public static class Sms {
        private boolean enabled = true;
        private String apiKey;
        private String apiSecret;
        private String fromNumber;
    }
}
```

#### 4. spring.factories 등록
```properties
# Auto Configuration
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
com.sk.framework.notification.config.NotificationAutoConfiguration
```

### 8.2 기존 기능 확장

#### BaseController 확장 예제
```java
public abstract class ExtendedBaseController<T extends BaseEntity, ID> 
        extends BaseController<T, ID> {
    
    /**
     * 엔티티 일괄 생성
     */
    @PostMapping("/batch")
    public ResponseEntity<ApiResponse<List<T>>> createBatch(@Valid @RequestBody List<T> entities) {
        List<T> createdEntities = entities.stream()
                .map(entity -> getService().save(entity))
                .collect(Collectors.toList());
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(createdEntities, 
                        String.format("%d entities created successfully", createdEntities.size())));
    }
    
    /**
     * 조건부 삭제
     */
    @DeleteMapping("/conditional")
    public ResponseEntity<ApiResponse<Integer>> deleteConditional(@RequestBody Map<String, Object> conditions) {
        int deletedCount = getService().deleteConditional(conditions);
        return ResponseEntity.ok(ApiResponse.success(deletedCount, 
                String.format("%d entities deleted", deletedCount)));
    }
}
```

### 8.3 테스트 작성 가이드

#### 단위 테스트
```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @InjectMocks
    private UserService userService;
    
    @Test
    @DisplayName("사용자 생성 - 성공")
    void createUser_Success() {
        // Given
        User user = new User("testuser", "test@example.com", "Test User");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(user);
        
        // When
        User result = userService.save(user);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("testuser");
        verify(userRepository).save(user);
    }
    
    @Test
    @DisplayName("사용자 생성 - 중복 사용자명")
    void createUser_DuplicateUsername() {
        // Given
        User existingUser = new User("testuser", "existing@example.com", "Existing User");
        User newUser = new User("testuser", "test@example.com", "Test User");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(existingUser));
        
        // When & Then
        assertThatThrownBy(() -> userService.save(newUser))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Username already exists: testuser");
    }
}
```

#### 통합 테스트
```java
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
class UserControllerIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private UserRepository userRepository;
    
    @Test
    @DisplayName("사용자 CRUD API 통합 테스트")
    void userCrudIntegrationTest() {
        // 1. 사용자 생성
        User newUser = new User("integrationtest", "integration@example.com", "Integration Test");
        ResponseEntity<ApiResponse> createResponse = restTemplate.postForEntity(
                "/api/users", newUser, ApiResponse.class);
        
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        
        // 2. 사용자 조회
        ResponseEntity<ApiResponse> getResponse = restTemplate.getForEntity(
                "/api/users/1", ApiResponse.class);
        
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        // 3. 사용자 목록 조회
        ResponseEntity<ApiResponse> getAllResponse = restTemplate.getForEntity(
                "/api/users", ApiResponse.class);
        
        assertThat(getAllResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        // 4. 사용자 수정
        newUser.setName("Updated Name");
        restTemplate.put("/api/users/1", newUser);
        
        User updatedUser = userRepository.findById(1L).orElse(null);
        assertThat(updatedUser).isNotNull();
        assertThat(updatedUser.getName()).isEqualTo("Updated Name");
    }
}
```

---

## 9. 마이그레이션 가이드

### 9.1 기존 Spring Boot 프로젝트에서 마이그레이션

#### 1단계: 의존성 추가
```xml
<!-- 기존 의존성들을 SK Framework Starter로 교체 -->
<dependency>
    <groupId>com.sk</groupId>
    <artifactId>sk-framework-starter-all</artifactId>
    <version>1.0.0</version>
</dependency>
```

#### 2단계: 기존 엔티티 수정
```java
// Before
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    // ...
}

// After  
@Entity
@Table(name = "users")
@AutoCrud(path = "/api/users")  // 자동 CRUD API 생성
public class User extends BaseEntity {  // BaseEntity 상속
    // id, createdAt, updatedAt 등은 BaseEntity에서 제공
    
    @NotBlank
    @Column(name = "username", unique = true)
    private String username;
    // ...
}
```

#### 3단계: Repository 수정
```java
// Before
public interface UserRepository extends JpaRepository<User, Long> {
    List<User> findByUsernameContaining(String username);
}

// After
public interface UserRepository extends BaseRepository<User, Long> {
    // JpaRepository + JpaSpecificationExecutor + QuerydslPredicateExecutor 모두 포함
    List<User> findByUsernameContaining(String username);
}
```

#### 4단계: Service 수정
```java
// Before
@Service
@Transactional(readOnly = true)
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    public List<User> findAll() {
        return userRepository.findAll();
    }
    
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
    // ...
}

// After
@Service
@Transactional(readOnly = true)
public class UserService extends BaseService<User, Long> {
    
    private final UserRepository userRepository;
    
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    @Override
    protected JpaRepository<User, Long> getRepository() {
        return userRepository;
    }
    
    @Override
    protected String getEntityName() {
        return "User";
    }
    
    // 기본 CRUD 메서드들은 BaseService에서 제공
    // 추가 비즈니스 로직만 구현
}
```

#### 5단계: Controller 수정
```java
// Before
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @GetMapping
    public ResponseEntity<List<User>> findAll() {
        return ResponseEntity.ok(userService.findAll());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<User> findById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }
    // ...
}

// After
@RestController
@RequestMapping("/api/users")
public class UserController extends BaseController<User, Long> {
    
    private final UserService userService;
    
    public UserController(UserService userService) {
        this.userService = userService;
    }
    
    @Override
    protected BaseService<User, Long> getService() {
        return userService;
    }
    
    // 기본 CRUD API들은 BaseController에서 제공
    // 추가 API만 구현
}
```

---

## 10. 결론 및 다음 단계

### 10.1 개발 완료 내역

✅ **완료된 기능들:**
- **Common 모듈**: BaseEntity, BaseController, BaseService, 유틸리티 클래스들
- **Security 모듈**: JWT 인증, Role 기반 권한 관리, Security Filter Chain
- **Data 모듈**: BaseRepository, 동적 쿼리 지원, QueryDSL 통합
- **Web 모듈**: 전역 예외 처리, 표준 API 응답 형식
- **Cache 모듈**: Redis 기반 캐싱, 어노테이션 기반 캐시 관리
- **Logging 모듈**: 구조화된 로깅 지원
- **Starter 모듈들**: 모든 기능의 Spring Boot Starter 제공
- **샘플 프로젝트**: 실제 사용 예제 프로젝트

### 10.2 아키텍처의 장점

🎯 **SOLID 원칙 준수:**
- 각 모듈과 클래스는 명확한 단일 책임을 가짐
- 인터페이스 기반 설계로 확장성과 테스트 용이성 확보
- 의존성 주입을 통한 느슨한 결합

🚀 **개발 생산성 향상:**
- `@AutoCrud` 어노테이션으로 자동 CRUD API 생성
- BaseController, BaseService 상속으로 반복 코드 제거
- 표준화된 API 응답 형식으로 일관성 보장

⚡ **성능 최적화:**
- `@AutoCache` 어노테이션으로 쉬운 캐시 적용
- Connection Pool 최적화 설정
- QueryDSL과 JPA Specification을 통한 효율적인 쿼리

🔒 **보안 강화:**
- JWT 기반 Stateless 인증
- `@RequireRole` 어노테이션으로 메서드 레벨 권한 제어
- CORS 설정 및 보안 필터 체인

### 10.3 사용 가이드 요약

#### 빠른 시작
```java
// 1. 의존성 추가
<dependency>
    <groupId>com.sk</groupId>
    <artifactId>sk-framework-starter-all</artifactId>
    <version>1.0.0</version>
</dependency>

// 2. 메인 클래스에 @EnableSkFramework 추가
@SpringBootApplication
@EnableSkFramework
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}

// 3. 엔티티에 @AutoCrud 추가로 자동 API 생성
@Entity
@AutoCrud(path = "/api/users")
public class User extends BaseEntity {
    // 필드 정의...
}
```

### 10.4 다음 단계 권장사항

🔄 **지속적인 개선:**
1. **성능 모니터링**: APM 도구 연동으로 성능 지표 수집
2. **보안 강화**: OAuth2, SAML 등 추가 인증 방식 지원
3. **테스트 자동화**: CI/CD 파이프라인에 자동 테스트 통합
4. **문서화**: 개발자를 위한 상세 API 문서 제공

📈 **확장 기능 개발:**
1. **알림 모듈**: 이메일, SMS, Push 알림 기능
2. **파일 관리 모듈**: 파일 업로드/다운로드, 이미지 처리
3. **배치 처리 모듈**: Spring Batch 기반 대용량 데이터 처리
4. **API Gateway**: 마이크로서비스 아키텍처 지원

🎓 **팀 교육 및 표준화:**
1. **개발 가이드 배포**: 팀 내 프레임워크 사용법 교육
2. **코드 리뷰 체크리스트**: 프레임워크 사용 가이드라인
3. **예제 프로젝트 확장**: 다양한 사용 케이스 예제 추가

---

SK Enterprise Framework는 **SOLID 원칙과 품질, 유지보수성, 성능을 모두 고려한 Enterprise급 프레임워크**로써, 개발팀의 생산성 향상과 일관된 개발 표준을 제공합니다. 

이 가이드를 참고하여 프레임워크를 효과적으로 활용하고, 필요에 따라 확장 개발을 진행하시기 바랍니다.

**📞 문의 및 지원**
- 이슈 등록: [GitHub Issues](https://github.com/sk-comm/sk-enterprise-framework/issues)  
- 기술 지원: framework-team@sk.com
- 개발 가이드: [Framework Wiki](https://wiki.sk.com/framework)
