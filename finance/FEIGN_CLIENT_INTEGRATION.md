# Finance Service - Feign Client Integration Documentation

## Overview
This document describes the implementation of Feign client communication with proper authentication and role-based access control (RBAC) for the Finance Service microservice.

## Key Changes

### 1. **ProjectServiceClient** (Updated)
- **Location**: `src/main/java/com/buildsmart/finance/client/ProjectServiceClient.java`
- **Changes**:
  - Added proper URL configuration: `${project.service.url:http://localhost:8083}`
  - Added `@RequestHeader("Authorization")` parameter to pass bearer tokens
  - Implemented circuit breaker pattern with `@CircuitBreaker` annotation
  - Implemented retry logic with `@Retry` annotation
  - Added fallback method to handle service unavailability
  - Proper error handling for PROJECT-SERVICE-UNAVAILABLE scenario

### 2. **ProjectDTO** (Updated)
- **Location**: `src/main/java/com/buildsmart/finance/client/dto/ProjectDTO.java`
- **Changes**:
  - Extended with additional fields to match ProjectResponse from Project Manager service
  - New fields: `description`, `templateId`, `templateName`, `createdBy`, `createdAt`, `updatedAt`
  - Support for milestone and task counts
  - BigDecimal and Double fields for budget flexibility

### 3. **FeignClientConfig** (Updated)
- **Location**: `src/main/java/com/buildsmart/finance/client/config/FeignClientConfig.java`
- **Changes**:
  - Enhanced token extraction from SecurityContext
  - Proper handling of Bearer token prefix
  - Ensures token is passed to all Feign client requests
  - Maintains token state throughout service-to-service communication

### 4. **SecurityConfig** (Updated)
- **Location**: `src/main/java/com/buildsmart/finance/config/SecurityConfig.java`
- **Changes**:
  - Fixed role-based access control to enforce `ADMIN` and `FINANCE_OFFICER` roles only
  - Changed from `PROJECT_MANAGER` to `FINANCE_OFFICER` role
  - Public endpoints: `/swagger-ui/**`, `/v3/api-docs/**`, `/actuator/health`
  - All other endpoints require authentication and proper role
  - Returns 401 Unauthorized for missing/invalid tokens
  - Returns 403 Forbidden for unauthorized roles

### 5. **JwtAuthenticationFilter** (Updated)
- **Location**: `src/main/java/com/buildsmart/finance/security/JwtAuthenticationFilter.java`
- **Changes**:
  - Validates bearer token from Authorization header
  - Calls IAM service to verify user profile and role
  - **Role Validation**:
    - Only `ADMIN` and `FINANCE_OFFICER` roles allowed
    - Other roles receive 403 Forbidden error
    - Clear error messages about role restrictions
  - Extracts user email or ID as principal
  - Stores token without prefix in credentials for Feign use
  - Comprehensive logging for debugging
  - Proper exception handling with specific HTTP status codes

### 6. **ProjectFinanceService** (New)
- **Location**: `src/main/java/com/buildsmart/finance/service/ProjectFinanceService.java`
- **Purpose**: Service layer for project-related finance operations
- **Key Methods**:
  - `getProjectById(projectId)`: Fetch project details from Project Manager service
  - `hasAccessToProjectFinance()`: Validate user has required roles
  - `getCurrentUser()`: Get current authenticated user
- **Features**:
  - Automatic authorization header extraction and passing
  - Comprehensive error handling with BusinessRuleException
  - Specific error messages for different failure scenarios
  - Circuit breaker integration for resilience
  - Full audit logging

### 7. **ProjectFinanceController** (New)
- **Location**: `src/main/java/com/buildsmart/finance/controller/ProjectFinanceController.java`
- **Endpoints**:
  - `GET /api/projects/{projectId}`: Get project details
  - `GET /api/projects/validate/access`: Validate user access
- **Features**:
  - `@PreAuthorize` annotations for method-level security
  - Only accessible by ADMIN and FINANCE_OFFICER roles
  - Comprehensive Swagger documentation
  - CORS enabled for cross-origin requests

### 8. **Application Properties** (Updated)
- **Location**: `src/main/resources/application.properties`
- **New Configurations**:
  ```properties
  iam.service.url=http://localhost:8082
  project.service.url=http://localhost:8083
  jwt.secret=...
  jwt.expiration=86400000
  
  # Resilience4j Circuit Breaker
  resilience4j.circuitbreaker.instances.projectService.*
  resilience4j.retry.instances.projectService.*
  ```

## Role-Based Access Control (RBAC)

### Allowed Roles
- **ADMIN**: Full access to all finance operations
- **FINANCE_OFFICER**: Full access to all finance operations

### Denied Roles
- Any other role (e.g., PROJECT_MANAGER, USER, GUEST) will receive:
  - HTTP 403 Forbidden response
  - Error message: "Access denied. Only ADMIN and FINANCE_OFFICER can access this service. Your role: [USER_ROLE]"

## Authentication Flow

```
1. User Login (via IAM Service)
   ↓
2. Receive Bearer Token
   ↓
3. Include Token in Authorization Header
   ↓
4. Finance Service receives request
   ↓
5. JwtAuthenticationFilter extracts token
   ↓
6. Calls IAM Service to validate user/role
   ↓
7. Validates role is ADMIN or FINANCE_OFFICER
   ↓
8. If valid → Process request with SecurityContext
   If invalid role → Return 403 Forbidden
   If expired token → Return 401 Unauthorized
   ↓
9. SecurityContext available for Feign calls
   ↓
10. Feign automatically includes token in inter-service calls
```

## Error Handling

### HTTP 401 Unauthorized
- Missing Authorization header
- Invalid or expired token
- IAM service unreachable
- User profile not found in IAM service

### HTTP 403 Forbidden
- User role is not ADMIN or FINANCE_OFFICER
- User profile exists but doesn't have required role

### HTTP 503 Service Unavailable
- Project Manager service is down
- Circuit breaker is in OPEN state

## Feign Client Features

### Circuit Breaker Settings
```properties
slidingWindowSize: 10 calls
minimumNumberOfCalls: 5
failureRateThreshold: 50%
slowCallDurationThreshold: 2 seconds
waitDurationInOpenState: 5 seconds
```

### Retry Settings
```properties
maxAttempts: 3
waitDuration: 1 second
Retries on: ServiceUnavailable, GatewayTimeout, ConnectException, IOException
```

## Usage Examples

### 1. Get Project Details (with Authentication)

**Request**:
```bash
curl -X GET "http://localhost:8080/api/projects/PROJ-001" \
  -H "Authorization: Bearer {your_token}" \
  -H "Content-Type: application/json"
```

**Success Response** (200 OK):
```json
{
  "projectId": "PROJ-001",
  "projectName": "Construction Project A",
  "description": "Main construction project",
  "templateId": "TPL-001",
  "budget": 100000.00,
  "startDate": "2026-01-01",
  "endDate": "2026-12-31",
  "status": "ACTIVE",
  "createdBy": "john@cognizant.com",
  "totalMilestones": 10,
  "completedMilestones": 3,
  "totalTasks": 50,
  "completedTasks": 15
}
```

**Unauthorized Response** (401):
```json
{
  "error": "Unauthorized",
  "message": "Invalid or expired token"
}
```

**Forbidden Response** (403):
```json
{
  "error": "Forbidden",
  "message": "Access denied. Only ADMIN and FINANCE_OFFICER can access this service. Your role: PROJECT_MANAGER"
}
```

### 2. Validate User Access

**Request**:
```bash
curl -X GET "http://localhost:8080/api/projects/validate/access" \
  -H "Authorization: Bearer {your_token}"
```

**Success Response** (200 OK):
```json
true
```

## Configuration Requirements

### Environment Variables / Properties
```properties
# IAM Service Configuration
iam.service.url=http://localhost:8082

# Project Manager Service Configuration
project.service.url=http://localhost:8083

# JWT Configuration
jwt.secret=Y291cmlzcmluc2t5YW1pbHlzdWNjZXNzaW52b2tlZW50ZXJwcmlzZWNvbXBhbnlwb3RlbnRpYWxncm93dGhwcm9mZXNzaW9uYWw=
jwt.expiration=86400000
```

### Required Services
1. **IAM Service** (Port 8082)
   - Endpoint: `GET /users/profile` (with Bearer token)
   - Returns user role information
   
2. **Project Manager Service** (Port 8083)
   - Endpoint: `GET /projects/{projectId}` (with Bearer token)
   - Returns project details

## Security Measures

1. **Token Validation**: Every request validates the JWT token via IAM service
2. **Role-Based Access Control**: Only ADMIN and FINANCE_OFFICER roles allowed
3. **Bearer Token Management**: Tokens are securely passed between services
4. **Stateless Session Management**: No server-side session state
5. **CORS Configuration**: Configurable cross-origin requests
6. **Circuit Breaker**: Prevents cascading failures
7. **Retry Logic**: Handles transient failures automatically
8. **Comprehensive Logging**: All security events are logged

## Debugging

### Enable Debug Logging

Add to `application.properties`:
```properties
logging.level.com.buildsmart.finance=DEBUG
logging.level.org.springframework.security=DEBUG
logging.level.feign=DEBUG
```

### Common Issues

**Issue**: 403 Forbidden for valid users
- **Solution**: Verify user role in IAM service is exactly `ADMIN` or `FINANCE_OFFICER` (case-sensitive)

**Issue**: 401 Unauthorized errors
- **Solution**: Check token validity, ensure IAM service is running, verify JWT secret matches

**Issue**: Circuit breaker is open
- **Solution**: Check Project Manager service health, wait for waitDurationInOpenState (5 seconds)

## Testing

### Test Scenarios

1. **Valid ADMIN User**: Should get 200 OK with project details
2. **Valid FINANCE_OFFICER User**: Should get 200 OK with project details
3. **Invalid Role (PROJECT_MANAGER)**: Should get 403 Forbidden
4. **Missing Token**: Should get 401 Unauthorized
5. **Expired Token**: Should get 401 Unauthorized
6. **Project Manager Down**: Should get 503 Service Unavailable

## Future Enhancements

1. Add more granular RBAC (e.g., department-based access)
2. Implement token caching to reduce IAM service calls
3. Add API rate limiting
4. Implement request/response encryption
5. Add audit logging to database
6. Implement API versioning

