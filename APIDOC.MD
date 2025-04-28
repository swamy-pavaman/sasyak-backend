# Sasyak API Documentation by Role

This document outlines the REST APIs for the Sasyak system organized by user role.

## Table of Contents
- [Authentication](#authentication)
- [Super Admin Operations](#super-admin-operations)
- [Admin Operations](#admin-operations)
- [Manager Operations](#manager-operations)
- [Supervisor Operations](#supervisor-operations)
- [Common Operations](#common-operations)
    - [Task Operations](#task-operations)
    - [Notification Operations](#notification-operations)

---

## Authentication

### Register User
- **URL**: `/api/auth/register`
- **Method**: `POST`
- **Description**: Register a new user
- **Access**: Public
- **Request Body**:
  ```json
  {
    "name": "string",
    "email": "string",
    "phone_number": "string",
    "role": "string", // "SUPER_ADMIN", "ADMIN", "MANAGER", "SUPERVISOR"
    "password": "string",
    "tenantId": "string", // UUID
    "managerId": "integer" // Optional
  }
  ```

## Error Handling

All error responses follow a consistent format:

```json
{
  "timestamp": "2024-05-01T12:00:00Z",
  "status": 400, // HTTP status code
  "error": "Bad Request", // Error type
  "message": "Detailed error message", // Specific error details
  "path": "/api/..." // The endpoint that was called
}
```

### Common HTTP Status Codes

- **200 OK** - Request succeeded
- **201 Created** - Resource successfully created
- **400 Bad Request** - Invalid input parameters
- **401 Unauthorized** - Authentication failed or token expired
- **403 Forbidden** - Authenticated but not authorized for the resource
- **404 Not Found** - Resource not found
- **409 Conflict** - Request conflicts with current state (e.g., duplicate email)
- **500 Internal Server Error** - Server-side error

## Authentication and Security

### JWT Authentication

The API uses JWT (JSON Web Tokens) for authentication:

1. After successful login or registration, the system returns an `accessToken` and a `refreshToken`
2. The `accessToken` must be included in the Authorization header for authenticated requests:
   ```
   Authorization: Bearer <accessToken>
   ```
3. When the `accessToken` expires, use the `/api/auth/refresh-token` endpoint with the `refreshToken` to get a new `accessToken`

### Role-Based Access Control

Access to endpoints is controlled by user roles:

- **SUPER_ADMIN** - Can manage tenants and perform system-wide operations
- **ADMIN** - Can manage all users and resources within their tenant
- **MANAGER** - Can manage their team members and create/assign tasks
- **SUPERVISOR** - Can update task implementations and manage their profile
- **EMPLOYEE** - Can view and implement assigned tasks

## Data Models

### User
```json
{
  "userId": "integer",
  "name": "string",
  "email": "string",
  "phone_number": "string",
  "role": "string", // "SUPER_ADMIN", "ADMIN", "MANAGER", "SUPERVISOR", "EMPLOYEE"
  "managerId": "integer", // Optional
  "tenantId": "string" // UUID
}
```

### Task
```json
{
  "id": "integer",
  "taskType": "string",
  "description": "string",
  "status": "string", // "PENDING", "IN_PROGRESS", "COMPLETED", etc.
  "createdBy": "string",
  "assignedTo": "string",
  "createdAt": "string", // ISO date
  "updatedAt": "string", // ISO date
  "detailsJson": "string", // JSON string with task details
  "imagesJson": "string", // JSON string with image references
  "implementationJson": "string" // JSON string with implementation details
}
```

### TaskAdvice
```json
{
  "id": "integer",
  "taskId": "integer",
  "managerName": "string",
  "adviceText": "string",
  "createdAt": "string" // ISO date
}
```

### Tenant
```json
{
  "tenantId": "string", // UUID
  "companyName": "string",
  "contactEmail": "string",
  "createdAt": "string", // ISO date
  "isActive": "boolean"
}
```

### Notification
```json
{
  "id": "integer",
  "title": "string",
  "message": "string",
  "taskId": "integer", // Optional
  "isRead": "boolean",
  "createdAt": "string" // ISO date
}
```

## Pagination

Many list endpoints support pagination with the following query parameters:

- `page`: Page number (zero-based, default: 0)
- `size`: Page size (default: 10)

Paginated responses typically include:

```json
{
  "items": [...], // Array of items (may be named differently, e.g., "employees", "tasks")
  "totalItems": "long", // Total number of items across all pages
  "totalPages": "integer", // Total number of pages
  "currentPage": "integer" // Current page number
}
```

## Rate Limiting

The API implements rate limiting to prevent abuse:

- Standard endpoints: 60 requests per minute
- Authentication endpoints: 10 requests per minute

Exceeding these limits will result in a `429 Too Many Requests` response.

## Versioning

The current API version is v1, which is implicit in the base URL. Future versions may be explicitly versioned as `/api/v2/...`
- **Response**:
    - Status Code: 201 Created
  ```json
  {
    "accessToken": "string",
    "refreshToken": "string",
    "email": "string",
    "name": "string",
    "userId": "integer"
  }
  ```

### Login User
- **URL**: `/api/auth/login`
- **Method**: `POST`
- **Description**: Authenticate a user
- **Access**: Public
- **Request Body**:
  ```json
  {
    "email": "string",
    "password": "string"
  }
  ```
- **Response**:
    - Status Code: 200 OK
  ```json
  {
    "accessToken": "string",
    "refreshToken": "string",
    "email": "string",
    "name": "string",
    "userId": "integer",
    "role": "string"
  }
  ```

### Refresh Token
- **URL**: `/api/auth/refresh-token`
- **Method**: `POST`
- **Description**: Refresh the access token using a refresh token
- **Access**: Public
- **Request Body**:
  ```json
  {
    "refreshToken": "string"
  }
  ```
- **Response**:
    - Status Code: 200 OK
  ```json
  {
    "accessToken": "string",
    "refreshToken": "string",
    "email": "string"
  }
  ```

---

## Super Admin Operations

### Login Super Admin
- **URL**: `/api/superadmin/login`
- **Method**: `POST`
- **Description**: Authenticate a super admin user
- **Access**: Public
- **Request Body**:
  ```json
  {
    "email": "string",
    "password": "string"
  }
  ```
- **Response**:
    - Status Code: 200 OK
  ```json
  {
    "accessToken": "string",
    "refreshToken": "string",
    "email": "string",
    "name": "string",
    "userId": "integer"
  }
  ```

### Create Tenant
- **URL**: `/api/tenants`
- **Method**: `POST`
- **Description**: Create a new tenant with admin user
- **Access**: Requires SUPER_ADMIN role
- **Request Body**:
  ```json
  {
    "companyName": "string",
    "contactEmail": "string",
    "adminName": "string",
    "adminEmail": "string",
    "role": "string" // Optional, defaults to "ADMIN"
  }
  ```
- **Response**:
    - Status Code: 201 Created
  ```json
  {
    "message": "string",
    "tenant": {
      "id": "string",
      "companyName": "string",
      "contactEmail": "string"
    },
    "adminUser": {
      "id": "integer",
      "name": "string",
      "email": "string"
    }
  }
  ```

### Get All Tenants
- **URL**: `/api/tenants`
- **Method**: `GET`
- **Description**: Get all tenants
- **Access**: Requires SUPER_ADMIN role
- **Response**:
    - Status Code: 200 OK
  ```json
  {
    "tenants": [
      {
        "id": "string", // UUID
        "companyName": "string",
        "contactEmail": "string",
        "createdAt": "string" // ISO date
      }
    ]
  }
  ```

### Get Tenant by ID
- **URL**: `/api/tenants/{id}`
- **Method**: `GET`
- **Description**: Get tenant by ID
- **Access**: Requires SUPER_ADMIN role
- **Path Parameters**:
    - `id`: Tenant ID (UUID)
- **Response**:
    - Status Code: 200 OK
  ```json
  {
    "id": "string", // UUID
    "companyName": "string",
    "contactEmail": "string",
    "createdAt": "string" // ISO date
  }
  ```

### Get Tenant Users
- **URL**: `/api/tenants/{id}/users`
- **Method**: `GET`
- **Description**: Get users for a specific tenant
- **Access**: Requires SUPER_ADMIN role
- **Path Parameters**:
    - `id`: Tenant ID (UUID)
- **Response**:
    - Status Code: 200 OK
  ```json
  {
    "employees": [
      {
        "id": "integer",
        "name": "string",
        "email": "string",
        "role": "string"
      }
    ]
  }
  ```

### Deactivate Tenant
- **URL**: `/api/tenants/{id}/deactivate`
- **Method**: `PUT`
- **Description**: Deactivate a tenant
- **Access**: Requires SUPER_ADMIN role
- **Path Parameters**:
    - `id`: Tenant ID
- **Response**:
    - Status Code: 200 OK - "Tenant deactivated successfully"

### Activate Tenant
- **URL**: `/api/tenants/{id}/activate`
- **Method**: `PUT`
- **Description**: Reactivate a tenant
- **Access**: Requires SUPER_ADMIN role
- **Path Parameters**:
    - `id`: Tenant ID
- **Response**:
    - Status Code: 200 OK - "Tenant activated successfully"

---

## Admin Operations

### Get Admin Dashboard
- **URL**: `/api/admin/users/dashboard`
- **Method**: `GET`
- **Description**: Get dashboard statistics for the admin's tenant
- **Access**: Requires ADMIN role
- **Response**:
    - Status Code: 200 OK
  ```json
  {
    "totalEmployees": 45,
    "totalSupervisors": 10,
    "totalManagers": 5,
    "totalTasks": 120,
    "taskStatusBreakdown": {
      "PENDING": 25,
      "IN_PROGRESS": 50,
      "COMPLETED": 45
    },
    "recentTasks": 8,
    "adviceCount": 30,
    "errorMessage": null
  }
  ```

### Get All Users
- **URL**: `/api/admin/users`
- **Method**: `GET`
- **Description**: Get all users in the admin's tenant
- **Access**: Requires ADMIN role
- **Response**:
    - Status Code: 200 OK
  ```json
  {
    "employees": [
      {
        "id": "integer",
        "name": "string",
        "email": "string",
        "role": "string"
      }
    ]
  }
  ```

### Get User by ID
- **URL**: `/api/admin/users/{id}`
- **Method**: `GET`
- **Description**: Get user details by ID
- **Access**: Requires ADMIN role
- **Path Parameters**:
    - `id`: User ID
- **Response**:
    - Status Code: 200 OK
  ```json
  {
    "id": "integer",
    "name": "string",
    "email": "string",
    "role": "string",
    "tenantId": "string" // UUID
  }
  ```

### Create User
- **URL**: `/api/admin/users`
- **Method**: `POST`
- **Description**: Create a new user in the admin's tenant
- **Access**: Requires ADMIN role
- **Request Body**:
  ```json
  {
    "name": "string",
    "email": "string",
    "phone_number": "string",
    "role": "string",
    "companyName": "string"
  }
  ```
- **Response**:
    - Status Code: 201 Created
  ```json
  {
    "id": "integer",
    "name": "string",
    "email": "string",
    "role": "string"
  }
  ```

### Update User
- **URL**: `/api/admin/users/{id}`
- **Method**: `PUT`
- **Description**: Update a user in the admin's tenant
- **Access**: Requires ADMIN role
- **Path Parameters**:
    - `id`: User ID
- **Request Body**:
  ```json
  {
    "name": "string", // Optional
    "email": "string", // Optional
    "phone_number": "string", // Optional
    "role": "string", // Optional
    "password": "string", // Optional
    "managerId": "integer" // Optional
  }
  ```
- **Response**:
    - Status Code: 200 OK
  ```json
  {
    "id": "integer",
    "name": "string",
    "email": "string",
    "role": "string",
    "tenantId": "string" // UUID
  }
  ```

### Delete User
- **URL**: `/api/admin/users/{id}`
- **Method**: `DELETE`
- **Description**: Delete a user from the admin's tenant
- **Access**: Requires ADMIN role
- **Path Parameters**:
    - `id`: User ID
- **Response**:
    - Status Code: 200 OK - "User deleted successfully"

### Get Users by Role
- **URL**: `/api/admin/users/by-role/{role}`
- **Method**: `GET`
- **Description**: Get users by specific role
- **Access**: Requires ADMIN role
- **Path Parameters**:
    - `role`: Role name (e.g., "MANAGER", "SUPERVISOR")
- **Response**:
    - Status Code: 200 OK
  ```json
  {
    "employees": [
      {
        "id": "integer",
        "name": "string",
        "email": "string",
        "role": "string"
      }
    ]
  }
  ```

### Get Paginated Users by Role
- **URL**: `/api/admin/users/by-role/{role}/paged`
- **Method**: `GET`
- **Description**: Get paginated users by role
- **Access**: Requires ADMIN role
- **Path Parameters**:
    - `role`: Role name
- **Query Parameters**:
    - `page`: Page number (default: 0)
    - `size`: Page size (default: 10)
- **Response**:
    - Status Code: 200 OK
  ```json
  {
    "employees": [
      {
        "id": "integer",
        "name": "string",
        "email": "string",
        "role": "string"
      }
    ],
    "totalItems": "long",
    "totalPages": "integer",
    "currentPage": "integer"
  }
  ```

### Get Managers
- **URL**: `/api/admin/users/managers`
- **Method**: `GET`
- **Description**: Get paginated list of managers
- **Access**: Requires ADMIN role
- **Query Parameters**:
    - `page`: Page number (default: 0)
    - `size`: Page size (default: 10)
- **Response**:
    - Status Code: 200 OK
  ```json
  {
    "employees": [
      {
        "id": "integer",
        "name": "string",
        "email": "string",
        "role": "string"
      }
    ],
    "totalItems": "long",
    "totalPages": "integer",
    "currentPage": "integer"
  }
  ```

### Get Supervisors
- **URL**: `/api/admin/users/supervisors`
- **Method**: `GET`
- **Description**: Get paginated list of supervisors
- **Access**: Requires ADMIN role
- **Query Parameters**:
    - `page`: Page number (default: 0)
    - `size`: Page size (default: 10)
- **Response**:
    - Status Code: 200 OK
  ```json
  {
    "employees": [
      {
        "id": "integer",
        "name": "string",
        "email": "string",
        "role": "string"
      }
    ],
    "totalItems": "long",
    "totalPages": "integer",
    "currentPage": "integer"
  }
  ```

### Assign Manager to User
- **URL**: `/api/admin/users/{userId}/assign-manager/{managerId}`
- **Method**: `PUT`
- **Description**: Assign a manager to a user
- **Access**: Requires ADMIN role
- **Path Parameters**:
    - `userId`: User ID
    - `managerId`: Manager ID
- **Response**:
    - Status Code: 200 OK
  ```json
  {
    "id": "integer",
    "name": "string",
    "email": "string",
    "role": "string",
    "tenantId": "string" // UUID
  }
  ```

### Remove Manager from User
- **URL**: `/api/admin/users/{userId}/remove-manager`
- **Method**: `PUT`
- **Description**: Remove manager assignment from a user
- **Access**: Requires ADMIN role
- **Path Parameters**:
    - `userId`: User ID
- **Response**:
    - Status Code: 200 OK
  ```json
  {
    "id": "integer",
    "name": "string",
    "email": "string",
    "role": "string",
    "tenantId": "string" // UUID
  }
  ```

### Get Users by Manager
- **URL**: `/api/admin/users/manager/{managerId}`
- **Method**: `GET`
- **Description**: Get users assigned to a specific manager
- **Access**: Requires ADMIN role
- **Path Parameters**:
    - `managerId`: Manager ID
- **Response**:
    - Status Code: 200 OK
  ```json
  {
    "employees": [
      {
        "id": "integer",
        "name": "string",
        "email": "string",
        "role": "string"
      }
    ]
  }
  ```

---

## Manager Operations

### Get Team Members
- **URL**: `/api/manager/users/team`
- **Method**: `GET`
- **Description**: Get all users that report to the current manager
- **Access**: Requires MANAGER role
- **Response**:
    - Status Code: 200 OK
  ```json
  {
    "employees": [
      {
        "id": "integer",
        "name": "string",
        "email": "string",
        "role": "string"
      }
    ]
  }
  ```

### Get Supervisors
- **URL**: `/api/manager/users/supervisors`
- **Method**: `GET`
- **Description**: Get paginated list of supervisors in the manager's tenant
- **Access**: Requires MANAGER role
- **Query Parameters**:
    - `page`: Page number (default: 0)
    - `size`: Page size (default: 10)
- **Response**:
    - Status Code: 200 OK
  ```json
  {
    "employees": [
      {
        "id": "integer",
        "name": "string",
        "email": "string",
        "role": "string"
      }
    ],
    "totalItems": "long",
    "totalPages": "integer",
    "currentPage": "integer"
  }
  ```

### Update Team Member
- **URL**: `/api/manager/users/team/{id}`
- **Method**: `PUT`
- **Description**: Update a team member's details
- **Access**: Requires MANAGER role
- **Path Parameters**:
    - `id`: User ID
- **Request Body**:
  ```json
  {
    "name": "string", // Optional
    "phone_number": "string" // Optional
  }
  ```
- **Response**:
    - Status Code: 200 OK
  ```json
  {
    "id": "integer",
    "name": "string",
    "email": "string",
    "role": "string",
    "tenantId": "string" // UUID
  }
  ```

### Assign Supervisor to Team
- **URL**: `/api/manager/users/assign/{supervisorId}`
- **Method**: `PUT`
- **Description**: Assign a supervisor to the manager's team
- **Access**: Requires MANAGER role
- **Path Parameters**:
    - `supervisorId`: Supervisor ID
- **Response**:
    - Status Code: 200 OK
  ```json
  {
    "id": "integer",
    "name": "string",
    "email": "string",
    "role": "string",
    "tenantId": "string" // UUID
  }
  ```

### Unassign Supervisor from Team
- **URL**: `/api/manager/users/unassign/{supervisorId}`
- **Method**: `PUT`
- **Description**: Remove a supervisor from the manager's team
- **Access**: Requires MANAGER role
- **Path Parameters**:
    - `supervisorId`: Supervisor ID
- **Response**:
    - Status Code: 200 OK
  ```json
  {
    "id": "integer",
    "name": "string",
    "email": "string",
    "role": "string",
    "tenantId": "string" // UUID
  }
  ```

### Create Task
- **URL**: `/api/tasks`
- **Method**: `POST`
- **Description**: Create a new task
- **Access**: Requires MANAGER, SUPERVISOR, or ADMIN role
- **Request Body**:
  ```json
  {
    "taskType": "string",
    "description": "string",
    "detailsJson": "string",
    "imagesJson": "string",
    "assignedToId": "integer" // Optional
  }
  ```
- **Response**:
    - Status Code: 201 Created
  ```json
  {
    "id": "integer",
    "taskType": "string",
    "description": "string",
    "status": "string",
    "createdBy": "string",
    "assignedTo": "string",
    "createdAt": "string",
    "updatedAt": "string",
    "detailsJson": "string",
    "imagesJson": "string",
    "implementationJson": "string"
  }
  ```

### Get All Tasks
- **URL**: `/api/tasks`
- **Method**: `GET`
- **Description**: Get all tasks for the tenant
- **Access**: Requires MANAGER or ADMIN role
- **Query Parameters**:
    - `page`: Page number (default: 0)
    - `size`: Page size (default: 10)
- **Response**:
    - Status Code: 200 OK
  ```json
  {
    "tasks": [
      {
        "id": "integer",
        "taskType": "string",
        "description": "string",
        "status": "string",
        "createdBy": "string",
        "assignedTo": "string",
        "createdAt": "string",
        "updatedAt": "string",
        "detailsJson": "string",
        "imagesJson": "string",
        "implementationJson": "string"
      }
    ],
    "totalCount": "integer"
  }
  ```

#### Get Tasks Assigned to Current User
- **URL**: `/api/tasks/assigned`
- **Method**: `GET`
- **Description**: Get tasks assigned to the current user
- **Access**: Requires MANAGER, SUPERVISOR, ADMIN, or EMPLOYEE role
- **Query Parameters**:
    - `page`: Page number (default: 0)
    - `size`: Page size (default: 10)
- **Response**:
    - Status Code: 200 OK
  ```json
  {
    "tasks": [
      {
        "id": "integer",
        "taskType": "string",
        "description": "string",
        "status": "string",
        "createdBy": "string",
        "assignedTo": "string",
        "createdAt": "string",
        "updatedAt": "string",
        "detailsJson": "string",
        "imagesJson": "string",
        "implementationJson": "string"
      }
    ],
    "totalCount": "integer"
  }
  ```

#### Get Advice for Task
- **URL**: `/api/task-advices/task/{taskId}`
- **Method**: `GET`
- **Description**: Get all advice for a task
- **Access**: Requires MANAGER, SUPERVISOR, ADMIN, or EMPLOYEE role
- **Path Parameters**:
    - `taskId`: Task ID
- **Response**:
    - Status Code: 200 OK
  ```json
  {
    "advices": [
      {
        "id": "integer",
        "taskId": "integer",
        "managerName": "string",
        "adviceText": "string",
        "createdAt": "string"
      }
    ]
  }
  ```

### Notification Operations

#### Get Unread Notification Count
- **URL**: `/api/notifications/unread/count`
- **Method**: `GET`
- **Description**: Get count of unread notifications for current user
- **Access**: Authenticated
- **Response**:
    - Status Code: 200 OK
  ```json
  {
    "count": "integer"
  }
  ```

#### Get Notifications
- **URL**: `/api/notifications`
- **Method**: `GET`
- **Description**: Get notifications for current user
- **Access**: Authenticated
- **Query Parameters**:
    - `onlyUnread`: Only show unread notifications (default: false)
    - `page`: Page number (default: 0)
    - `size`: Page size (default: 10)
- **Response**:
    - Status Code: 200 OK
  ```json
  {
    "notifications": [
      {
        "id": "integer",
        "title": "string",
        "message": "string",
        "taskId": "integer",
        "isRead": "boolean",
        "createdAt": "string"
      }
    ],
    "totalCount": "integer",
    "currentPage": "integer",
    "totalPages": "integer"
  }
  ```

#### Mark Notification as Read
- **URL**: `/api/notifications/{notificationId}/read`
- **Method**: `PUT`
- **Description**: Mark a notification as read
- **Access**: Authenticated
- **Path Parameters**:
    - `notificationId`: Notification ID
- **Response**:
    - Status Code: 200 OK
  ```json
  {
    "success": true
  }
  ```

#### Mark All Notifications as Read
- **URL**: `/api/notifications/read-all`
- **Method**: `PUT`
- **Description**: Mark all notifications as read
- **Access**: Authenticated
- **Response**:
    - Status Code: 200 OK
  ```json
  {
    "success": true
  }
  ```Json": "string"
      }
    ],
    "totalCount": "integer"
  }
  ```

### Get Tasks By Status
- **URL**: `/api/tasks/status/{status}`
- **Method**: `GET`
- **Description**: Get tasks by status
- **Access**: Requires MANAGER or ADMIN role
- **Path Parameters**:
    - `status`: Task status
- **Query Parameters**:
    - `page`: Page number (default: 0)
    - `size`: Page size (default: 10)
- **Response**:
    - Status Code: 200 OK
  ```json
  {
    "tasks": [
      {
        "id": "integer",
        "taskType": "string",
        "description": "string",
        "status": "string",
        "createdBy": "string",
        "assignedTo": "string",
        "createdAt": "string",
        "updatedAt": "string",
        "detailsJson": "string",
        "imagesJson": "string",
        "implementationJson": "string"
      }
    ],
    "totalCount": "integer"
  }
  ```

### Update Task Status
- **URL**: `/api/tasks/{taskId}/status`
- **Method**: `PUT`
- **Description**: Update task status
- **Access**: Requires MANAGER, SUPERVISOR, or ADMIN role
- **Path Parameters**:
    - `taskId`: Task ID
- **Request Body**:
  ```json
  {
    "status": "string", // "approved", "rejected", "implemented"
    "comment": "string" // Optional
  }
  ```
- **Response**:
    - Status Code: 200 OK
  ```json
  {
    "id": "integer",
    "taskType": "string",
    "description": "string",
    "status": "string",
    "createdBy": "string",
    "assignedTo": "string",
    "createdAt": "string",
    "updatedAt": "string",
    "detailsJson": "string",
    "imagesJson": "string",
    "implementationJson": "string"
  }
  ```

### Assign Task to User
- **URL**: `/api/tasks/{taskId}/assign/{assignedToId}`
- **Method**: `PUT`
- **Description**: Assign task to a user
- **Access**: Requires MANAGER or ADMIN role
- **Path Parameters**:
    - `taskId`: Task ID
    - `assignedToId`: User ID to assign the task to
- **Response**:
    - Status Code: 200 OK
  ```json
  {
    "id": "integer",
    "taskType": "string",
    "description": "string",
    "status": "string",
    "createdBy": "string",
    "assignedTo": "string",
    "createdAt": "string",
    "updatedAt": "string",
    "detailsJson": "string",
    "imagesJson": "string",
    "implementationJson": "string"
  }
  ```

### Get Task Report
- **URL**: `/api/tasks/report`
- **Method**: `GET`
- **Description**: Get task report with statistics
- **Access**: Requires MANAGER or ADMIN role
- **Response**:
    - Status Code: 200 OK
  ```json
  {
    "totalTasks": "integer",
    "tasksByType": {
      "taskType1": "integer",
      "taskType2": "integer"
    },
    "tasksByStatus": {
      "PENDING": "integer",
      "IN_PROGRESS": "integer",
      "COMPLETED": "integer"
    },
    "tasksByUser": {
      "userName1": "integer",
      "userName2": "integer"
    },
    "avgCompletionTimeByType": {
      "taskType1": "double",
      "taskType2": "double"
    }
  }
  ```

### Create Task Advice
- **URL**: `/api/task-advices`
- **Method**: `POST`
- **Description**: Create advice for a task
- **Access**: Requires MANAGER or ADMIN role
- **Request Body**:
  ```json
  {
    "taskId": "integer",
    "adviceText": "string"
  }
  ```
- **Response**:
    - Status Code: 201 Created
  ```json
  {
    "id": "integer",
    "taskId": "integer",
    "managerName": "string",
    "adviceText": "string",
    "createdAt": "string"
  }
  ```

### Get Advice Provided By Me
- **URL**: `/api/task-advices/provided`
- **Method**: `GET`
- **Description**: Get all advice provided by the current manager
- **Access**: Requires MANAGER or ADMIN role
- **Response**:
    - Status Code: 200 OK
  ```json
  {
    "advices": [
      {
        "id": "integer",
        "taskId": "integer",
        "managerName": "string",
        "adviceText": "string",
        "createdAt": "string"
      }
    ]
  }
  ```

---

## Supervisor Operations

### Get Manager
- **URL**: `/api/supervisor/manager`
- **Method**: `GET`
- **Description**: Get the supervisor's manager
- **Access**: Requires SUPERVISOR role
- **Response**:
    - Status Code: 200 OK
  ```json
  {
    "id": "integer",
    "name": "string",
    "email": "string",
    "role": "string",
    "tenantId": "string" // UUID
  }
  ```

### Update Profile
- **URL**: `/api/supervisor/profile`
- **Method**: `PUT`
- **Description**: Update the supervisor's own profile
- **Access**: Requires SUPERVISOR role
- **Request Body**:
  ```json
  {
    "name": "string", // Optional
    "phone_number": "string", // Optional
    "password": "string" // Optional
  }
  ```
- **Response**:
    - Status Code: 200 OK
  ```json
  {
    "id": "integer",
    "name": "string",
    "email": "string",
    "role": "string",
    "tenantId": "string" // UUID
  }
  ```

### Get Profile
- **URL**: `/api/supervisor/profile`
- **Method**: `GET`
- **Description**: Get supervisor's profile
- **Access**: Requires SUPERVISOR role
- **Response**:
    - Status Code: 200 OK
  ```json
  {
    "id": "integer",
    "name": "string",
    "email": "string",
    "role": "string",
    "tenantId": "string" // UUID
  }
  ```

### Create Task
- **URL**: `/api/tasks`
- **Method**: `POST`
- **Description**: Create a new task
- **Access**: Requires MANAGER, SUPERVISOR, or ADMIN role
- **Request Body**:
  ```json
  {
    "taskType": "string",
    "description": "string",
    "detailsJson": "string",
    "imagesJson": "string",
    "assignedToId": "integer" // Optional
  }
  ```
- **Response**:
    - Status Code: 201 Created
  ```json
  {
    "id": "integer",
    "taskType": "string",
    "description": "string",
    "status": "string",
    "createdBy": "string",
    "assignedTo": "string",
    "createdAt": "string",
    "updatedAt": "string",
    "detailsJson": "string",
    "imagesJson": "string",
    "implementationJson": "string"
  }
  ```

### Update Task Status
- **URL**: `/api/tasks/{taskId}/status`
- **Method**: `PUT`
- **Description**: Update task status
- **Access**: Requires MANAGER, SUPERVISOR, or ADMIN role
- **Path Parameters**:
    - `taskId`: Task ID
- **Request Body**:
  ```json
  {
    "status": "string", // "approved", "rejected", "implemented"
    "comment": "string" // Optional
  }
  ```
- **Response**:
    - Status Code: 200 OK
  ```json
  {
    "id": "integer",
    "taskType": "string",
    "description": "string",
    "status": "string",
    "createdBy": "string",
    "assignedTo": "string",
    "createdAt": "string",
    "updatedAt": "string",
    "detailsJson": "string",
    "imagesJson": "string",
    "implementationJson": "string"
  }
  ```

### Update Task Implementation
- **URL**: `/api/tasks/{taskId}/implementation`
- **Method**: `PUT`
- **Description**: Update task implementation
- **Access**: Requires SUPERVISOR or EMPLOYEE role
- **Path Parameters**:
    - `taskId`: Task ID
- **Request Body**:
  ```json
  {
    "implementationJson": "string"
  }
  ```
- **Response**:
    - Status Code: 200 OK
  ```json
  {
    "id": "integer",
    "taskType": "string",
    "description": "string",
    "status": "string",
    "createdBy": "string",
    "assignedTo": "string",
    "createdAt": "string",
    "updatedAt": "string",
    "detailsJson": "string",
    "imagesJson": "string",
    "implementationJson": "string"
  }
  ```

---

## Common Operations

### Get Current User
- **URL**: `/api/user/me`
- **Method**: `GET`
- **Description**: Get current authenticated user details
- **Access**: Authenticated
- **Response**:
    - Status Code: 200 OK
  ```json
  {
    "id": "integer",
    "name": "string",
    "email": "string",
    "role": "string",
    "tenantId": "string" // UUID
  }
  ```
# Sasyak API Documentation - Task Operations

### Task Operations

#### Get Task by ID
- **URL**: `/api/tasks/{taskId}`
- **Method**: `GET`
- **Description**: Get a specific task with advice
- **Access**: Requires MANAGER, SUPERVISOR, ADMIN, or EMPLOYEE role
- **Path Parameters**:
    - `taskId`: Task ID
- **Response**:
    - Status Code: 200 OK
  ```json
  {
    "task": {
      "id": "integer",
      "taskType": "string",
      "description": "string",
      "status": "string",
      "createdBy": "string",
      "assignedTo": "string",
      "createdAt": "string",
      "updatedAt": "string",
      "detailsJson": "string",
      "imagesJson": "string",
      "implementationJson": "string"
    },
    "advices": [
      {
        "id": "integer",
        "taskId": "integer",
        "managerName": "string",
        "adviceText": "string",
        "createdAt": "string"
      }
    ]
  }
  ```

#### Get Tasks Created by Current User
- **URL**: `/api/tasks/created`
- **Method**: `GET`
- **Description**: Get tasks created by the current user
- **Access**: Requires MANAGER, SUPERVISOR, or ADMIN role
- **Query Parameters**:
    - `page`: Page number (default: 0)
    - `size`: Page size (default: 10)
- **Response**:
    - Status Code: 200 OK
  ```json
  {
    "tasks": [
      {
        "id": "integer",
        "taskType": "string",
        "description": "string",
        "status": "string",
        "createdBy": "string",
        "assignedTo": "string",
        "createdAt": "string",
        "updatedAt": "string",
        "detailsJson": "string",
        "imagesJson": "string",
        "implementationJson": "string"
      }
    ],
    "totalCount": "integer"
  }
  ```

#### Get Tasks Assigned to Current User
- **URL**: `/api/tasks/assigned`
- **Method**: `GET`
- **Description**: Get tasks assigned to the current user
- **Access**: Requires MANAGER, SUPERVISOR, ADMIN, or EMPLOYEE role
- **Query Parameters**:
    - `page`: Page number (default: 0)
    - `size`: Page size (default: 10)
- **Response**:
    - Status Code: 200 OK
  ```json
  {
    "tasks": [
      {
        "id": "integer",
        "taskType": "string",
        "description": "string",
        "status": "string",
        "createdBy": "string",
        "assignedTo": "string",
        "createdAt": "string",
        "updatedAt": "string",
        "detailsJson": "string",
        "imagesJson": "string",
        "implementationJson": "string"
      }
    ],
    "totalCount": "integer"
  }
  ```

#### Get All Tasks
- **URL**: `/api/tasks`
- **Method**: `GET`
- **Description**: Get all tasks for the tenant
- **Access**: Requires MANAGER or ADMIN role
- **Query Parameters**:
    - `page`: Page number (default: 0)
    - `size`: Page size (default: 10)
- **Response**:
    - Status Code: 200 OK
  ```json
  {
    "tasks": [
      {
        "id": "integer",
        "taskType": "string",
        "description": "string",
        "status": "string",
        "createdBy": "string",
        "assignedTo": "string",
        "createdAt": "string",
        "updatedAt": "string",
        "detailsJson": "string",
        "imagesJson": "string",
        "implementationJson": "string"
      }
    ],
    "totalCount": "integer"
  }
  ```

#### Get Tasks by Status
- **URL**: `/api/tasks/status/{status}`
- **Method**: `GET`
- **Description**: Get tasks by status
- **Access**: Requires MANAGER or ADMIN role
- **Path Parameters**:
    - `status`: Task status (e.g., "PENDING", "IN_PROGRESS", "COMPLETED")
- **Query Parameters**:
    - `page`: Page number (default: 0)
    - `size`: Page size (default: 10)
- **Response**:
    - Status Code: 200 OK
  ```json
  {
    "tasks": [
      {
        "id": "integer",
        "taskType": "string",
        "description": "string",
        "status": "string",
        "createdBy": "string",
        "assignedTo": "string",
        "createdAt": "string",
        "updatedAt": "string",
        "detailsJson": "string",
        "imagesJson": "string",
        "implementationJson": "string"
      }
    ],
    "totalCount": "integer"
  }
  ```

#### Create Task
- **URL**: `/api/tasks`
- **Method**: `POST`
- **Description**: Create a new task
- **Access**: Requires MANAGER, SUPERVISOR, or ADMIN role
- **Request Body**:
  ```json
  {
    "taskType": "string",
    "description": "string",
    "detailsJson": "string",
    "imagesJson": "string",
    "assignedToId": "integer" // Optional
  }
  ```
- **Response**:
    - Status Code: 201 Created
  ```json
  {
    "id": "integer",
    "taskType": "string",
    "description": "string",
    "status": "string",
    "createdBy": "string",
    "assignedTo": "string",
    "createdAt": "string",
    "updatedAt": "string",
    "detailsJson": "string",
    "imagesJson": "string",
    "implementationJson": "string"
  }
  ```

#### Update Task Status
- **URL**: `/api/tasks/{taskId}/status`
- **Method**: `PUT`
- **Description**: Update task status
- **Access**: Requires MANAGER, SUPERVISOR, or ADMIN role
- **Path Parameters**:
    - `taskId`: Task ID
- **Request Body**:
  ```json
  {
    "status": "string", // "PENDING", "IN_PROGRESS", "COMPLETED"
    "comment": "string" // Optional
  }
  ```
- **Response**:
    - Status Code: 200 OK
  ```json
  {
    "id": "integer",
    "taskType": "string",
    "description": "string",
    "status": "string",
    "createdBy": "string",
    "assignedTo": "string",
    "createdAt": "string",
    "updatedAt": "string",
    "detailsJson": "string",
    "imagesJson": "string",
    "implementationJson": "string"
  }
  ```

#### Update Task Implementation
- **URL**: `/api/tasks/{taskId}/implementation`
- **Method**: `PUT`
- **Description**: Update task implementation
- **Access**: Requires SUPERVISOR or EMPLOYEE role
- **Path Parameters**:
    - `taskId`: Task ID
- **Request Body**:
  ```json
  {
    "implementationJson": "string"
  }
  ```
- **Response**:
    - Status Code: 200 OK
  ```json
  {
    "id": "integer",
    "taskType": "string",
    "description": "string",
    "status": "string",
    "createdBy": "string",
    "assignedTo": "string",
    "createdAt": "string",
    "updatedAt": "string",
    "detailsJson": "string",
    "imagesJson": "string",
    "implementationJson": "string"
  }
  ```

#### Assign Task to User
- **URL**: `/api/tasks/{taskId}/assign/{assignedToId}`
- **Method**: `PUT`
- **Description**: Assign task to a user
- **Access**: Requires MANAGER or ADMIN role
- **Path Parameters**:
    - `taskId`: Task ID
    - `assignedToId`: User ID to assign the task to
- **Response**:
    - Status Code: 200 OK
  ```json
  {
    "id": "integer",
    "taskType": "string",
    "description": "string",
    "status": "string",
    "createdBy": "string",
    "assignedTo": "string",
    "createdAt": "string",
    "updatedAt": "string",
    "detailsJson": "string",
    "imagesJson": "string",
    "implementationJson": "string"
  }
  ```

#### Get Task Report
- **URL**: `/api/tasks/report`
- **Method**: `GET`
- **Description**: Get task report with statistics
- **Access**: Requires MANAGER or ADMIN role
- **Response**:
    - Status Code: 200 OK
  ```json
  {
    "totalTasks": "integer",
    "tasksByType": {
      "taskType1": "integer",
      "taskType2": "integer"
    },
    "tasksByStatus": {
      "PENDING": "integer",
      "IN_PROGRESS": "integer",
      "COMPLETED": "integer"
    },
    "tasksByUser": {
      "userName1": "integer",
      "userName2": "integer"
    },
    "avgCompletionTimeByType": {
      "taskType1": "double",
      "taskType2": "double"
    }
  }
  ```

### Task Advice Operations

#### Create Task Advice
- **URL**: `/api/task-advices`
- **Method**: `POST`
- **Description**: Create advice for a task
- **Access**: Requires MANAGER or ADMIN role
- **Request Body**:
  ```json
  {
    "taskId": "integer",
    "adviceText": "string"
  }
  ```
- **Response**:
    - Status Code: 201 Created
  ```json
  {
    "id": "integer",
    "taskId": "integer",
    "managerName": "string",
    "adviceText": "string",
    "createdAt": "string"
  }
  ```

#### Get Advice for Task
- **URL**: `/api/task-advices/task/{taskId}`
- **Method**: `GET`
- **Description**: Get all advice for a task
- **Access**: Requires MANAGER, SUPERVISOR, ADMIN, or EMPLOYEE role
- **Path Parameters**:
    - `taskId`: Task ID
- **Response**:
    - Status Code: 200 OK
  ```json
  {
    "advices": [
      {
        "id": "integer",
        "taskId": "integer",
        "managerName": "string",
        "adviceText": "string",
        "createdAt": "string"
      }
    ]
  }
  ```

#### Get Advice Provided by Current Manager
- **URL**: `/api/task-advices/provided`
- **Method**: `GET`
- **Description**: Get all advice provided by the current manager
- **Access**: Requires MANAGER or ADMIN role
- **Response**:
    - Status Code: 200 OK
  ```json
  {
    "advices": [
      {
        "id": "integer",
        "taskId": "integer",
        "managerName": "string",
        "adviceText": "string",
        "createdAt": "string"
      }
    ]
  }
  ```
## Error Handling for Tasks

### Common Task-Related Errors

- **403 Forbidden** - User doesn't have permission to access/modify the task
- **404 Not Found** - Task not found with the specified ID
- **400 Bad Request** - Invalid task data or status transition

### Sample Error Response
```json
{
  "timestamp": "2024-05-01T12:00:00Z",
  "status": 403,
  "error": "Forbidden",
  "message": "You don't have permission to update this task",
  "path": "/api/tasks/123/status"
}
```

## Task Status Flow

Tasks typically follow this status flow:

1. **PENDING** - Initial state when task is created
2. **IN_PROGRESS** - Task is currently being worked on
3. **COMPLETED** - Task has been implemented and verified

The status transitions available depend on the user's role:
- **MANAGERS/ADMINS** can change status to any state
- **SUPERVISORS** can only change from PENDING to IN_PROGRESS or IN_PROGRESS to COMPLETED
- **EMPLOYEES** can only change from PENDING to IN_PROGRESS

## Task Lifecycle

1. **Creation** - Task is created by a MANAGER, SUPERVISOR, or ADMIN
2. **Assignment** - Task is assigned to a user (SUPERVISOR or EMPLOYEE)
3. **Implementation** - Assigned user updates the task implementation
4. **Advice** - MANAGERs or ADMINs can provide advice on tasks
5. **Completion** - Task is marked as completed

## Best Practices for Task API Usage

1. Always check task status before allowing status updates
2. Use pagination for task lists to improve performance
3. Include detailed implementation information in `implementationJson`
4. Keep task descriptions concise but informative
5. Use the task report endpoint for analytics rather than fetching all tasks