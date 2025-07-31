# 🗄️ Database Setup Guide

**Fabric Platform Database Configuration Guide**

This guide provides comprehensive instructions for setting up and configuring databases for the Fabric Platform across different environments and database types.

## 📋 Table of Contents

- [Overview](#overview)
- [Oracle Database Setup](#oracle-database-setup)
- [PostgreSQL Setup](#postgresql-setup)
- [MySQL Setup](#mysql-setup)
- [MongoDB Setup](#mongodb-setup)
- [Redis Setup](#redis-setup)
- [AWS RDS Setup](#aws-rds-setup)
- [Environment Configuration](#environment-configuration)
- [Schema Management](#schema-management)
- [Performance Tuning](#performance-tuning)
- [Troubleshooting](#troubleshooting)

## 🎯 Overview

The Fabric Platform supports multiple database technologies for different use cases:

- **Oracle**: Primary production database (default)
- **PostgreSQL**: Alternative relational database
- **MySQL**: Alternative relational database
- **MongoDB**: Document storage for configuration data
- **Redis**: Caching and session management
- **AWS RDS**: Cloud-managed databases

## 🔷 Oracle Database Setup

### Prerequisites

- Oracle Database 19c or higher
- Oracle SQL*Plus client
- Oracle JDBC driver (included in Maven dependencies)

### 1. Create Database User

```sql
-- Connect as SYSDBA
sqlplus sys/password@localhost:1521/XE as sysdba

-- Create tablespace
CREATE TABLESPACE fabric_data
  DATAFILE '/opt/oracle/oradata/XE/fabric_data.dbf' 
  SIZE 1G AUTOEXTEND ON;

-- Create user
CREATE USER fabric_user IDENTIFIED BY secure_password
  DEFAULT TABLESPACE fabric_data
  TEMPORARY TABLESPACE temp;

-- Grant permissions
GRANT CONNECT, RESOURCE TO fabric_user;
GRANT CREATE VIEW, CREATE SEQUENCE TO fabric_user;
GRANT UNLIMITED TABLESPACE TO fabric_user;

-- For batch processing
GRANT CREATE JOB TO fabric_user;
GRANT SELECT ANY DICTIONARY TO fabric_user;
```

### 2. Create Schema Objects

```bash
# Run DDL scripts
cd fabric-core/fabric-api/src/main/resources/sql/ddl
sqlplus fabric_user/secure_password@localhost:1521/XE @authentication-ddl.sql
sqlplus fabric_user/secure_password@localhost:1521/XE @configuration-ddl.sql
sqlplus fabric_user/secure_password@localhost:1521/XE @templates-ddl.sql
```

### 3. Environment Configuration

```bash
# Development
export DB_URL="jdbc:oracle:thin:@localhost:1521:XE"
export DB_USERNAME="fabric_user"
export DB_PASSWORD="secure_password"
export SPRING_PROFILES_ACTIVE="dev"

# Production
export DB_URL="jdbc:oracle:thin:@prod-oracle:1521:PROD"
export DB_USERNAME="fabric_prod"
export DB_PASSWORD="encrypted_password"
export SPRING_PROFILES_ACTIVE="prod"
```

### 4. Connection Pool Tuning

```yaml
# application-prod.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 50
      minimum-idle: 10
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      data-source-properties:
        cachePrepStmts: true
        prepStmtCacheSize: 500
        prepStmtCacheSqlLimit: 2048
```

## 🐘 PostgreSQL Setup

### Prerequisites

- PostgreSQL 13 or higher
- psql client
- PostgreSQL JDBC driver

### 1. Create Database and User

```sql
-- Connect as superuser
psql -h localhost -U postgres

-- Create database
CREATE DATABASE fabric_platform;

-- Create user
CREATE USER fabric_user WITH PASSWORD 'secure_password';

-- Grant permissions
GRANT ALL PRIVILEGES ON DATABASE fabric_platform TO fabric_user;

-- Connect to the database
\c fabric_platform

-- Grant schema permissions
GRANT ALL ON SCHEMA public TO fabric_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO fabric_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO fabric_user;
```

### 2. Environment Configuration

```bash
# PostgreSQL with AWS profile
export SPRING_PROFILES_ACTIVE="aws,postgresql"
export AWS_RDS_URL="jdbc:postgresql://localhost:5432/fabric_platform"
export AWS_RDS_USERNAME="fabric_user"
export AWS_RDS_PASSWORD="secure_password"
```

### 3. Schema Creation

```sql
-- Run PostgreSQL-specific DDL
-- Note: Adapt Oracle DDL scripts for PostgreSQL syntax
CREATE TABLE fabric_users (
    user_id VARCHAR(50) PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_fabric_users_username ON fabric_users(username);
```

## 🐬 MySQL Setup

### Prerequisites

- MySQL 8.0 or higher
- mysql client
- MySQL JDBC driver

### 1. Create Database and User

```sql
-- Connect as root
mysql -h localhost -u root -p

-- Create database
CREATE DATABASE fabric_platform CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Create user
CREATE USER 'fabric_user'@'%' IDENTIFIED BY 'secure_password';

-- Grant permissions
GRANT ALL PRIVILEGES ON fabric_platform.* TO 'fabric_user'@'%';
FLUSH PRIVILEGES;
```

### 2. Environment Configuration

```bash
# MySQL with AWS profile
export SPRING_PROFILES_ACTIVE="aws,mysql"
export AWS_RDS_MYSQL_URL="jdbc:mysql://localhost:3306/fabric_platform?useSSL=true&serverTimezone=UTC"
export AWS_RDS_MYSQL_USERNAME="fabric_user"
export AWS_RDS_MYSQL_PASSWORD="secure_password"
```

### 3. Performance Tuning

```sql
-- MySQL configuration (my.cnf)
[mysqld]
innodb_buffer_pool_size = 2G
innodb_log_file_size = 512M
innodb_flush_log_at_trx_commit = 2
max_connections = 200
query_cache_size = 128M
```

## 🍃 MongoDB Setup

### Prerequisites

- MongoDB 5.0 or higher
- MongoDB shell (mongosh)
- Spring Data MongoDB

### 1. Install and Start MongoDB

```bash
# Ubuntu/Debian
sudo apt-get install -y mongodb-org

# Start MongoDB
sudo systemctl start mongod
sudo systemctl enable mongod

# macOS with Homebrew
brew install mongodb/brew/mongodb-community
brew services start mongodb/brew/mongodb-community
```

### 2. Create Database and User

```javascript
// Connect to MongoDB
mongosh

// Switch to fabric database
use fabric_platform

// Create user
db.createUser({
  user: "fabric_user",
  pwd: "secure_password",
  roles: [
    { role: "readWrite", db: "fabric_platform" },
    { role: "dbAdmin", db: "fabric_platform" }
  ]
})

// Create collections with validation
db.createCollection("fabric_configurations", {
  validator: {
    $jsonSchema: {
      bsonType: "object",
      required: ["configId", "name", "createdAt"],
      properties: {
        configId: { bsonType: "string" },
        name: { bsonType: "string" },
        createdAt: { bsonType: "date" }
      }
    }
  }
})

// Create indexes
db.fabric_configurations.createIndex({ "configId": 1 }, { unique: true })
db.fabric_configurations.createIndex({ "name": 1 })
db.fabric_configurations.createIndex({ "createdAt": -1 })
```

### 3. Environment Configuration

```bash
# MongoDB standalone
export SPRING_PROFILES_ACTIVE="mongodb"
export MONGODB_URI="mongodb://fabric_user:secure_password@localhost:27017/fabric_platform"

# MongoDB Atlas (Cloud)
export SPRING_PROFILES_ACTIVE="mongodb,atlas"
export MONGODB_ATLAS_URI="mongodb+srv://fabric_user:password@cluster.mongodb.net/fabric_platform"
```

## 🔴 Redis Setup

### Prerequisites

- Redis 6.0 or higher
- Redis CLI

### 1. Install and Configure Redis

```bash
# Ubuntu/Debian
sudo apt-get install redis-server

# macOS with Homebrew
brew install redis

# Start Redis
sudo systemctl start redis-server
sudo systemctl enable redis-server

# macOS
brew services start redis
```

### 2. Redis Configuration

```bash
# Edit redis.conf
sudo nano /etc/redis/redis.conf

# Key settings
maxmemory 2gb
maxmemory-policy allkeys-lru
save 900 1
save 300 10
save 60 10000

# Security (if needed)
requirepass your_redis_password
```

### 3. Environment Configuration

```bash
# Redis local
export SPRING_PROFILES_ACTIVE="dev,redis"
export REDIS_HOST="localhost"
export REDIS_PORT="6379"
export REDIS_PASSWORD=""

# Redis with authentication
export REDIS_PASSWORD="secure_redis_password"

# Redis cluster
export SPRING_PROFILES_ACTIVE="redis,cluster"
export REDIS_CLUSTER_NODES="localhost:7000,localhost:7001,localhost:7002"
```

## ☁️ AWS RDS Setup

### Prerequisites

- AWS CLI configured
- Appropriate IAM permissions
- VPC and security groups configured

### 1. Create RDS Instance

```bash
# Create Oracle RDS instance
aws rds create-db-instance \
  --db-instance-identifier fabric-prod-oracle \
  --db-instance-class db.r5.xlarge \
  --engine oracle-ee \
  --engine-version 19.0.0.0.ru-2023-01.rur-2023-01.r1 \
  --allocated-storage 100 \
  --storage-type gp2 \
  --storage-encrypted \
  --master-username fabric_admin \
  --master-user-password SecurePassword123 \
  --vpc-security-group-ids sg-12345678 \
  --db-subnet-group-name fabric-subnet-group \
  --backup-retention-period 7 \
  --multi-az \
  --publicly-accessible false

# Create PostgreSQL RDS instance
aws rds create-db-instance \
  --db-instance-identifier fabric-prod-postgresql \
  --db-instance-class db.r5.large \
  --engine postgres \
  --engine-version 13.7 \
  --allocated-storage 100 \
  --storage-type gp2 \
  --storage-encrypted \
  --master-username fabric_admin \
  --master-user-password SecurePassword123 \
  --vpc-security-group-ids sg-12345678 \
  --db-subnet-group-name fabric-subnet-group \
  --backup-retention-period 7 \
  --multi-az
```

### 2. Environment Configuration

```bash
# RDS Oracle
export SPRING_PROFILES_ACTIVE="staging"
export DB_URL="jdbc:oracle:thin:@fabric-prod-oracle.cluster-xyz.us-east-1.rds.amazonaws.com:1521:ORCL"
export DB_USERNAME="fabric_admin"
export DB_PASSWORD="SecurePassword123"

# RDS PostgreSQL
export SPRING_PROFILES_ACTIVE="aws,postgresql"
export AWS_RDS_URL="jdbc:postgresql://fabric-prod-postgresql.cluster-xyz.us-east-1.rds.amazonaws.com:5432/fabric"
export AWS_RDS_USERNAME="fabric_admin"
export AWS_RDS_PASSWORD="SecurePassword123"
```

### 3. Aurora Cluster Setup

```bash
# Create Aurora PostgreSQL cluster
aws rds create-db-cluster \
  --db-cluster-identifier fabric-prod-aurora \
  --engine aurora-postgresql \
  --engine-version 13.7 \
  --master-username fabric_admin \
  --master-user-password SecurePassword123 \
  --vpc-security-group-ids sg-12345678 \
  --db-subnet-group-name fabric-subnet-group \
  --backup-retention-period 7 \
  --storage-encrypted

# Create Aurora instances
aws rds create-db-instance \
  --db-instance-identifier fabric-prod-aurora-writer \
  --db-instance-class db.r5.large \
  --engine aurora-postgresql \
  --db-cluster-identifier fabric-prod-aurora
```

## 🔧 Environment Configuration

### Development Environment

```bash
# .env.development
DB_URL=jdbc:oracle:thin:@localhost:1521:XE
DB_USERNAME=fabric_dev
DB_PASSWORD=fabric_dev_pass
REDIS_HOST=localhost
REDIS_PORT=6379
SPRING_PROFILES_ACTIVE=dev
```

### Staging Environment

```bash
# .env.staging
DB_URL=jdbc:oracle:thin:@staging-oracle.company.com:1521:STAGING
DB_USERNAME=fabric_staging
DB_PASSWORD=${DB_PASSWORD_ENCRYPTED}
REDIS_HOST=staging-redis.company.com
REDIS_PORT=6379
SPRING_PROFILES_ACTIVE=staging
```

### Production Environment

```bash
# .env.production
DB_URL=jdbc:oracle:thin:@prod-oracle.company.com:1521:PROD
DB_USERNAME=fabric_prod
DB_PASSWORD=${DB_PASSWORD_ENCRYPTED}
REDIS_CLUSTER_NODES=prod-redis-001:6379,prod-redis-002:6379,prod-redis-003:6379
SPRING_PROFILES_ACTIVE=prod
```

## 📊 Schema Management

### Flyway Migration

```bash
# Configure Flyway
cd fabric-core/fabric-api

# Create migration scripts
mkdir -p src/main/resources/db/migration
```

```sql
-- V1__Initial_schema.sql
CREATE TABLE fabric_users (
    user_id VARCHAR2(50) PRIMARY KEY,
    username VARCHAR2(100) NOT NULL UNIQUE,
    email VARCHAR2(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- V2__Add_authentication_tables.sql
CREATE TABLE fabric_roles (
    role_id VARCHAR2(50) PRIMARY KEY,
    role_name VARCHAR2(100) NOT NULL UNIQUE,
    description VARCHAR2(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### Running Migrations

```bash
# Run migrations
mvn flyway:migrate

# Check migration status
mvn flyway:info

# Rollback (if needed)
mvn flyway:undo
```

## 🚀 Performance Tuning

### Oracle Performance

```sql
-- Monitor active sessions
SELECT username, status, count(*) 
FROM v$session 
GROUP BY username, status;

-- Check table statistics
SELECT table_name, num_rows, last_analyzed 
FROM user_tables 
WHERE table_name LIKE 'FABRIC%';

-- Update statistics
EXEC DBMS_STATS.GATHER_SCHEMA_STATS('FABRIC_USER');
```

### Connection Pool Optimization

```yaml
# High-load configuration
spring:
  datasource:
    hikari:
      maximum-pool-size: 50
      minimum-idle: 10
      connection-timeout: 30000
      idle-timeout: 300000
      max-lifetime: 1800000
      leak-detection-threshold: 60000
```

### Redis Performance

```bash
# Monitor Redis
redis-cli INFO memory
redis-cli INFO stats
redis-cli MONITOR

# Performance tuning
redis-cli CONFIG SET maxmemory-policy allkeys-lru
redis-cli CONFIG SET save "900 1 300 10 60 10000"
```

## 🔍 Troubleshooting

### Common Oracle Issues

```bash
# Check Oracle listener
lsnrctl status

# Check database status
sqlplus / as sysdba
SELECT name, open_mode FROM v$database;

# Check tablespace usage
SELECT tablespace_name, 
       ROUND(bytes/1024/1024,2) as total_mb,
       ROUND(maxbytes/1024/1024,2) as max_mb
FROM dba_data_files;
```

### Connection Issues

```bash
# Test database connectivity
telnet localhost 1521

# Test JDBC connection
java -cp "ojdbc8.jar" oracle.jdbc.OracleDriver \
  "jdbc:oracle:thin:@localhost:1521:XE" \
  username password

# Check application logs
tail -f logs/fabric-platform.log | grep -i "connection\|database"
```

### Performance Issues

```sql
-- Check slow queries (Oracle)
SELECT sql_text, executions, elapsed_time
FROM v$sql 
WHERE elapsed_time > 1000000
ORDER BY elapsed_time DESC;

-- Check locks
SELECT s.sid, s.username, s.program, s.machine,
       l.type, l.mode_held, l.mode_requested
FROM v$session s, v$lock l
WHERE s.sid = l.sid
  AND l.block = 1;
```

### Memory Issues

```bash
# Check Java heap usage
jstat -gc -t $(jps | grep fabric | cut -d' ' -f1) 5s

# Monitor database memory
# Oracle
SELECT * FROM v$sga;
SELECT * FROM v$pga_target_advice;

# PostgreSQL
SELECT * FROM pg_stat_database;
```

## 📝 Best Practices

### Security

1. **Use encrypted passwords** in production
2. **Limit database user privileges** to minimum required
3. **Enable SSL/TLS** for database connections
4. **Regular security updates** for database software
5. **Audit database access** and changes

### Backup and Recovery

```bash
# Oracle backup
expdp fabric_user/password DIRECTORY=dpump_dir DUMPFILE=fabric_backup.dmp

# PostgreSQL backup
pg_dump -h localhost -U fabric_user fabric_platform > fabric_backup.sql

# MySQL backup
mysqldump -h localhost -u fabric_user -p fabric_platform > fabric_backup.sql
```

### Monitoring

```bash
# Set up database monitoring
# Oracle - AWR reports
# PostgreSQL - pg_stat extensions
# MySQL - Performance Schema
# MongoDB - Profiler
# Redis - INFO command monitoring
```

---

For additional support, consult the main [README.md](./README.md) or contact the Fabric Platform team.

**© 2025 Truist Financial Corporation. All rights reserved.**