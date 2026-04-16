# Dashboard Visuals Application

A comprehensive monitoring and visualization platform for tracking service health, API endpoints, websites, servers, and custom data metrics across Broadcast, Print, and Online services.

## 📋 Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Database Setup](#database-setup)
- [Configuration](#configuration)
- [Running the Application](#running-the-application)
- [API Documentation](#api-documentation)
- [Testing](#testing)
- [Troubleshooting](#troubleshooting)
- [Future Improvements](#future-improvements)

---

## Overview

The Dashboard Visuals Application solves the challenge of monitoring complex service ecosystems by providing:

- **Real-time visualization** of service metrics through customizable dashboards
- **Proactive monitoring** of websites, APIs, and servers with automatic alerting
- **Data flow visualization** showing how information moves through your organization
- **Centralized alert management** with email notifications

---

## Features

### 1. Service Dashboards
- **Service Overview**: At-a-glance health status for Broadcast, Print, and Online services
- **Flow Diagram**: Snake-pattern visualization showing data flow from procurement to delivery
- **Custom Visuals**: Text indicators, status badges, bar charts, and time-series graphs

### 2. Monitoring System
- **Site Monitors**: Track website availability, content verification, and response times
- **API Monitors**: Monitor REST endpoints with custom headers, body, and JSONPath validation
- **Server Monitors**: Ping servers and check TCP/HTTP/HTTPS connectivity
- **Retry Logic**: Configurable retry attempts before triggering alerts
- **Business Hours**: Schedule monitoring only during specified hours/days

### 3. Alert Management
- **Severity Levels**: Critical, High, Medium, Low with color coding
- **Email Notifications**: Automatic alerts with detailed failure information
- **Acknowledge/Resolve**: Track alert lifecycle from detection to resolution
- **Alert History**: Complete audit trail of all incidents

### 4. Reporting
- **Email Reports**: Send dashboard snapshots to stakeholders
- **Filterable Reports**: Choose status types (green/yellow/red) and service filters

---

## Tech Stack

### Backend
| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 17+ | Core language |
| Spring Boot | 3.1.x | Application framework |
| Spring Security | 3.1.x | Authentication & Authorization |
| Spring Data JPA | 3.1.x | Database access |
| MySQL | 8.0+ | Primary database |
| JWT | 0.11.x | Token-based authentication |
| SendGrid/SMTP | - | Email delivery |

### Frontend
| Technology | Version | Purpose |
|------------|---------|---------|
| Angular | 17+ | Frontend framework |
| Chart.js | 4.4+ | Data visualization |
| RxJS | 7.8+ | Reactive programming |
| TypeScript | 5.0+ | Type safety |

---

## Prerequisites

- **Java 17** or higher
- **Node.js 18** or higher
- **MySQL 8.0** or higher
- **Maven** (or use included wrapper)
- **Angular CLI** (`npm install -g @angular/cli`)

---

## Installation

### Step 1: Clone the Repository

```bash
git clone https://github.com/LyndtBravos/dashboard-visuals-application.git
cd dashboard-visuals
```

### Step 2: Backend Setup

```bash
# Navigate to backend directory
cd backend

# Copy and configure application properties
cp application.properties.example application.properties

# Edit application.properties with your database credentials
# See Configuration section below

# Build the application
mvn clean install

# Run the application
mvn spring-boot:run
```

### Step 3: Frontend Setup

```bash
# Navigate to frontend directory
cd frontend

# Install dependencies
npm install

# Run development server
ng serve
```

### Step 4: Access the Application

- **Frontend**: http://localhost:4200
- **Backend API**: http://localhost:8080

---

## Database Setup

### Automatic Table Creation

On first run, the application will automatically create all required tables if `spring.jpa.hibernate.ddl-auto=update` is set.

### Manual Table Creation

If you prefer to create tables manually, run the following SQL script:

```sql
-- Users table
CREATE TABLE IF NOT EXISTS `user` (
    `id` INT PRIMARY KEY AUTO_INCREMENT,
    `Name` VARCHAR(100),
    `UserID` VARCHAR(4) UNIQUE,
    `Password` VARCHAR(20),
    `DT` DATETIME,
    `level` INT(3),
    `email` VARCHAR(50),
    `Statusid` INT NOT NULL DEFAULT 1,
    `Shift` INT DEFAULT 0,
    `RegionId` INT
);

-- Dashboard Configurations
CREATE TABLE IF NOT EXISTS dashboard_configs (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    service_type ENUM('Broadcast', 'Print', 'Online') NOT NULL, -- mention here the service you need, I chose these
    graph_type ENUM('text', 'text_minutes', 'bar', 'time_series', 'status_indicator') NOT NULL,
    query_text TEXT NOT NULL,
    threshold_warning DECIMAL(10,2),
    threshold_danger DECIMAL(10,2),
    alert_email VARCHAR(255),
    alert BOOLEAN DEFAULT FALSE,
    width ENUM('small', 'medium', 'large', 'full') DEFAULT 'medium',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE,
    updated_by INT,
    is_active BOOLEAN DEFAULT TRUE,
    x_axis_column VARCHAR(255),
    y_axis_columns TEXT,
    series_columns TEXT,
    aggregation_type ENUM('none', 'sum', 'avg', 'count', 'min', 'max'),
    time_interval VARCHAR(50),
    flow_order INT,
    FOREIGN KEY (updated_by) REFERENCES user(id)
);

-- Site Monitors
CREATE TABLE IF NOT EXISTS site_monitors (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    url VARCHAR(500) NOT NULL,
    expected_phrase VARCHAR(255),
    expected_phrase_missing BOOLEAN DEFAULT FALSE,
    retry_count INT DEFAULT 3,
    retry_interval_seconds INT DEFAULT 60,
    check_interval_minutes INT DEFAULT 5,
    timeout_seconds INT DEFAULT 30,
    follow_redirects BOOLEAN DEFAULT TRUE,
    expected_status_code INT DEFAULT 200,
    service_type ENUM('Broadcast', 'Print', 'Online') NOT NULL, -- mention here the service you need
    severity ENUM('critical', 'high', 'medium', 'low') DEFAULT 'medium',
    business_hours_only BOOLEAN DEFAULT FALSE,
    business_hours_start TIME DEFAULT '09:00',
    business_hours_end TIME DEFAULT '17:00',
    business_days VARCHAR(50) DEFAULT '1,2,3,4,5',
    is_active BOOLEAN DEFAULT TRUE,
    alert BOOLEAN DEFAULT TRUE,
    alert_email VARCHAR(255),
    last_check_time DATETIME,
    last_check_status ENUM('success', 'failed', 'error', 'skipped'),
    last_check_message TEXT,
    current_failure_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE,
    updated_by INT,
    FOREIGN KEY (updated_by) REFERENCES user(id)
);

-- API Monitors
CREATE TABLE IF NOT EXISTS api_monitors (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    url VARCHAR(500) NOT NULL,
    method ENUM('GET', 'POST', 'PUT', 'DELETE', 'PATCH', 'HEAD', 'OPTIONS') DEFAULT 'GET',
    request_headers_json TEXT,
    request_body TEXT,
    request_content_type VARCHAR(100) DEFAULT 'application/json',
    expected_status_code INT DEFAULT 200,
    expected_response_time_ms INT,
    expected_response_size_bytes INT,
    expected_response_contains VARCHAR(255),
    expected_json_path VARCHAR(255),
    expected_value VARCHAR(255),
    timeout_seconds INT DEFAULT 30,
    retry_count INT DEFAULT 3,
    retry_interval_seconds INT DEFAULT 60,
    check_interval_minutes INT DEFAULT 5,
    service_type ENUM('Broadcast', 'Print', 'Online') NOT NULL,
    severity ENUM('critical', 'high', 'medium', 'low') DEFAULT 'medium',
    business_hours_only BOOLEAN DEFAULT FALSE,
    business_hours_start TIME DEFAULT '09:00',
    business_hours_end TIME DEFAULT '17:00',
    business_days VARCHAR(50) DEFAULT '1,2,3,4,5',
    is_active BOOLEAN DEFAULT TRUE,
    alert BOOLEAN DEFAULT TRUE,
    alert_email VARCHAR(255),
    last_check_time DATETIME,
    last_check_status ENUM('success', 'failed', 'error', 'skipped'),
    last_check_message TEXT,
    last_response_time_ms INT,
    last_response_size_bytes INT,
    last_response_body TEXT,
    current_failure_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE,
    updated_by INT,
    FOREIGN KEY (updated_by) REFERENCES user(id)
);

-- Server Monitors
CREATE TABLE IF NOT EXISTS server_monitors (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    host VARCHAR(255) NOT NULL,
    port INT,
    protocol ENUM('icmp', 'tcp', 'http', 'https') DEFAULT 'icmp',
    timeout_seconds INT DEFAULT 5,
    retry_count INT DEFAULT 2,
    retry_interval_seconds INT DEFAULT 30,
    check_interval_minutes INT DEFAULT 5,
    service_type ENUM('Broadcast', 'Print', 'Online') NOT NULL,
    severity ENUM('critical', 'high', 'medium', 'low') DEFAULT 'medium',
    business_hours_only BOOLEAN DEFAULT FALSE,
    business_hours_start TIME DEFAULT '09:00',
    business_hours_end TIME DEFAULT '17:00',
    business_days VARCHAR(50) DEFAULT '1,2,3,4,5',
    is_active BOOLEAN DEFAULT TRUE,
    alert BOOLEAN DEFAULT TRUE,
    alert_email VARCHAR(255),
    last_check_time DATETIME,
    last_check_status ENUM('success', 'failed', 'error', 'pending') DEFAULT 'pending',
    last_check_message TEXT,
    response_time_ms INT,
    current_failure_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE,
    updated_by INT,
    FOREIGN KEY (updated_by) REFERENCES user(id)
);

-- Monitor History
CREATE TABLE IF NOT EXISTS monitor_history (
    id INT PRIMARY KEY AUTO_INCREMENT,
    monitor_type ENUM('site', 'api', 'server') NOT NULL,
    monitor_id INT NOT NULL,
    check_time DATETIME NOT NULL,
    status ENUM('failed', 'error') NOT NULL,
    response_time_ms INT,
    status_code INT,
    response_preview TEXT,
    error_message TEXT NOT NULL,
    failure_count_at_time INT,
    alert_created BOOLEAN DEFAULT FALSE
);

-- Monitor Alerts
CREATE TABLE IF NOT EXISTS monitor_alerts (
    id INT PRIMARY KEY AUTO_INCREMENT,
    monitor_type ENUM('site', 'api', 'server') NOT NULL,
    monitor_id INT NOT NULL,
    started_at DATETIME NOT NULL,
    last_occurrence DATETIME NOT NULL,
    occurrence_count INT DEFAULT 1,
    severity ENUM('critical', 'high', 'medium', 'low') NOT NULL,
    failure_reason TEXT NOT NULL,
    current_status ENUM('failing', 'resolved') DEFAULT 'failing',
    acknowledged BOOLEAN DEFAULT FALSE,
    acknowledged_by INT,
    acknowledged_at DATETIME,
    resolved_at DATETIME,
    FOREIGN KEY (acknowledged_by) REFERENCES user(id)
);
```

---

## Configuration

### Backend Configuration (`application.properties`)

Create a copy of `application.properties.example` and update the following:

```properties
# Database Configuration (REQUIRED)
spring.datasource.url=jdbc:mysql://localhost:3306/dashboard_db?useSSL=false&serverTimezone=UTC
spring.datasource.username=your_db_username
spring.datasource.password=your_db_password

# JWT Secret (REQUIRED - generate a secure key)
jwt.secret=your-super-secret-jwt-key-at-least-256-bits-long

# Email Configuration (REQUIRED for alerts)
spring.mail.host=smtp.sendgrid.net
spring.mail.port=587
spring.mail.username=apikey
spring.mail.password=your_sendgrid_api_key
spring.mail.from=noreply@yourdomain.com

# Dashboard Report Recipients
dashboard.report.recipients=admin@yourdomain.com,team@yourdomain.com

# For first-time setup, use this to create tables:
spring.jpa.hibernate.ddl-auto=update

# After tables are created, change to validate to prevent accidental changes:
# spring.jpa.hibernate.ddl-auto=validate
```

### Frontend Configuration

No additional configuration needed. The frontend communicates with the backend via relative paths.

---

## Running the Application

### Development Mode

```bash
# Terminal 1 - Backend
cd backend
mvn spring-boot:run

# Terminal 2 - Frontend
cd frontend
ng serve
```

### Production Build

```bash
# Build frontend
cd frontend
ng build --configuration=production

# Copy dist to backend static folder
cp -r dist/dashboard-visuals/* ../backend/src/main/resources/static/

# Build backend JAR
cd ../backend
mvn clean package
java -jar target/dashboard-1.0.0.jar
```

---

## API Documentation

### Main Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/login` | Authenticate user |
| POST | `/api/auth/register` | Register new user |
| GET | `/api/dashboards/summary` | Get service health summary |
| GET | `/api/dashboards/service/{type}` | Get visuals by service |
| GET | `/api/monitors/sites` | Get all site monitors |
| GET | `/api/monitors/apis` | Get all API monitors |
| GET | `/api/monitors/servers` | Get all server monitors |
| GET | `/api/alerts` | Get all alerts |
| GET | `/api/alerts/active` | Get active alerts |
| POST | `/api/email/send-report` | Send dashboard report |
| GET | `/api/health/ping` | Health check |

### Authentication

All endpoints (except `/api/auth/*` and `/api/health/*`) require a JWT token:

```
Authorization: Bearer <your-jwt-token>
```

---

## Testing

### Backend Tests

```bash
cd backend
mvn test
```

### Frontend Tests

```bash
cd frontend
ng test
```

### Sample API Test (using curl)

```bash
# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"userId":"ADMIN","password":"admin123"}'

# Get dashboard summary (replace token)
curl -X GET http://localhost:8080/api/dashboards/summary \
  -H "Authorization: Bearer YOUR_TOKEN"
```

---

## Troubleshooting

### Common Issues

#### 1. "Access denied for user"
**Solution**: Check database credentials in `application.properties`

#### 2. "Connection refused" on port 8080
**Solution**: Another application is using port 8080. Change `server.port` in `application.properties`

#### 3. Frontend can't connect to backend
**Solution**: Ensure backend is running on port 8080 and frontend on port 4200

#### 4. Email not sending
**Solution**: Verify SMTP credentials in `application.properties`

#### 5. JWT token expired
**Solution**: Re-login to get a new token

#### 6. Tables not created
**Solution**: Set `spring.jpa.hibernate.ddl-auto=update` for first run, then change to `validate`

### Logs Location

- **Backend**: Console output
- **Frontend**: Browser DevTools Console (F12)

---

## Future Improvements

The following features are planned for future releases:

- **Flow Arrows RTL Fix** - Improve arrow drawing for right-to-left rows
- **Multi-threading for Dashboard Summary** - Parallel queries for faster loading
- **Redis Caching** - Cache query results for improved performance
- **Dark/Light Theme Toggle** - User preference theming
- **PDF Export** - Export dashboard data as PDF
- **User Roles & Permissions** - Admin, Viewer, Editor roles
- **Alert Grouping** - Group related alerts for better organization
- **SLA Tracking** - Track uptime percentages and service levels

---

## Project Structure

```
dashboard-visuals/
├── backend/
│   ├── src/main/java/com/mediahost/dashboard/
│   │   ├── config/          # Configuration classes
│   │   ├── controller/      # REST controllers
│   │   ├── service/         # Business logic
│   │   ├── repository/      # Data access
│   │   ├── model/           # Entities and DTOs
│   │   └── security/        # JWT security
│   └── src/main/resources/
│       ├── application.properties
│       └── templates/       # Email templates
├── frontend/
│   └── src/app/
│       ├── core/            # Services, guards, interceptors
│       ├── features/        # Components by feature
│       ├── layouts/         # Layout components
│       └── shared/          # Reusable components
└── README.md
```

---

## License

Private - All rights reserved

---

## Support

For issues or questions:
- **Documentation**: This README
- **Email**: lindtbravos@gmail.com

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 2026-04-16 | Initial release |

---
