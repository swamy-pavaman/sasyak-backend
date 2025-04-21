# Sasyak API Documentation

## Table of Contents
- [Authentication](#authentication)
    - [Register User](#register-user)
    - [Login User](#login-user)
    - [Login Super Admin](#login-super-admin)
    - [Refresh Token](#refresh-token)
- [User Management](#user-management)
    - [Get User by ID](#get-user-by-id)
    - [Get Current User](#get-current-user)
    - [Create User](#create-user)
    - [Update User](#update-user)
    - [Delete User](#delete-user)
- [Admin User Management](#admin-user-management)
    - [Get All Users](#get-all-users)
    - [Get User by ID (Admin)](#get-user-by-id-admin)
    - [Create User (Admin)](#create-user-admin)
    - [Update User (Admin)](#update-user-admin)
    - [Delete User (Admin)](#delete-user-admin)
    - [Get Users by Role](#get-users-by-role)
    - [Get Paginated Users by Role](#get-paginated-users-by-role)
    - [Assign Manager to User](#assign-manager-to-user)
    - [Remove Manager from User](#remove-manager-from-user)
    - [Get Users by Manager](#get-users-by-manager)
- [Manager User Management](#manager-user-management)
    - [Get Team Members](#get-team-members)
    - [Get All Supervisors](#get-all-supervisors)
    - [Get Team Member by ID](#get-team-member-by-id)
    - [Update Team Member](#update-team-member)
    - [Assign Supervisor to Team](#assign-supervisor-to-team)
    - [Unassign Supervisor from Team](#unassign-supervisor-from-team)
- [Supervisor Operations](#supervisor-operations)
    - [Get Manager](#get-manager)
    - [Update Profile](#update-profile)
    - [Get Profile](#get-profile)
- [Super Admin Operations](#super-admin-operations)
    - [Create Tenant](#create-tenant)
    - [Get All Tenants](#get-all-tenants)
    - [Get Tenant by ID](#get-tenant-by-id)
    - [Deactivate Tenant](#deactivate-tenant)
    - [Activate Tenant](#activate-tenant)
- [Task Operations](#task-operations)
    - [Create Task](#create-task)
    - [Get Task by ID](#get-task-by-id)
    - [Get Tasks Created by Current User](#get-tasks-created-by-current-user)
    - [Get Tasks Assigned to Current User](#get-tasks-assigned-to-current-user)
    - [Get All Tasks](#get-all-tasks)
    - [Get Tasks by Status](#get-tasks-by-status)
    - [Update Task Status](#update-task-status)
    - [Update Task Implementation](#update-task-implementation)
    - [Assign Task to User](#assign-task-to-user)
    - [Get Task Report](#get-task-report)
- [Task Advice Operations](#task-advice-operations)
    - [Create Task Advice](#create-task-advice)
    - [Get Advice for Task](#get-advice-for-task)
    - [Get Advice Provided by Current Manager](#get-advice-provided-by-current-manager)
- [Notification Operations](#notification-operations)
    - [Get Unread Notification Count](#get-unread-notification-count)
    - [Get Notifications](#get-notifications)
    - [Mark Notification as Read](#mark-notification-as-read)
    - [Mark All Notifications as Read](#mark-all-notifications-as-read)

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
    - Status Code: 409 Conflict - User already exists
    - Status Code: 500 Internal Server Error

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
    "userId": "integer"
  }
  ```
    - Status Code: 401 Unauthorized - Invalid credentials
    - Status Code: 500 Internal Server Error

### Login Super Admin
- **URL**: `/api/auth/superadmin/login`
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
    - Status Code: 401 Unauthorized - Invalid credentials or not authorized as super admin
    - Status Code: 500 Internal Server Error

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
    - Status Code: 401 Unauthorized - Invalid or expired refresh token

---

## User Management

### Get User by ID
- **URL**: `/api/user/{id}`
- **Method**: `GET`
- **Description**: Get user details by ID
- **Access**: Authenticated
- **Path Parameters**:
    - `id`: User ID
- **Response**:
    - Status Code: 200 OK
  ```json
  {
    "userId": "integer",
    "name": "string",
    "email": "string",
    "phoneNumber": "string",
    "role": "string",
    "managerId": "integer"
  }
  ```
    - Status Code: 404 Not Found - User not found

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
    - Status Code: 401 Unauthorized - User not authenticated
    - Status Code: 404 Not Found - User not found
    - Status Code: 500 Internal Server Error

### Create User
- **URL**: `/api/user`
- **Method**: `POST`
- **Description**: Create a new user
- **Access**: Authenticated
- **Request Body**:
  ```json
  {
    "name": "string",
    "email": "string",
    "phone_number": "string",
    "role": "string",
    "password": "string",
    "tenantId": "string", // UUID
    "managerId": "integer" // Optional
  }
  ```
- **Response**:
    - Status Code: 201 Created
  ```json
  {
    "id": "integer",
    "name": "string",
    "email": "string",
    "role": "string",
    "tenantId": "string" // UUID
  }
  ```
    - Status Code: 500 Internal Server Error

### Update User
- **URL**: `/api/user/{id}`
- **Method**: `PUT`
- **Description**: Update user details
- **Access**: Authenticated
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
    - Status Code: 404 Not Found - User not found
    - Status Code: 500 Internal Server Error

### Delete User
- **URL**: `/api/user/{id}`
- **Method**: `DELETE`
- **Description**: Delete a user
- **Access**: Authenticated
- **Path Parameters**:
    - `id`: User ID
- **Response**:
    - Status Code: 200 OK - "User deleted successfully"
    - Status Code: 404 Not Found - User not found
    - Status Code: 500 Internal Server Error

---

## Admin User Management

### 📊 Get Admin Dashboard Stats
- **URL**: `/api/admin/dashboard`
- **Method**: `GET`
- **Description**: Fetch dashboard statistics for the current tenant, including employee counts, task stats, and advice count.
- **Access**: Requires `ADMIN` role

---

### ✅ Successful Response
- **Status Code**: `200 OK`
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

---

### ❌ Error Response
- **Status Code**: `500 Internal Server Error`
```json
{
  "totalEmployees": 0,
  "totalSupervisors": 0,
  "totalManagers": 0,
  "totalTasks": 0,
  "taskStatusBreakdown": {},
  "recentTasks": 0,
  "adviceCount": 0,
  "errorMessage": "Failed to fetch dashboard stats: <error message>"
}
```

### Get All Users
- **URL**: `/api/admin/users`
- **Method**: `GET`
- **Description**: Get all users for the current tenant
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
    - Status Code: 500 Internal Server Error

### Get User by ID (Admin)
- **URL**: `/api/admin/users/{id}`
- **Method**: `GET`
- **Description**: Get user details by ID (Admin access)
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
    - Status Code: 403 Forbidden - Not authorized to access this user
    - Status Code: 404 Not Found - User not found
    - Status Code: 500 Internal Server Error

### Create User (Admin)
- **URL**: `/api/admin/users`
- **Method**: `POST`
- **Description**: Create a new user (Admin access)
- **Access**: Requires ADMIN role
- **Request Body**:
  ```json
  {
    "name": "string",
    "email": "string",
    "phone_number": "string",
    "role": "string",
    "managerId": "integer" // Optional
  }
  ```
- **Response**:
    - Status Code: 201 Created
  ```json
  {
    "id": "integer",
    "name": "string",
    "email": "string",
    "role": "string",
    "tenantId": "string" // UUID
  }
  ```
    - Status Code: 500 Internal Server Error

### Update User (Admin)
- **URL**: `/api/admin/users/{id}`
- **Method**: `PUT`
- **Description**: Update user details (Admin access)
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
    - Status Code: 403 Forbidden - Not authorized to modify this user
    - Status Code: 404 Not Found - User not found
    - Status Code: 500 Internal Server Error

### Delete User (Admin)
- **URL**: `/api/admin/users/{id}`
- **Method**: `DELETE`
- **Description**: Delete a user (Admin access)
- **Access**: Requires ADMIN role
- **Path Parameters**:
    - `id`: User ID
- **Response**:
    - Status Code: 200 OK - "User deleted successfully"
    - Status Code: 403 Forbidden - Not authorized to delete this user
    - Status Code: 404 Not Found - User not found
    - Status Code: 500 Internal Server Error

### Get Users by Role
- **URL**: `/api/admin/users/by-role/{role}`
- **Method**: `GET`
- **Description**: Get users by role for the current tenant
- **Access**: Requires ADMIN role
- **Path Parameters**:
    - `role`: User role (e.g., "MANAGER", "SUPERVISOR")
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
    - Status Code: 500 Internal Server Error

### Get Paginated Users by Role
- **URL**: `/api/admin/users/by-role/{role}/paged`
- **Method**: `GET`
- **Description**: Get paginated users by role for the current tenant
- **Access**: Requires ADMIN role
- **Path Parameters**:
    - `role`: User role (e.g., "MANAGER", "SUPERVISOR")
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
    - Status Code: 500 Internal Server Error

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
    - Status Code: 400 Bad Request - The specified user is not a manager
    - Status Code: 404 Not Found - User or manager not found
    - Status Code: 500 Internal Server Error

### Remove Manager from User
- **URL**: `/api/admin/users/{userId}/remove-manager`
- **Method**: `PUT`
- **Description**: Remove manager from a user
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
    - Status Code: 404 Not Found - User not found
    - Status Code: 500 Internal Server Error

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
    - Status Code: 400 Bad Request - The specified user is not a manager
    - Status Code: 404 Not Found - Manager not found
    - Status Code: 500 Internal Server Error

---

## Manager User Management

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
    - Status Code: 500 Internal Server Error

### Get All Supervisors
- **URL**: `/api/manager/users/supervisors`
- **Method**: `GET`
- **Description**: Get supervisors from the manager's tenant
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
    - Status Code: 500 Internal Server Error

### Get Team Member by ID
- **URL**: `/api/manager/users/{id}`
- **Method**: `GET`
- **Description**: Get user by ID (manager can only access users in their tenant and team)
- **Access**: Requires MANAGER role
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
    - Status Code: 403 Forbidden - Not authorized to access this user
    - Status Code: 404 Not Found - User not found
    - Status Code: 500 Internal Server Error

### Update Team Member
- **URL**: `/api/manager/users/team/{id}`
- **Method**: `PUT`
- **Description**: Update a team member (manager can only update users in their team)
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
    - Status Code: 403 Forbidden - Not authorized to modify this user
    - Status Code: 404 Not Found - User not found
    - Status Code: 500 Internal Server Error

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
    - Status Code: 400 Bad Request - Invalid supervisor ID or user is not a supervisor
    - Status Code: 404 Not Found - Supervisor not found
    - Status Code: 500 Internal Server Error

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
    - Status Code: 400 Bad Request - Supervisor is not part of your team
    - Status Code: 404 Not Found - Supervisor not found
    - Status Code: 500 Internal Server Error

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
    - Status Code: 404 Not Found - No manager assigned
    - Status Code: 500 Internal Server Error

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
    - Status Code: 500 Internal Server Error

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
    - Status Code: 500 Internal Server Error

---

## Super Admin Operations

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
    - Status Code: 500 Internal Server Error

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
    - Status Code: 500 Internal Server Error

### Get Tenant by ID
- **URL**: `/api/tenants/{id}`
- **Method**: `GET`
- **Description**: Get tenant by ID
- **Access**: Requires SUPER_ADMIN role
- **Path Parameters**:
    - `id`: Tenant ID
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
    - Status Code: 404 Not Found - Tenant not found
    - Status Code: 500 Internal Server Error

### Deactivate Tenant
- **URL**: `/api/tenants/{id}/deactivate`
- **Method**: `PUT`
- **Description**: Deactivate a tenant
- **Access**: Requires SUPER_ADMIN role
- **Path Parameters**:
    - `id`: Tenant ID
- **Response**:
    - Status Code: 200 OK - "Tenant deactivated successfully"
    - Status Code: 404 Not Found - Tenant not found
    - Status Code: 500 Internal Server Error

### Activate Tenant
- **URL**: `/api/tenants/{id}/activate`
- **Method**: `PUT`
- **Description**: Reactivate a tenant
- **Access**: Requires SUPER_ADMIN role
- **Path Parameters**:
    - `id`: Tenant ID
- **Response**:
    - Status Code: 200 OK - "Tenant activated successfully"
    - Status Code: 404 Not Found - Tenant not found
    - Status Code: 500 Internal Server Error

---

## Task Operations

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
    - Status Code: 400 Bad Request - Invalid input
    - Status Code: 500 Internal Server Error

### Get Task by ID
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
    - Status Code: 403 Forbidden - Not authorized to access this task
    - Status Code: 404 Not Found - Task not found
    - Status Code: 500 Internal Server Error

### Get Tasks Created by Current User
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
    - Status Code: 500 Internal Server Error

### Get Tasks by Status
- **URL**: `/api/tasks/status/{status}`
- **Method**: `GET`
- **Description**: Get tasks by status
- **Access**: Requires MANAGER or ADMIN role
- **Path Parameters**:
    - `status`: Task status (e.g., "submitted", "approved", "rejected", "implemented")
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
    - Status Code: 500 Internal Server Error

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
    - Status Code: 403 Forbidden - Not authorized to update this task
    - Status Code: 404 Not Found - Task not found
    - Status Code: 500 Internal Server Error

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
    - Status Code: 403 Forbidden - Not authorized to update this task
    - Status Code: 404 Not Found - Task not found
    - Status Code: 500 Internal Server Error

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
    - Status Code: 400 Bad Request - Invalid assigned user
    - Status Code: 403 Forbidden - Not authorized to update this task
    - Status Code: 404 Not Found - Task not found
    - Status Code: 500 Internal Server Error

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
      "submitted": "integer",
      "approved": "integer",
      "rejected": "integer",
      "implemented": "integer"
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
    - Status Code: 500 Internal Server Error

---

## Task Advice Operations

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
    - Status Code: 400 Bad Request - Invalid input
    - Status Code: 403 Forbidden - Not authorized to provide advice for this task
    - Status Code: 500 Internal Server Error

### Get Advice for Task
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
    - Status Code: 403 Forbidden - Not authorized to access advice for this task
    - Status Code: 500 Internal Server Error

### Get Advice Provided by Current Manager
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
    - Status Code: 500 Internal Server Error

---

## Notification Operations

### Get Unread Notification Count
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
    - Status Code: 500 Internal Server Error

### Get Notifications
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
    - Status Code: 500 Internal Server Error

### Mark Notification as Read
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
    - Status Code: 403 Forbidden - Not authorized to access this notification
    - Status Code: 500 Internal Server Error

### Mark All Notifications as Read
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
  ```
    - Status Code: 500 Internal Server Error",
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
    - Status Code: 500 Internal Server Error

### Get Tasks Assigned to Current User
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
    - Status Code: 500 Internal Server Error

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
        "taskType": "string,
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

  