# Fabric Platform - Local Development Setup Guide

Complete guide for setting up the Fabric Platform in your local development environment using Eclipse for backend and VS Code for frontend.

## Table of Contents
1. [Prerequisites](#prerequisites)
2. [Database Setup](#database-setup)
3. [Backend Setup (Eclipse)](#backend-setup-eclipse)
4. [Frontend Setup (VS Code)](#frontend-setup-vs-code)
5. [Running the Applications](#running-the-applications)
6. [Verification](#verification)
7. [Troubleshooting](#troubleshooting)

---

## Prerequisites

### Required Software

#### Backend Development
- **Java 17 or higher** - [Download OpenJDK](https://adoptium.net/)
  ```bash
  java -version  # Should show version 17 or higher
  ```

- **Maven 3.8+** - [Download Maven](https://maven.apache.org/download.cgi)
  ```bash
  mvn -version  # Should show version 3.8 or higher
  ```

- **Eclipse IDE for Enterprise Java Developers** - [Download Eclipse](https://www.eclipse.org/downloads/)
  - Version: 2023-06 or later
  - Includes Spring Tools, Maven integration, and Git support

- **Oracle Database 19c or higher** (or Docker container)
  - JDBC URL: `jdbc:oracle:thin:@localhost:1521/ORCLPDB1`
  - Schema: CM3INT
  - User: cm3int / MySecurePass123

#### Frontend Development
- **Node.js 18.x or higher** - [Download Node.js](https://nodejs.org/)
  ```bash
  node -v  # Should show v18.x or higher
  npm -v   # Should show v9.x or higher
  ```

- **Visual Studio Code** - [Download VS Code](https://code.visualstudio.com/)

- **VS Code Extensions** (Recommended):
  - ESLint
  - Prettier - Code formatter
  - ES7+ React/Redux/React-Native snippets
  - TypeScript Vue Plugin (Volar)
  - Path Intellisense
  - Auto Rename Tag
  - GitLens

---

## Database Setup

### Option 1: Using Docker (Recommended)

```bash
# Pull Oracle Database image
docker pull container-registry.oracle.com/database/express:latest

# Run Oracle Database container
docker run -d \
  --name oracle-db \
  -p 1521:1521 \
  -e ORACLE_PWD=MySecurePass123 \
  container-registry.oracle.com/database/express:latest

# Wait for database to start (check logs)
docker logs -f oracle-db
```

### Option 2: Local Oracle Installation

1. Install Oracle Database 19c from [Oracle Downloads](https://www.oracle.com/database/technologies/oracle-database-software-downloads.html)
2. Create pluggable database: ORCLPDB1
3. Create user and grant privileges:

```sql
-- Connect as SYSDBA
sqlplus sys/password@localhost:1521/ORCLPDB1 as sysdba

-- Create user
CREATE USER cm3int IDENTIFIED BY MySecurePass123;

-- Grant privileges
GRANT CONNECT, RESOURCE, DBA TO cm3int;
GRANT CREATE SESSION TO cm3int;
GRANT CREATE TABLE TO cm3int;
GRANT CREATE SEQUENCE TO cm3int;
GRANT UNLIMITED TABLESPACE TO cm3int;

-- Verify connection
CONNECT cm3int/MySecurePass123@localhost:1521/ORCLPDB1
```

### Create Required Directories

```bash
# For production paths (if you have permissions)
sudo mkdir -p /data/{output,input,archive,error}/{hr,encore,payroll}
sudo chmod -R 777 /data

# For local macOS development (recommended)
mkdir -p /tmp/data/{output,input,archive}/hr
mkdir -p /tmp/data/{output,input,archive,error}/encore
mkdir -p /tmp/data/{output,input,archive}/payroll
```

---

## Backend Setup (Eclipse)

### Step 1: Clone Repository

```bash
# Clone the repository
git clone <your-repository-url> fabric-platform-new
cd fabric-platform-new
```

### Step 2: Import Project into Eclipse

1. **Open Eclipse**
2. **Import Maven Project**:
   - File → Import → Maven → Existing Maven Projects
   - Browse to: `/path/to/fabric-platform-new/fabric-core`
   - Select all modules:
     - fabric-core (parent)
     - fabric-api
     - fabric-batch
     - fabric-utils
     - fabric-data-loader
   - Click "Finish"

3. **Wait for Maven Dependencies**:
   - Eclipse will automatically download dependencies
   - This may take 5-10 minutes on first import
   - Check progress in "Progress" view (Window → Show View → Other → General → Progress)

### Step 3: Configure Database Connection

1. **Navigate to**: `fabric-api/src/main/resources/application-local.properties`

2. **Update database credentials**:
   ```properties
   # Database Configuration
   spring.datasource.url=jdbc:oracle:thin:@localhost:1521/ORCLPDB1
   spring.datasource.username=cm3int
   spring.datasource.password=MySecurePass123
   spring.datasource.driver-class-name=oracle.jdbc.OracleDriver

   # HikariCP Connection Pool
   spring.datasource.hikari.maximum-pool-size=10
   spring.datasource.hikari.minimum-idle=5
   spring.datasource.hikari.connection-timeout=30000

   # Server Configuration
   server.port=8080

   # Local Development Settings
   spring.profiles.active=local
   logging.level.com.truist.batch=DEBUG
   ```

### Step 4: Run Liquibase Migrations

**Option A: Using Eclipse Run Configuration**

1. Right-click on `fabric-api` project → Run As → Maven build...
2. **Goals**: `liquibase:update`
3. **Profiles**: (leave empty or use `local`)
4. Click "Run"

**Option B: Using Terminal**

```bash
cd fabric-platform-new/fabric-core/fabric-api

# Check migration status
mvn liquibase:status

# Apply all pending migrations
mvn liquibase:update

# Verify tables were created
mvn liquibase:status
```

**Expected Output**:
```
[INFO] 15 changesets have been applied to CM3INT@jdbc:oracle:thin:@localhost:1521/ORCLPDB1
[INFO] Liquibase: Update has been successful.
```

### Step 5: Build the Project

1. **Right-click** on `fabric-core` (parent project)
2. **Run As** → Maven install
3. **Or use terminal**:
   ```bash
   cd fabric-platform-new/fabric-core
   mvn clean install -DskipTests
   ```

**Expected Output**:
```
[INFO] ------------------------------------------------------------------------
[INFO] Reactor Summary for fabric-core 0.0.1-SNAPSHOT:
[INFO]
[INFO] fabric-core ........................................ SUCCESS [  0.123 s]
[INFO] fabric-utils ....................................... SUCCESS [  2.456 s]
[INFO] fabric-batch ....................................... SUCCESS [  3.789 s]
[INFO] fabric-api ......................................... SUCCESS [  5.234 s]
[INFO] fabric-data-loader ................................. SUCCESS [  1.567 s]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

### Step 6: Create Run Configuration

1. **Right-click** on `fabric-api` project → Run As → Run Configurations...
2. **Create new Spring Boot App** configuration:
   - **Name**: Fabric API - Local
   - **Project**: fabric-api
   - **Main Type**: `com.truist.batch.FabricApiApplication`
   - **Profile**: local
   - **VM Arguments**:
     ```
     -Dspring.profiles.active=local
     -Dmaven.test.skip=true
     ```
3. Click "Apply" and "Run"

### Step 7: Verify Backend Startup

Check Eclipse Console for:
```
2025-11-23 10:00:00 [main] INFO  c.t.b.FabricApiApplication - Starting FabricApiApplication
2025-11-23 10:00:05 [main] INFO  c.t.b.config.DatabasePropertySource - Successfully loaded 12 properties from database
2025-11-23 10:00:10 [main] INFO  o.s.b.w.e.tomcat.TomcatWebServer - Tomcat started on port(s): 8080 (http)
2025-11-23 10:00:10 [main] INFO  c.t.b.FabricApiApplication - Started FabricApiApplication in 10.123 seconds
```

---

## Frontend Setup (VS Code)

### Step 1: Open Project in VS Code

```bash
cd fabric-platform-new/fabric-ui
code .
```

### Step 2: Install Dependencies

Open integrated terminal in VS Code (Ctrl+` or Cmd+`) and run:

```bash
# Install all npm dependencies
npm install

# This will install:
# - React 18.2
# - TypeScript 4.9
# - Material-UI 5.x
# - React Router 6.x
# - Axios
# - And all other dependencies from package.json
```

**Expected Output**:
```
added 1523 packages, and audited 1524 packages in 45s

234 packages are looking for funding
  run `npm fund` for details

found 0 vulnerabilities
```

### Step 3: Configure Environment Variables

1. **Create `.env.local` file** in `fabric-ui/` directory:

```bash
# .env.local
REACT_APP_API_BASE_URL=http://localhost:8080
REACT_APP_ENVIRONMENT=development
REACT_APP_LOG_LEVEL=debug
```

2. **Verify proxy configuration** in `package.json`:
```json
{
  "proxy": "http://localhost:8080"
}
```

### Step 4: Install VS Code Extensions (if not already installed)

1. Open Extensions (Ctrl+Shift+X / Cmd+Shift+X)
2. Search and install:
   - **ESLint** (dbaeumer.vscode-eslint)
   - **Prettier** (esbenp.prettier-vscode)
   - **ES7+ React** (dsznajder.es7-react-js-snippets)
   - **TypeScript Vue Plugin** (Vue.volar)

### Step 5: Configure VS Code Settings

Create `.vscode/settings.json` in `fabric-ui/`:

```json
{
  "editor.formatOnSave": true,
  "editor.defaultFormatter": "esbenp.prettier-vscode",
  "editor.codeActionsOnSave": {
    "source.fixAll.eslint": true
  },
  "typescript.tsdk": "node_modules/typescript/lib",
  "typescript.enablePromptUseWorkspaceTsdk": true,
  "files.associations": {
    "*.tsx": "typescriptreact",
    "*.ts": "typescript"
  }
}
```

### Step 6: Verify TypeScript Configuration

Check `tsconfig.json`:
```json
{
  "compilerOptions": {
    "target": "es5",
    "lib": ["dom", "dom.iterable", "esnext"],
    "allowJs": true,
    "skipLibCheck": true,
    "esModuleInterop": true,
    "allowSyntheticDefaultImports": true,
    "strict": true,
    "forceConsistentCasingInFileNames": true,
    "noFallthroughCasesInSwitch": true,
    "module": "esnext",
    "moduleResolution": "node",
    "resolveJsonModule": true,
    "isolatedModules": true,
    "noEmit": true,
    "jsx": "react-jsx"
  },
  "include": ["src"]
}
```

---

## Running the Applications

### Terminal 1: Backend (Eclipse or Terminal)

**Option A: Using Eclipse**
1. Select "Fabric API - Local" run configuration
2. Click green "Run" button (or F11)

**Option B: Using Terminal**
```bash
cd fabric-platform-new/fabric-core/fabric-api
mvn spring-boot:run -Dspring-boot.run.profiles=local -DskipTests
```

**Backend will be available at**: http://localhost:8080

### Terminal 2: Frontend (VS Code Terminal)

```bash
cd fabric-platform-new/fabric-ui
npm start
```

**Frontend will be available at**: http://localhost:3000

The browser should automatically open to http://localhost:3000

---

## Verification

### 1. Check Backend Health

**Browser**: http://localhost:8080/actuator/health

**Expected Response**:
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "Oracle",
        "validationQuery": "isValid()"
      }
    },
    "diskSpace": {
      "status": "UP"
    }
  }
}
```

**cURL**:
```bash
curl http://localhost:8080/actuator/health
```

### 2. Check Swagger API Documentation

**Browser**: http://localhost:8080/swagger-ui.html

You should see the complete API documentation with all endpoints.

### 3. Test Database Connection

```bash
# Check SOURCE_CONFIG table
curl http://localhost:8080/api/v2/source-config/HR
```

**Expected Response**:
```json
{
  "sourceCode": "HR",
  "configurations": [
    {
      "configKey": "batch.defaults.outputBasePath",
      "configValue": "/tmp/data/output"
    }
  ]
}
```

### 4. Check Frontend

**Browser**: http://localhost:3000

You should see:
- Login page (if not authenticated)
- Dashboard with navigation (after login)
- No console errors in browser DevTools (F12)

### 5. Test Frontend-Backend Integration

```bash
# Open browser DevTools (F12) → Network tab
# Navigate to: http://localhost:3000/job-config
# You should see API calls to http://localhost:8080/api/v2/manual-job-config
```

---

## Troubleshooting

### Backend Issues

#### Issue 1: Port 8080 Already in Use

**Error**:
```
Web server failed to start. Port 8080 was already in use.
```

**Solution**:
```bash
# Find and kill process on port 8080
lsof -t -i :8080 | xargs kill -9

# Or use a different port in application-local.properties
server.port=8081
```

#### Issue 2: Database Connection Failed

**Error**:
```
Could not get JDBC Connection; nested exception is java.sql.SQLRecoverableException: IO Error: The Network Adapter could not establish the connection
```

**Solution**:
1. Verify Oracle Database is running:
   ```bash
   # For Docker
   docker ps | grep oracle-db

   # For local install
   lsnrctl status
   ```

2. Test connection manually:
   ```bash
   sqlplus cm3int/MySecurePass123@localhost:1521/ORCLPDB1
   ```

3. Check firewall settings allow port 1521

#### Issue 3: Liquibase Fails

**Error**:
```
liquibase.exception.LiquibaseException: liquibase.exception.DatabaseException: ORA-00942: table or view does not exist
```

**Solution**:
```bash
# Clear Liquibase lock
mvn liquibase:clearCheckSums

# Re-run migration
mvn liquibase:update

# If still fails, check database user permissions
```

#### Issue 4: Maven Build Fails

**Error**:
```
[ERROR] Failed to execute goal on project fabric-api: Could not resolve dependencies
```

**Solution**:
```bash
# Force update dependencies
mvn clean install -U

# Clear Maven cache
rm -rf ~/.m2/repository/com/truist
mvn clean install
```

#### Issue 5: Class Not Found Exception

**Error**:
```
java.lang.ClassNotFoundException: oracle.jdbc.OracleDriver
```

**Solution**:
1. Check `pom.xml` includes Oracle JDBC driver:
   ```xml
   <dependency>
       <groupId>com.oracle.database.jdbc</groupId>
       <artifactId>ojdbc11</artifactId>
       <version>23.6.0.24.10</version>
   </dependency>
   ```

2. Refresh Maven dependencies in Eclipse:
   - Right-click project → Maven → Update Project

### Frontend Issues

#### Issue 1: Port 3000 Already in Use

**Error**:
```
Something is already running on port 3000.
```

**Solution**:
```bash
# Find and kill process on port 3000
lsof -t -i :3000 | xargs kill -9

# Or start on different port
PORT=3001 npm start
```

#### Issue 2: npm install Fails

**Error**:
```
npm ERR! code ERESOLVE
npm ERR! ERESOLVE could not resolve
```

**Solution**:
```bash
# Clear npm cache
npm cache clean --force

# Delete node_modules and package-lock.json
rm -rf node_modules package-lock.json

# Reinstall
npm install

# If still fails, use legacy peer deps
npm install --legacy-peer-deps
```

#### Issue 3: Module Not Found

**Error**:
```
Module not found: Error: Can't resolve '@mui/material'
```

**Solution**:
```bash
# Reinstall Material-UI
npm install @mui/material @emotion/react @emotion/styled

# Restart development server
npm start
```

#### Issue 4: CORS Errors

**Error in Browser Console**:
```
Access to XMLHttpRequest at 'http://localhost:8080/api/v2/...' from origin 'http://localhost:3000' has been blocked by CORS policy
```

**Solution**:
1. Verify proxy in `package.json`:
   ```json
   {
     "proxy": "http://localhost:8080"
   }
   ```

2. Update API calls to use relative URLs:
   ```typescript
   // Correct
   axios.get('/api/v2/manual-job-config')

   // Incorrect (will cause CORS)
   axios.get('http://localhost:8080/api/v2/manual-job-config')
   ```

3. Check backend CORS configuration in `CorsConfig.java`

#### Issue 5: TypeScript Compilation Errors

**Error**:
```
TypeScript error in /src/components/JobConfigurationForm.tsx(45,18):
Property 'xyz' does not exist on type 'JobConfig'.  TS2339
```

**Solution**:
1. Check type definitions in `src/types/`
2. Update interface to include missing property
3. Restart TypeScript server in VS Code:
   - Cmd+Shift+P (Mac) / Ctrl+Shift+P (Windows)
   - Type: "TypeScript: Restart TS Server"

### Database Issues

#### Issue 1: Cannot Create /data Directory

**Error**:
```
Failed to create directory: /data/output/hr
```

**Solution** (macOS):
```bash
# Use /tmp/data instead
mkdir -p /tmp/data/{output,input,archive}/hr

# Update database configuration via Liquibase
cd fabric-core/fabric-api
mvn liquibase:update
```

#### Issue 2: Tables Not Found

**Error**:
```
ORA-00942: table or view does not exist
```

**Solution**:
```bash
# Check if tables exist
sqlplus cm3int/MySecurePass123@localhost:1521/ORCLPDB1

SQL> SELECT table_name FROM user_tables;

# If no tables, run Liquibase
cd fabric-core/fabric-api
mvn liquibase:update
```

#### Issue 3: Connection Pool Exhausted

**Error**:
```
HikariPool-1 - Connection is not available, request timed out after 30000ms.
```

**Solution**:
1. Increase pool size in `application-local.properties`:
   ```properties
   spring.datasource.hikari.maximum-pool-size=20
   spring.datasource.hikari.connection-timeout=60000
   ```

2. Check for connection leaks in code (ensure connections are closed)

---

## Additional Configuration

### Enable Debug Logging

**Backend** (`application-local.properties`):
```properties
# Application Logging
logging.level.com.truist.batch=DEBUG
logging.level.org.springframework.web=DEBUG
logging.level.org.springframework.security=DEBUG
logging.level.org.springframework.jdbc=DEBUG

# SQL Logging
logging.level.org.springframework.jdbc.core.JdbcTemplate=DEBUG
```

**Frontend** (`.env.local`):
```bash
REACT_APP_LOG_LEVEL=debug
```

### Hot Reload Configuration

**Eclipse**: Automatic by default with Spring Boot DevTools

**VS Code**: Automatic with React development server

### Git Configuration

```bash
# Set up Git user
git config --global user.name "Your Name"
git config --global user.email "your.email@example.com"

# Create .gitignore (if not exists)
cat <<EOF > .gitignore
# IDE
.vscode/
.idea/
*.iml
.classpath
.project
.settings/

# Build
target/
build/
dist/
node_modules/

# Environment
.env.local
application-local.properties

# Logs
*.log
EOF
```

---

## Performance Optimization Tips

### Backend

1. **Increase Maven Memory**:
   ```bash
   export MAVEN_OPTS="-Xmx2048m -XX:MaxPermSize=512m"
   ```

2. **Enable Parallel Builds**:
   ```bash
   mvn clean install -T 1C  # 1 thread per CPU core
   ```

3. **Skip Tests During Development**:
   ```bash
   mvn clean compile -DskipTests
   ```

### Frontend

1. **Reduce Bundle Size**:
   ```bash
   # Analyze bundle
   npm run build
   npx source-map-explorer 'build/static/js/*.js'
   ```

2. **Enable Fast Refresh**: Already enabled in Create React App

3. **Clear Cache if Issues**:
   ```bash
   rm -rf node_modules/.cache
   npm start
   ```

---

## Next Steps

After successful setup:

1. **Explore Swagger UI**: http://localhost:8080/swagger-ui.html
2. **Test API Endpoints**: Use Postman or curl
3. **Run Sample Batch Job**:
   ```bash
   curl -X POST http://localhost:8080/api/v2/manual-job-execution/execute/cfg_hr_123 \
     -H "Authorization: Bearer YOUR_JWT_TOKEN" \
     -H "Content-Type: application/json" \
     -d '{"batchDate": "2025-11-23"}'
   ```
4. **Review Documentation**: Check `/docs` folder for additional guides
5. **Join Development**: Start working on user stories and features

---

## Support

For issues not covered in this guide:

- **Backend Issues**: Check Eclipse console logs
- **Frontend Issues**: Check browser DevTools console (F12)
- **Database Issues**: Check Oracle alert logs
- **Build Issues**: Run with verbose logging (`mvn -X` or `npm start --verbose`)

## References

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [React Documentation](https://react.dev/)
- [Material-UI Documentation](https://mui.com/)
- [Maven Documentation](https://maven.apache.org/guides/)
- [Oracle JDBC Documentation](https://docs.oracle.com/en/database/oracle/oracle-database/19/jjdbc/)

---

**Last Updated**: 2025-11-23
**Version**: 1.0
**Status**: Tested on macOS and Windows environments
