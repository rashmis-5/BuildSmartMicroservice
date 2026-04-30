# Finance Service - Migration & Deployment Guide

## 📋 Overview

This guide helps teams migrate from the old Finance Service implementation to the new secured implementation with Feign client integration and role-based access control.

---

## 🔄 What Changed?

### Before Implementation
- ❌ No bearer token validation
- ❌ No role-based access control
- ❌ No proper error handling
- ❌ ProjectServiceClient was not properly configured
- ❌ No circuit breaker or retry logic
- ❌ Missing inter-service communication

### After Implementation
- ✅ Bearer token validation via IAM service
- ✅ Role-based access control (ADMIN & FINANCE_OFFICER only)
- ✅ Comprehensive error handling
- ✅ Fully configured ProjectServiceClient
- ✅ Circuit breaker prevents cascading failures
- ✅ Retry logic handles transient failures
- ✅ Secure inter-service communication
- ✅ Complete documentation and testing resources

---

## 📦 Files Modified

### 1. ProjectServiceClient.java
```java
// OLD
@FeignClient(name = "project-service")
public interface ProjectServiceClient {
    @GetMapping("/projects/{projectId}")
    ProjectDTO getProject(@PathVariable String projectId);
}

// NEW
@FeignClient(
    name = "project-service",
    url = "${project.service.url:http://localhost:8083}",
    configuration = FeignClientConfig.class
)
public interface ProjectServiceClient {
    @GetMapping("/projects/{projectId}")
    @CircuitBreaker(name = "projectService", fallbackMethod = "getProjectFallback")
    @Retry(name = "projectService")
    ProjectDTO getProject(
        @PathVariable String projectId,
        @RequestHeader("Authorization") String authorization);

    default ProjectDTO getProjectFallback(String projectId, String authorization, Throwable ex) {
        throw new BusinessRuleException("PROJECT-SERVICE-UNAVAILABLE", "...");
    }
}
```

### 2. ProjectDTO.java
```java
// OLD
private String projectId;
private String projectName;
private BigDecimal budget;
private LocalDate startDate;
private LocalDate endDate;
private String status;

// NEW (Added)
private String description;
private String templateId;
private String templateName;
private String createdBy;
private LocalDateTime createdAt;
private LocalDateTime updatedAt;
private Integer totalMilestones;
private Integer completedMilestones;
private Integer totalTasks;
private Integer completedTasks;
```

### 3. JwtAuthenticationFilter.java
```java
// OLD
if (!"ADMIN".equals(role) && !"PROJECT_MANAGER".equals(role)) {
    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
}

// NEW
private static final String ADMIN_ROLE = "ADMIN";
private static final String FINANCE_OFFICER_ROLE = "FINANCE_OFFICER";
// ...
if (!ADMIN_ROLE.equals(role) && !FINANCE_OFFICER_ROLE.equals(role)) {
    response.sendError(HttpServletResponse.SC_FORBIDDEN, 
        "Access denied. Only ADMIN and FINANCE_OFFICER can access this service. Your role: " + role);
}
```

### 4. SecurityConfig.java
```java
// OLD
.anyRequest().hasAnyRole("ADMIN", "PROJECT_MANAGER")

// NEW
.anyRequest().hasAnyRole("ADMIN", "FINANCE_OFFICER")
```

### 5. application.properties
```properties
# NEW additions
iam.service.url=http://localhost:8082
project.service.url=http://localhost:8083
jwt.secret=Y291cmlzcmluc2t5YW1pbHlzdWNjZXNzaW52b2tlZW50ZXJwcmlzZWNvbXBhbnlwb3RlbnRpYWxncm93dGhwcm9mZXNzaW9uYWw=
jwt.expiration=86400000

# Resilience4j configurations
resilience4j.circuitbreaker.instances.projectService.slidingWindowSize=10
resilience4j.circuitbreaker.instances.projectService.failureRateThreshold=50
resilience4j.circuitbreaker.instances.projectService.waitDurationInOpenState=5000

resilience4j.retry.instances.projectService.maxAttempts=3
resilience4j.retry.instances.projectService.waitDuration=1000
```

---

## 📁 Files Created

1. **ProjectFinanceService.java** (New Service Layer)
   - Path: `src/main/java/com/buildsmart/finance/service/ProjectFinanceService.java`
   - Handles project-related finance operations
   - Provides methods: getProjectById(), hasAccessToProjectFinance(), etc.

2. **ProjectFinanceController.java** (New REST Endpoints)
   - Path: `src/main/java/com/buildsmart/finance/controller/ProjectFinanceController.java`
   - Endpoints: GET /api/projects/{projectId}, GET /api/projects/validate/access
   - Method-level security with @PreAuthorize

---

## 🚀 Deployment Steps

### Step 1: Code Changes
1. Update all files listed in "Files Modified" section
2. Create new files listed in "Files Created" section
3. Update `application.properties`
4. Rebuild project: `mvn clean install`

### Step 2: Verify Build
```bash
# Build should succeed without errors
mvn clean compile

# Run tests if available
mvn test
```

### Step 3: Configuration
Before deployments, ensure:
```properties
# In application.properties or environment variables
iam.service.url=http://your-iam-service:8082
project.service.url=http://your-project-manager:8083
jwt.secret=your-secret-key
jwt.expiration=86400000
```

### Step 4: Dependencies Check
Ensure pom.xml has these dependencies:
```xml
<!-- Feign (should already exist) -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>

<!-- Resilience4j (should already exist) -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-circuitbreaker-resilience4j</artifactId>
</dependency>

<!-- JWT (should already exist) -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.3</version>
</dependency>
```

### Step 5: Database Migration (if needed)
No database changes required for this implementation.

### Step 6: Test Deployment
1. Start Finance Service
2. Test endpoints using Postman collection
3. Verify all services are communicating
4. Monitor logs for errors

---

## ✅ Pre-Deployment Checklist

### Code Changes
- [ ] ProjectServiceClient.java updated
- [ ] ProjectDTO.java extended
- [ ] JwtAuthenticationFilter.java updated
- [ ] SecurityConfig.java updated with new roles
- [ ] FeignClientConfig.java enhanced
- [ ] ProjectFinanceService.java created
- [ ] ProjectFinanceController.java created
- [ ] application.properties updated

### Configuration
- [ ] IAM Service URL configured (8082)
- [ ] Project Manager Service URL configured (8083)
- [ ] JWT secret configured
- [ ] JWT expiration configured
- [ ] Resilience4j settings configured

### Testing
- [ ] Unit tests pass
- [ ] Integration tests pass
- [ ] Postman collection tests pass
- [ ] All services can communicate

### Documentation
- [ ] Team briefed on changes
- [ ] Documentation shared
- [ ] API documentation updated
- [ ] Deployment plan shared

---

## 🔍 Testing After Deployment

### 1. Basic Connectivity Test
```bash
# Test Finance Service health
curl http://localhost:8080/actuator/health

# Test IAM Service health
curl http://localhost:8082/actuator/health

# Test Project Manager Service health
curl http://localhost:8083/actuator/health
```

### 2. Authentication Test
```bash
# Get token from IAM
TOKEN=$(curl -s -X POST "http://localhost:8082/users/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@buildsmart.com","password":"password"}' | jq -r '.data.token')

# Test Finance Service endpoint
curl -X GET "http://localhost:8080/api/projects/PROJ-001" \
  -H "Authorization: Bearer $TOKEN"
# Expected: 200 OK or 404 Not Found (but NOT 403 or 401)
```

### 3. Role Validation Test
```bash
# Test with FINANCE_OFFICER token (should work)
FINANCE_TOKEN=$(curl -s -X POST "http://localhost:8082/users/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"finance@buildsmart.com","password":"password"}' | jq -r '.data.token')

curl -X GET "http://localhost:8080/api/projects/PROJ-001" \
  -H "Authorization: Bearer $FINANCE_TOKEN"
# Expected: 200 OK

# Test with PROJECT_MANAGER token (should fail)
PM_TOKEN=$(curl -s -X POST "http://localhost:8082/users/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"pm@buildsmart.com","password":"password"}' | jq -r '.data.token')

curl -X GET "http://localhost:8080/api/projects/PROJ-001" \
  -H "Authorization: Bearer $PM_TOKEN"
# Expected: 403 Forbidden
```

### 4. Circuit Breaker Test
```bash
# Stop Project Manager Service
# Keep calling API
for i in {1..20}; do
  curl -X GET "http://localhost:8080/api/projects/PROJ-001" \
    -H "Authorization: Bearer $TOKEN"
  sleep 1
done

# Should see:
# 1. Initial failures with "Connection refused"
# 2. Then 503 Service Unavailable (circuit breaker open)
# 3. After 5 seconds, might try again and fail again
# 4. Restart Project Manager Service
# 5. Circuit recovers and calls succeed again
```

---

## 📊 Rollback Plan

If deployment fails:

### Option 1: Quick Rollback
```bash
# Revert to previous version
git revert HEAD

# Rebuild
mvn clean install

# Redeploy
docker build -t finance-service:old .
docker-compose down
docker-compose up
```

### Option 2: Database Rollback
No database changes needed (this deployment doesn't modify DB schema)

### Option 3: Data Rollback
No data changes needed

---

## 🔧 Configuration Migration

### Old Configuration
```properties
spring.application.name=finance-service
# Other basic configs
```

### New Configuration Required
```properties
spring.application.name=finance-service

# NEW: Service URLs
iam.service.url=http://localhost:8082
project.service.url=http://localhost:8083

# NEW: JWT settings
jwt.secret=Y291cmlzcmluc2t5YW1pbHlzdWNjZXNzaW52b2tlZW50ZXJwcmlzZWNvbXBhbnlwb3RlbnRpYWxncm93dGhwcm9mZXNzaW9uYWw=
jwt.expiration=86400000

# NEW: Resilience4j settings
resilience4j.circuitbreaker.instances.projectService.slidingWindowSize=10
resilience4j.circuitbreaker.instances.projectService.minimumNumberOfCalls=5
resilience4j.circuitbreaker.instances.projectService.failureRateThreshold=50
resilience4j.circuitbreaker.instances.projectService.waitDurationInOpenState=5000

resilience4j.retry.instances.projectService.maxAttempts=3
resilience4j.retry.instances.projectService.waitDuration=1000
resilience4j.retry.instances.projectService.retryExceptions=feign.FeignException.ServiceUnavailable,feign.FeignException.GatewayTimeout
```

---

## 🎯 Breaking Changes

### User Impact
1. **Existing Clients**: Need to use valid bearer tokens
2. **New Role Requirement**: Users need ADMIN or FINANCE_OFFICER role
3. **Error Responses**: Format may have changed (see documentation)

### API Changes
1. **Authentication**: Now required for all endpoints (except public ones)
2. **Authorization**: Stricter role checking
3. **Error Messages**: More descriptive

### Backward Compatibility
- ❌ NOT backward compatible
- ⚠️ All clients must send valid bearer token
- ⚠️ Only ADMIN and FINANCE_OFFICER can access
- ⚠️ PROJECT_MANAGER role will be denied

---

## 📞 Support & Troubleshooting

### Common Issues During Deployment

**Issue**: Connection refused to IAM Service
- **Solution**: Ensure IAM service is running on configured port
- **Check**: `curl http://localhost:8082/actuator/health`

**Issue**: 401 Unauthorized for valid users
- **Solution**: Check JWT secret matches between services
- **Check**: Compare jwt.secret values

**Issue**: 403 Forbidden for all users
- **Solution**: Verify user roles in IAM service
- **Check**: User role must be exactly "ADMIN" or "FINANCE_OFFICER" (case-sensitive)

**Issue**: Circuit breaker keeps timing out
- **Solution**: Check Project Manager service connectivity
- **Check**: `curl http://localhost:8083/actuator/health`

---

## 📈 Performance Impact

- **Expected Response Time**: +50-100ms (due to inter-service call)
- **Circuit Breaker Overhead**: Minimal (only on failures)
- **Retry Logic Impact**: Only on transient failures
- **JWT Validation**: +10-20ms per request

---

## 🔐 Security Considerations

### Before Deployment
- [ ] Rotate JWT secret in production
- [ ] Verify HTTPS is enabled
- [ ] Configure firewall rules
- [ ] Set up monitoring/alerting
- [ ] Ensure secrets are not in logs
- [ ] Test with real credentials

### After Deployment
- [ ] Monitor failed authentication attempts
- [ ] Watch for unusual error patterns
- [ ] Track circuit breaker state changes
- [ ] Monitor response times
- [ ] Review security logs regularly

---

## 📝 Deployment Checklist

### Pre-Deployment
- [ ] Code reviewed and approved
- [ ] All tests passing
- [ ] Documentation updated
- [ ] Configuration prepared
- [ ] Team trained on changes
- [ ] Rollback plan documented

### Deployment
- [ ] Backup current version
- [ ] Deploy new code
- [ ] Update configuration
- [ ] Verify services start
- [ ] Test basic functionality
- [ ] Monitor logs for errors

### Post-Deployment
- [ ] Run full test suite
- [ ] Verify all endpoints working
- [ ] Check service communications
- [ ] Monitor for 24 hours
- [ ] Verify performance metrics
- [ ] Update runbooks

---

## 📚 Documentation Links

- **Main Documentation**: README_IMPLEMENTATION.md
- **Technical Reference**: FEIGN_CLIENT_INTEGRATION.md
- **Quick Start**: QUICK_START_GUIDE.md
- **Architecture**: ARCHITECTURE_GUIDE.md
- **Testing**: Finance_Service_API.postman_collection.json

---

**Deployment Date**: TBD  
**Rollback Plan**: Available  
**Support Team**: Finance Platform Team  
**Escalation**: Development Manager

