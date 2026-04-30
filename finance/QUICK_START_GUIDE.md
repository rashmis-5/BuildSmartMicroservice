# Finance Service - Quick Start Guide

## 🚀 Getting Started

### Prerequisites
1. IAM Service running on `http://localhost:8082`
2. Project Manager Service running on `http://localhost:8083`
3. Finance Service with the updated code
4. Valid user credentials with ADMIN or FINANCE_OFFICER role

---

## 📌 Step-by-Step Guide

### Step 1: Authenticate User
Call IAM Service to get authorization token:

```bash
curl -X POST "http://localhost:8082/users/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "finance@buildsmart.com",
    "password": "password123"
  }'
```

**Response**:
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "userId": "USR-001",
    "email": "finance@buildsmart.com",
    "role": "FINANCE_OFFICER",
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }
}
```

**Save the token**: `TOKEN=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...`

---

### Step 2: Call Finance Service API

#### Get Project Details
```bash
curl -X GET "http://localhost:8080/api/projects/PROJ-001" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json"
```

**Success Response** (200 OK):
```json
{
  "projectId": "PROJ-001",
  "projectName": "Construction Project Alpha",
  "description": "Main construction phase",
  "templateId": "TPL-CV-001",
  "templateName": "Construction Template",
  "budget": 500000.00,
  "budgetAmount": 500000.0,
  "startDate": "2026-01-15",
  "endDate": "2026-12-31",
  "status": "ACTIVE",
  "createdBy": "admin@buildsmart.com",
  "createdAt": "2026-01-01T10:00:00",
  "updatedAt": "2026-04-28T15:30:00",
  "totalMilestones": 5,
  "completedMilestones": 2,
  "totalTasks": 25,
  "completedTasks": 8
}
```

---

### Step 3: Validate Access

Check if current user has permission to access project finance:

```bash
curl -X GET "http://localhost:8080/api/projects/validate/access" \
  -H "Authorization: Bearer $TOKEN"
```

**Response**:
```
true
```

---

## ⚠️ Common Error Scenarios

### Scenario 1: No Authorization Header

**Request**:
```bash
curl -X GET "http://localhost:8080/api/projects/PROJ-001"
```

**Response** (401 Unauthorized):
```json
{
  "timestamp": "2026-04-28T15:35:00.000+00:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Missing or invalid authorization header",
  "path": "/api/projects/PROJ-001"
}
```

---

### Scenario 2: Expired/Invalid Token

**Request**:
```bash
curl -X GET "http://localhost:8080/api/projects/PROJ-001" \
  -H "Authorization: Bearer invalid_expired_token"
```

**Response** (401 Unauthorized):
```json
{
  "timestamp": "2026-04-28T15:35:00.000+00:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid or expired token",
  "path": "/api/projects/PROJ-001"
}
```

---

### Scenario 3: Insufficient Permissions (Wrong Role)

**Request** (with PROJECT_MANAGER token):
```bash
curl -X GET "http://localhost:8080/api/projects/PROJ-001" \
  -H "Authorization: Bearer $PROJECT_MANAGER_TOKEN"
```

**Response** (403 Forbidden):
```json
{
  "timestamp": "2026-04-28T15:35:00.000+00:00",
  "status": 403,
  "error": "Forbidden",
  "message": "Access denied. Only ADMIN and FINANCE_OFFICER can access this service. Your role: PROJECT_MANAGER",
  "path": "/api/projects/PROJ-001"
}
```

---

### Scenario 4: Project Not Found

**Request**:
```bash
curl -X GET "http://localhost:8080/api/projects/INVALID-PROJ-999" \
  -H "Authorization: Bearer $TOKEN"
```

**Response** (400/404):
```json
{
  "timestamp": "2026-04-28T15:35:00.000+00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "PROJECT-NOT-FOUND: Project with ID INVALID-PROJ-999 not found",
  "path": "/api/projects/INVALID-PROJ-999"
}
```

---

### Scenario 5: Service Unavailable (Circuit Breaker Open)

**Response** (503):
```json
{
  "timestamp": "2026-04-28T15:35:00.000+00:00",
  "status": 503,
  "error": "Service Unavailable",
  "message": "PROJECT-SERVICE-UNAVAILABLE: Project service is currently unavailable. Cannot fetch project: PROJ-001",
  "path": "/api/projects/PROJ-001"
}
```

---

## 🔍 Debugging Tips

### 1. Check Service Health
```bash
curl http://localhost:8080/actuator/health
```

### 2. View Circuit Breaker Status
```bash
curl http://localhost:8080/actuator/circuitbreakers
```

### 3. Enable Debug Logging
Add to `application.properties`:
```properties
logging.level.com.buildsmart.finance=DEBUG
logging.level.org.springframework.security=DEBUG
logging.level.feign=DEBUG
```

### 4. Verify Token in JWT.io
1. Go to https://jwt.io
2. Paste your token (right side, "Encoded")
3. Check payload for user information and expiration

### 5. Check Service URLs
```bash
# Test IAM Service
curl http://localhost:8082/actuator/health

# Test Project Manager Service
curl http://localhost:8083/actuator/health
```

---

## 🛠️ Common Commands

### Get Token (ADMIN)
```bash
ADMIN_TOKEN=$(curl -s -X POST "http://localhost:8082/users/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@buildsmart.com","password":"admin123"}' \
  | jq -r '.data.token')
echo $ADMIN_TOKEN
```

### Get Project Details
```bash
curl -s -X GET "http://localhost:8080/api/projects/PROJ-001" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq .
```

### Get Multiple Projects (with pagination)
```bash
curl -s -X GET "http://localhost:8080/api/budgets/projects/PROJ-001?page=0&size=10" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq .
```

---

## 📋 Allowed Roles

| Role | Access to Finance Service | Use Case |
|------|---------------------------|----------|
| ADMIN | ✅ YES | Full administrative access |
| FINANCE_OFFICER | ✅ YES | Finance operations and budget management |
| PROJECT_MANAGER | ❌ NO | Cannot access finance operations |
| USER | ❌ NO | Cannot access finance operations |
| GUEST | ❌ NO | Cannot access any endpoints |

---

## 🔐 Security Important Points

1. **Always use HTTPS** in production
2. **Don't share tokens** - they expire in 24 hours
3. **Rotate secrets** regularly in production
4. **Log all access** for audit purposes
5. **Use strong passwords** for user accounts
6. **Monitor failed attempts** - potential security threat

---

## 📞 Support

If you encounter issues:

1. Check service logs:
   ```bash
   docker logs finance-service
   docker logs iam-service
   docker logs project-manager-service
   ```

2. Verify all services are running:
   ```bash
   curl http://localhost:8080/actuator/health
   curl http://localhost:8082/actuator/health
   curl http://localhost:8083/actuator/health
   ```

3. Check database connectivity
4. Verify network/firewall settings
5. Review application configuration

---

## 🎓 API Documentation

For complete API documentation, visit:
- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI Spec**: `http://localhost:8080/v3/api-docs`

---

## ✅ Checklist Before Going Live

- [ ] All services are running and healthy
- [ ] Users with ADMIN role can access API
- [ ] Users with FINANCE_OFFICER role can access API
- [ ] Users with other roles receive 403 error
- [ ] Missing token receives 401 error
- [ ] Expired token receives 401 error
- [ ] Circuit breaker is working
- [ ] Retry logic is functioning
- [ ] Logging is configured
- [ ] Monitoring is set up
- [ ] Database is configured
- [ ] Environment variables are set

---

**Last Updated**: April 28, 2026
**Status**: Ready for Testing

