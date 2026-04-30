# Finance Service - Architecture & Component Overview

## 🏗️ System Architecture

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                            CLIENT APPLICATION                                   │
│                                                                                  │
│  1. Login → IAM Service → Get Bearer Token                                      │
│  2. Call Finance Service with Bearer Token                                      │
└──────────────────────────┬──────────────────────────────────────────────────────┘
                           │
                           │ Bearer Token
                           ▼
┌──────────────────────────────────────────────────────────────────────────────────┐
│                        FINANCE SERVICE (Port 8080)                               │
│                                                                                  │
│  ┌────────────────────────────────────────────────────────────────────────┐   │
│  │ HTTP Request Handler Layer                                            │   │
│  │                                                                       │   │
│  │ ProjectFinanceController                                            │   │
│  │  └─ GET /api/projects/{projectId}                                 │   │
│  │  └─ GET /api/projects/validate/access                            │   │
│  │  └─ BudgetController (existing)                                  │   │
│  │  └─ ExpenseController (existing)                                 │   │
│  │  └─ PaymentController (existing)                                 │   │
│  └────────────────────────────────────────────────────────────────────────┘   │
│                           │                                                 │   │
│                           ▼                                                 │   │
│  ┌────────────────────────────────���───────────────────────────────────────┐   │
│  │ SECURITY FILTER CHAIN                                               │   │
│  │                                                                     │   │
│  │  JwtAuthenticationFilter                                          │   │
│  │  ├─ Extract Bearer Token from Authorization Header              │   │
│  │  ├─ Call IAM Service to validate token                         │   │
│  │  ├─ Get user role from IAM Service                            │   │
│  │  ├─ Check if role is ADMIN or FINANCE_OFFICER               │   │
│  │  │   ├─ ✅ YES → Continue to handler                        │   │
│  │  │   └─ ❌ NO → Return 403 Forbidden                        │   │
│  │  ├─ Store authentication in SecurityContext                 │   │
│  │  └─ Make token available for Feign calls                   │   │
│  └────────────────────────────────────────────────────────────────────────┘   │
│                           │                                                 │   │
│                           ▼                                                 │   │
│  ┌────────────────────────────────────────────────────────────────────────┐   │
│  │ SERVICE LAYER                                                       │   │
│  │                                                                     │   │
│  │  ProjectFinanceService                                           │   │
│  │  ├─ getProjectById(projectId)                                  │   │
│  │  ├─ hasAccessToProjectFinance()                               │   ��
│  │  ├─ getAuthorizationHeader()                                  │   │
│  │  └─ getCurrentUser()                                          │   │
│  │                                                               │   │
│  │  BudgetService (existing)                                   │   │
│  │  ExpenseService (existing)                                  │   │
│  │  PaymentService (existing)                                  │   │
│  └────────���───────────────────────────────────────────────────────────────┘   │
│                           │                                                 │   │
│                           ▼                                                 ���   │
│  ┌────────────────────────────────────────────────────────────────────────┐   │
│  │ FEIGN CLIENT + RESILIENCE LAYER                                      │   │
│  │                                                                     │   │
│  │  ProjectServiceClient                                            │   │
│  │  ├─ @CircuitBreaker(projectService)                           │   │
│  │  ├─ @Retry(projectService)                                    │   │
│  │  ├─ URL: http://localhost:8083/projects/{projectId}         │   │
│  │  ├─ RequestHeader: Authorization: Bearer <token>            │   │
│  │  └─ Fallback: Throw BusinessRuleException                  │   │
│  │                                                             │   │
│  │  FeignClientConfig                                          │   │
│  │  └─ RequestInterceptor (adds Bearer token to requests)     │   │
│  │                                                             │   │
│  │  Resilience4j Configuration                                │   │
│  │  ├─ Circuit Breaker:                                       │   │
│  │  │  ├─ Sliding window: 10 calls                          │   │
│  │  │  ├─ Failure threshold: 50%                            │   │
│  │  │  ├─ Half-open calls: 3                               │   │
│  │  │  └─ Wait time (open): 5 seconds                      │   │
│  │  └─ Retry:                                               │   │
│  │     ├─ Max attempts: 3                                  │   │
│  │     └─ Wait duration: 1 second                          │   │
│  └────────────────────────────────────────────────────────────────────────┘   │
└──────────────────────────┬──────────────────────────────────────────────────────┘
                           │
                 ┌─────────┴─────────┐
                 │                   │
                 ▼                   ▼
    ┌────────────────────┐  ┌────────────────────┐
    │  IAM SERVICE       │  │ PROJECT MANAGER    │
    │ (Port 8082)        │  │ SERVICE (Port 8083)│
    │                    │  │                    │
    │ GET /users/profile │  │ GET /projects/{id} │
    │ (validates token)  │  │ (returns project)  │
    │ Returns: role,     │  │ Returns:           │
    │         user, etc  │  │ ├─ projectId       │
    │                    │  │ ├─ projectName     │
    │                    │  │ ├─ budget          │
    │                    │  │ ├─ startDate       │
    │                    │  │ ├─ endDate         │
    │                    │  │ ├─ status          │
    │                    │  │ └─ etc             │
    └────────────────────┘  └────────────────────┘
```

---

## 📊 Request Flow Diagram

```
START
  │
  ├─ Receive HTTP Request with Bearer Token
  │
  ├─ Extract Authorization Header
  │  └─ Check if starts with "Bearer "
  │     ├─ YES → Extract token
  │     └─ NO → Return 401 Unauthorized
  │
  ├─ Call JwtAuthenticationFilter
  │  │
  │  ├─ Call IamServiceClient.getCurrentUserProfile(token)
  │  │
  │  ├─ Receive IAM Response with User Role
  │  │
  │  ├─ Check Role
  │  │  │
  │  │  ├─ Role = "ADMIN" OR "FINANCE_OFFICER"
  │  │  │  │
  │  │  │  ├─ Create UsernamePasswordAuthenticationToken
  │  │  │  ├─ Store in SecurityContext
  │  │  │  └─ Continue to Handler ►─────────────────┐
  │  │  │                                            │
  │  │  └─ Role = OTHER (PROJECT_MANAGER, USER, etc)
  │  │     │                                        │
  │  │     └─ Return 403 Forbidden ────────────────┤
  │  │        "Access denied. Only ADMIN and       │
  │  │         FINANCE_OFFICER can access..."      │
  │  │                                             │
  │  └─ Exception Handling                         │
  │     ├─ FeignException.Unauthorized             │
  │     │  └─ Return 401 Unauthorized             │
  │     └─ Other Exception                        │
  │        └─ Return 401 Unauthorized             │
  │                                                │
  └──────────────────────────────────────────────┨
                                                 │
                                                 ▼
                                   REQUEST REACHES HANDLER
                                   (e.g., ProjectFinanceController)
                                                 │
                                                 ├─ Get projectId from path
                                                 │
                                                 ├─ Call ProjectFinanceService
                                                 │  .getProjectById(projectId)
                                                 │
                                                 ├─ Extract Authorization from SecurityContext
                                                 │
                                                 ├─ Call ProjectServiceClient
                                                 │  .getProject(projectId, authHeader)
                                                 │
                                                 ├─ Circuit Breaker Check
                                                 │  ├─ State = CLOSED
                                                 │  │  └─ Call Project Manager Service
                                                 │  ├─ State = OPEN
                                                 │  │  └─ Call Fallback Method
                                                 │  │     └─ Throw BusinessRuleException
                                                 │  └─ State = HALF_OPEN
                                                 │     └─ Call Project Manager Service
                                                 │
                                                 ├─ Retry Logic
                                                 │  ├─ Attempt 1
                                                 │  │  ├─ SUCCESS → Return Response
                                                 │  │  └─ FAILURE → Try Again
                                                 │  ├─ Attempt 2
                                                 │  │  ├─ SUCCESS → Return Response
                                                 │  │  └─ Wait 1s, Try Again
                                                 │  └─ Attempt 3
                                                 │     ├─ SUCCESS → Return Response
                                                 │     └─ FAILURE → Execute Fallback
                                                 │
                                                 ├─ FeignClientConfig interceptor
                                                 │  └─ Automatically add Authorization header
                                                 │     to project manager service call
                                                 │
                                                 ├─ Call Project Manager Service
                                                 │  GET /projects/{projectId}
                                                 │  Header: Authorization: Bearer {token}
                                                 │
                                                 ├─ Receive ProjectDTO
                                                 │
                                                 ├─ Return to Controller
                                                 │
                                                 ├─ Serialize to JSON
                                                 │
                                                 └─ Return 200 OK with ProjectDTO
                                                    {
                                                      "projectId": "...",
                                                      "projectName": "...",
                                                      "budget": 100000,
                                                      "startDate": "2026-01-01",
                                                      "endDate": "2026-12-31",
                                                      "status": "ACTIVE"
                                                    }
END
```

---

## 🔄 Component Interaction Map

```
┌──────────────────────────────────────────────────────────────────────┐
│                      Finance Service Components                      │
├──────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │ HTTP Layer                                               │   │
│  │ ┌────────────────────────────────────────────────────┐  │   │
│  │ │ ProjectFinanceController                         │  │   │
│  │ │ └─ Endpoints                                    │  │   │
│  │ │    ├─ GET /api/projects/{id}                  │  │   │
│  │ │    └─ GET /api/projects/validate/access      │  │   │
│  │ └────────────────────────────────────────────────────┘  │   │
│  │                     │                                   │   │
│  │    Calling          ▼                                   │   │
│  │    Methods     ┌─────────────────────────────────────┐ │   │
│  │               │ ProjectFinanceService               │ │   │
│  │               │ • getProjectById()                 │ │   │
│  │               │ • hasAccessToProjectFinance()      │ │   │
│  │               │ • getAuthorizationHeader()         │ │   │
│  │               │ • getCurrentUser()                 │ │   │
│  │               └──────────┬──────────────────────────┘ │   │
│  │                          │                            │   │
│  │                 Calling  │ Feign Client              │   │
│  │                 Methods  │                            │   │
│  │                          ▼                            │   │
│  │         ┌────────────────────────────────────────┐  │   │
│  │         │ ProjectServiceClient (Feign)          │  │   │
│  │         │ • getProject()                         │  │   │
│  │         │ • getProjectFallback()                │  │   │
│  │         └─────────┬──────────────────────────────┘  │   │
│  │                   │                                 │   │
│  │    Decorated by   │                                 │   │
│  │                   ├─ @CircuitBreaker               │   │
│  │                   ├─ @Retry                        │   │
│  │                   ├─ @RequestHeader                │   │
│  │                   └─ FeignClientConfig             │   │
│  │                                                    │   │
│  └─────────────────────��───────────────────────────────────┘   │
│                                                              │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ Security Layer                                          │   │
│  │ ┌────────────────────────────��───────────────────────┐ │   │
│  │ │ SecurityConfig                                   │ │   │
│  │ │ └─ securityFilterChain()                        │ │   │
│  │ │    ├─ CSRF disabled                            │ │   │
│  │ │    ├─ CORS enabled                             │ │   │
│  │ │    ├─ JWT Filter added                         │ │   │
│  │ │    ├─ Only ADMIN, FINANCE_OFFICER allowed     │ │   │
│  │ │    └─ Public endpoints: /swagger-ui, /actuator/health  │ │
│  │ └────────────────────────────────────────────────────┘ │   │
│  │                                                        │   │
│  │ ┌────────────────────────────────────────────────────┐ │   │
│  │ │ JwtAuthenticationFilter                          │ │   │
│  │ │ └─ doFilterInternal()                            │ │   │
│  │ │    ├─ Extract bearer token                      │ │   │
│  │ │    ├─ Validate via IamServiceClient             │ │   │
│  │ │    ├─ Check role (ADMIN/FINANCE_OFFICER)       │ │   │
│  │ │    ├─ Create authentication token               │ │   │
│  │ │    └─ Set in SecurityContext                    │ │   │
│  │ └────────────────────────────────────────────────────┘ │   │
│  │                                                        │   │
│  │ ┌────────────────────────────────────────────────────┐ │   │
│  │ │ FeignClientConfig                               │ │   │
│  │ │ └─ authRequestInterceptor()                     │ │   │
│  │ │    ├─ Extract token from SecurityContext        │ │   │
│  │ │    ├─ Add Bearer prefix                         │ │   │
│  │ │    └─ Add to Feign request headers              │ │   │
│  │ └────────────────────────────────────────────────────┘ │   │
│  │                                                        │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                              │
│  ┌──────────────────────────���──────────────────────────────┐   │
│  │ Resilience Layer (Resilience4j)                       │   │
│  │ ┌────────────────────────────────────────────────────┐ │   │
│  │ │ Circuit Breaker (projectService)               │ │   │
│  │ │ • Monitors call success/failure               │ │   │
│  │ │ • Prevents cascading failures                 │ │   │
│  │ │ • States: CLOSED → OPEN → HALF_OPEN          │ │   │
│  │ └────────────────────────────────────────────────────┘ │   │
│  │                                                        │   │
│  │ ┌────────────────────────────────────────────────────┐ │   │
│  │ │ Retry (projectService)                           │ │   │
│  │ │ • Retries failed calls                          │ │   │
│  │ │ • Max 3 attempts                                │ │   │
│  │ │ • 1 second wait between attempts               │ │   │
│  │ └────────────────────────────────────────────────────┘ │   │
│  │                                                        │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                              │
│  ┌──────────────────────────────────────────���──────────────┐   │
│  │ Client DTO Layer                                       │   │
│  │ ┌────────────────────────────────────────────────────┐ │   │
│  │ │ ProjectDTO                                       │ │   │
│  │ │ ├─ projectId                                    │ │   │
│  │ │ ├─ projectName                                 │ │   │
│  │ │ ├─ description                                 │ │   │
│  │ │ ├─ budget                                      │ │   │
│  │ │ ├─ startDate                                   │ │   │
│  │ │ ├─ endDate                                     │ │   │
│  │ │ ├─ status                                      │ │   │
│  │ │ └─ ... (and other fields)                      │ │   │
│  │ └────────────────────────────────────────────────────┘ │   │
│  │                                                        │   │
│  │ ┌────────────────────────────────────────────────────┐ │   │
│  │ │ IamProfileResponse                              │ │   │
│  │ │ └─ IamUserProfile                              │ │   │
│  │ │    ├─ userId                                  │ │   │
│  │ │    ├─ name                                    │ │   │
│  │ │    ├─ email                                   │ │   │
│  │ │    ├─ role (ADMIN, FINANCE_OFFICER, etc)     │ │   │
│  │ │    └─ status                                 │ │   │
│  │ └────────────────────────────────────────────────────┘ │   │
│  │                                                        │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                              │
└──────────────────────────────────────────────────────────────────────┘
        ▲
        │
        ├─ Uses config from
        │  application.properties
        │
        └─ Depends on external services:
           ├─ IAM Service (8082)
           └─ Project Manager Service (8083)
```

---

## 🔐 Security Decision Tree

```
Request Arrives
  │
  ├─ Has Authorization Header?
  │  ├─ NO → Return 401 Unauthorized
  │  │
  │  └─ YES
  │     │
  │     ├─ Header format is "Bearer <token>"?
  │     │  ├─ NO → Return 401 Unauthorized
  │     │  │
  │     │  └─ YES
  │     │     │
  │     │     ├─ Call IAM Service to validate
  │     │     │  ├─ Service unavailable?
  │     │     │  │  └─ Return 401 Unauthorized
  │     │     │  │
  │     │     │  └─ Response received
  │     │     │     │
  │     │     │     ├─ Token expired?
  │     │     │     │  └─ Return 401 Unauthorized
  │     │     │     │
  │     │     │     ├─ Token invalid?
  │     │     │     │  └─ Return 401 Unauthorized
  │     │     │     │
  │     │     │     └─ Token valid
  │     │     │        │
  │     │     │        ├─ User profile contains role?
  │     │     │        │  ├─ NO → Return 401 Unauthorized
  │     │     │        │  │
  │     │     │        │  └─ YES
  │     │     │        │     │
  │     │     │        │     ├─ Role is ADMIN or FINANCE_OFFICER?
  │     │     │        │     │  ├─ YES → Store in SecurityContext
  │     │     │        │     │  │        Continue to Handler
  │     │     │        │     │  │
  │     │     │        │     │  └─ NO → Return 403 Forbidden
  │     │     │        │     │         with role info
  │     │     │        │     │
  │     │     │        │     └─ End
  │     │     │        │
  │     │     │        └─ End
  │     │     │
  │     │     └─ End
  │     │
  │     └─ End
  │
  └─ End
```

---

## 📈 Data Flow Summary

### 1. Authentication Flow
```
User Input (email, password)
         ↓
IAM Service Login API
         ↓
JWT Token Generated
         ↓
Return Token to Client
         ↓
Client Stores Token
         ↓
Client includes in Authorization Header
```

### 2. Request Processing Flow
```
HTTP Request + Bearer Token
         ↓
JwtAuthenticationFilter
         ↓
Extract & Validate Token via IAM Service
         ↓
Check User Role
         ↓
SecurityContext Setup
         ↓
Handler/Controller Called
         ↓
ProjectFinanceService Called
         ↓
Feign Client Call (with auto Bearer Token)
         ↓
Project Manager Service Responds
         ↓
Response Serialized & Returned
```

### 3. Inter-Service Communication Flow
```
ProjectServiceClient.getProject(projectId, authHeader)
         ↓
FeignClientConfig RequestInterceptor
         ↓
Add Bearer Token to Headers
         ↓
HTTP GET Request to Project Manager Service
         │
         ├─ Circuit Breaker Check
         │  ├─ CLOSED: Call Service
         │  ├─ OPEN: Use Fallback
         │  └─ HALF_OPEN: Try Call
         │
         └─ Retry Logic
            ├─ Attempt 1
            ├─ Attempt 2 (after 1s wait)
            └─ Attempt 3 (after 1s wait)
         ↓
Receive ProjectDTO
         ↓
Return to Service Layer
```

---

## 🎯 Key Design Patterns

1. **Circuit Breaker Pattern**: Prevents cascading failures
2. **Retry Pattern**: Handles transient failures
3. **Bearer Token Pattern**: RESTful API authentication
4. **Filter Pattern**: Security filter chain
5. **Interceptor Pattern**: Feign request interception
6. **DTO Pattern**: Data transfer between services
7. **Service Layer Pattern**: Business logic separation
8. **Exception Translation**: BusinessRuleException wrapping

---

**Last Updated**: April 28, 2026

