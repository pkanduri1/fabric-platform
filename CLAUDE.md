# Fabric Platform — Claude Instructions

## Project Overview

Enterprise-grade Spring Boot batch processing platform for financial data pipelines. Provides REST APIs for job configuration, execution, scheduling, and monitoring. Used by operations teams to manage Shaw→C360 data migrations and other batch ETL workflows.

**GitHub:** `pkanduri1/fabric-platform`
**Main application:** `fabric-core/fabric-api` (Spring Boot, port 8080)
**Frontend:** `fabric-ui/` (React 18, port 3000)
**Active branches:** `main` (primary), feature branches off it

---

## Implementation Process (Per Issue)

Follow these steps in order for every feature or bug fix:

| #  | Step | What happens |
|----|------|--------------|
| 1  | **Explore** | Read relevant source files — understand existing patterns before touching code |
| 2  | **Task list** | Break issue into concrete subtasks using TodoWrite |
| 3  | **Tests first** | Write failing tests before writing implementation |
| 4  | **Implement** | Write minimal code to make the tests pass |
| 5  | **Unit/integration test run** | `mvn test -pl fabric-api` — all tests must pass against Oracle |
| 6  | **Coverage check** | `mvn verify -pl fabric-api` — JaCoCo gate ≥80% line coverage must pass |
| 7  | **Spec review** | Tick off every acceptance criterion from the issue |
| 8  | **Architecture review** | Verify change against the 5 Architecture Principles below |
| 9  | **Code review** | No SQL injection, no hardcoded credentials, no mock/stub data left in |
| 10 | **Documentation update** | Update OpenAPI annotations, README/docs if behaviour changed |
| 11 | **E2E test** | Add/update Playwright test in `fabric-ui/e2e/` covering the feature |
| 12 | **Regression suite** | Run full Playwright suite (`npx playwright test`) — all tests must pass |
| 13 | **Commit** | Conventional commit message with issue number |

---

## 5 Architecture Principles

Check every change against these:

1. **JdbcTemplate, not JPA** — All DB access uses `JdbcTemplate` + `RowMapper`. No Hibernate, no `@Entity`, no Spring Data repositories. This is intentional for performance and SQL control.

2. **Repository interface + impl** — Every table has an interface (e.g. `ManualJobConfigRepository`) and a `*RepositoryImpl` class. Never put SQL in services. Never skip the interface.

3. **Thin controllers** — Controllers only validate input (`@Valid`), call a service method, and return `ResponseEntity`. No business logic in controllers.

4. **Profile separation** — `@Profile("local")` code never runs in production. `@Profile("!local")` code (including `SecurityConfig`, `QuerySecurityConfig`) is always active in tests. Tests connect to the real Oracle test schema — no H2, no mocks.

5. **Liquibase for all schema changes** — Never modify existing changeset files. Every column, table, or index goes through a new Liquibase changeset in `releases/usXXX/`. Liquibase must be enabled (`spring.liquibase.enabled=true`) in all non-local profiles.

---

## Build & Test Commands

All Maven commands run from `fabric-core/` directory:

```bash
# Run all tests (Oracle must be running on port 1522)
mvn test -pl fabric-api

# Run a specific test class
mvn test -pl fabric-api -Dtest="ClassName"

# Run a specific test method
mvn test -pl fabric-api -Dtest="ClassName#methodName"

# Full build + coverage gate (≥80% required)
mvn verify -pl fabric-api

# Full build (skip tests)
mvn clean install -DskipTests -pl fabric-api

# Run application locally
mvn spring-boot:run -pl fabric-api -Dspring-boot.run.profiles=local

# Run E2E tests (frontend + backend must be running)
cd fabric-ui && npx playwright test

# Run a specific Playwright spec
cd fabric-ui && npx playwright test e2e/job-execution.spec.ts

# Run Playwright with headed browser (for debugging)
cd fabric-ui && npx playwright test --headed
```

**Test targets:** All unit/integration tests pass, 0 failures. JaCoCo coverage ≥80%. All Playwright E2E tests pass.

---

## Key Directories

```
fabric-core/
  fabric-api/src/main/java/com/fabric/batch/
    controller/          # REST endpoints — 1 controller per resource
    service/             # Business logic — 1 service per domain
    repository/          # Interfaces (contracts)
    repository/impl/     # JdbcTemplate implementations with RowMappers
    entity/              # DB row → Java object POJOs (no @Entity)
    dto/                 # Request/Response DTOs (Lombok + validation)
    config/              # Spring @Configuration classes
    security/            # JWT filter, LDAP provider, SecurityConfig
      config/            # SecurityConfig, QuerySecurityConfig
      jwt/               # JwtAuthenticationFilter, JwtTokenProvider
      ldap/              # LdapAuthenticationProvider, FabricUserDetails, LdapUserDetails
      service/           # UserSecurityService, SecurityAuditService
    exception/           # Custom exception hierarchy

  fabric-api/src/main/resources/
    application.yml                    # Main config (no hardcoded credentials)
    application-local.properties       # Local dev overrides (gitignored)
    db/changelog/db.changelog-master.xml  # Liquibase root
    db/changelog/releases/             # Migrations by user story (usXXX folders)

  fabric-api/src/test/java/com/fabric/batch/
    integration/         # @SpringBootTest + Oracle integration tests
    service/             # Mockito unit tests
    controller/          # Security slice tests (also @SpringBootTest + Oracle)

fabric-ui/               # React frontend (separate npm project)
  e2e/                   # Playwright E2E tests
  .env.playwright         # Playwright config (BASE_URL, E2E_USERNAME, E2E_PASSWORD)
docs/plans/              # Feature plans (YYYY-MM-DD-feature.md)
```

---

## Oracle Database

### Local Development

| Property | Value |
|----------|-------|
| Container | `fabric-oracle-free` (Docker) |
| Port | `1522` (mapped from container 1521) |
| Service | `FREEPDB1` |
| Username | `cm3int` |
| JDBC URL | `jdbc:oracle:thin:@localhost:1522/FREEPDB1` |
| Driver | `ojdbc11` (thin mode, no Instant Client needed) |
| Credentials | `application-local.properties` (gitignored) — **never commit passwords** |

### Environment Variables (required for non-local profiles)

```
DB_URL=jdbc:oracle:thin:@localhost:1522/FREEPDB1
DB_USERNAME=cm3int
DB_PASSWORD=<from vault/secrets>
```

`application.yml` must reference these as `${DB_URL}`, `${DB_USERNAME}`, `${DB_PASSWORD}` with **no default values**.

### Oracle SQL conventions

- Use `FETCH FIRST ? ROWS ONLY` for pagination (Oracle syntax)
- Use `SYSDATE` or `CURRENT_TIMESTAMP` for timestamps
- Use `MERGE INTO table USING DUAL ON (key = ?) WHEN MATCHED ... WHEN NOT MATCHED ...` for upsert
- `QuerySecurityConfig` health check: `SELECT 1 FROM DUAL`

---

## Integration Test Pattern

Tests connect to **real Oracle** (not H2). The Oracle container must be running on port 1522.

Use `@TestPropertySource` to point at the test Oracle schema, then clean up test data in `@AfterEach`:

```java
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:oracle:thin:@localhost:1522/FREEPDB1",
    "spring.datasource.driver-class-name=oracle.jdbc.OracleDriver",
    "spring.datasource.username=cm3int",
    "spring.datasource.password=MySecurePass123",
    "spring.datasource.primary.url=jdbc:oracle:thin:@localhost:1522/FREEPDB1",
    "spring.datasource.primary.driver-class-name=oracle.jdbc.OracleDriver",
    "spring.datasource.primary.username=cm3int",
    "spring.datasource.primary.password=MySecurePass123",
    "spring.datasource.readonly.url=jdbc:oracle:thin:@localhost:1522/FREEPDB1",
    "spring.datasource.readonly.driver-class-name=oracle.jdbc.OracleDriver",
    "spring.datasource.readonly.username=cm3int",
    "spring.datasource.readonly.password=MySecurePass123",
    "spring.liquibase.enabled=false",
    "fabric.security.csrf.enabled=false",
    "fabric.security.ldap.enabled=false"
})
class MyFeatureIntegrationTest {

    @Autowired @Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate;

    private static final String TEST_ID = "TEST-IT-001";

    @BeforeEach
    void seed() {
        jdbcTemplate.update("DELETE FROM MY_TABLE WHERE ID LIKE 'TEST-%'");
        jdbcTemplate.update("INSERT INTO MY_TABLE (ID, ...) VALUES (?, ...)", TEST_ID, ...);
    }

    @AfterEach
    void cleanup() {
        jdbcTemplate.update("DELETE FROM MY_TABLE WHERE ID LIKE 'TEST-%'");
    }
}
```

**Key rules:**
- Always prefix test data IDs with `TEST-` so cleanup is safe (`DELETE WHERE ID LIKE 'TEST-%'`)
- Never share test data between test classes without a different prefix
- `spring.liquibase.enabled=false` in integration tests (schema already exists in Oracle)

---

## LDAP / Authentication

### Local LDAP Setup (Docker)

Run OpenLDAP with TLS locally:

```bash
# Start OpenLDAP (from project root)
docker run -d \
  --name fabric-ldap \
  -p 389:389 -p 636:636 \
  -e LDAP_ORGANISATION="Fabric Platform" \
  -e LDAP_DOMAIN="fabric.local" \
  -e LDAP_ADMIN_PASSWORD="FabricAdmin123" \
  -e LDAP_TLS=true \
  osixia/openldap:1.5.0

# Load test users (after container is ready)
docker exec fabric-ldap ldapadd -x -H ldap://localhost \
  -D "cn=admin,dc=fabric,dc=local" -w FabricAdmin123 \
  -f /path/to/test-users.ldif
```

### Application Configuration (non-local profiles)

```yaml
fabric:
  security:
    ldap:
      enabled: true
      url: ldap://localhost:389
      base-dn: dc=fabric,dc=local
      user-search-base: ou=users,dc=fabric,dc=local
      user-search-filter: uid={0}
      group-search-base: ou=groups,dc=fabric,dc=local
      manager-dn: cn=admin,dc=fabric,dc=local
      manager-password: ${LDAP_ADMIN_PASSWORD}
```

### LDAP User → Fabric Role Mapping

| LDAP group CN | Fabric role |
|---------------|------------|
| `fabric-admins` | `ROLE_ADMIN` |
| `fabric-managers` | `ROLE_MANAGER` |
| `fabric-job-viewers` | `ROLE_JOB_VIEWER` |
| `fabric-job-creators` | `ROLE_JOB_CREATOR` |
| `fabric-job-modifiers` | `ROLE_JOB_MODIFIER` |
| `fabric-job-executors` | `ROLE_JOB_EXECUTOR` |
| `fabric-api-executors` | `ROLE_API_EXECUTOR` |
| `fabric-monitoring` | `ROLE_OPERATIONS_MANAGER` |

### JWT flow

1. `POST /api/auth/login` → validates against LDAP, issues JWT
2. All subsequent requests: `Authorization: Bearer <token>` header
3. `JwtAuthenticationFilter` validates token on every request
4. Roles embedded in JWT claims from LDAP group membership

---

## Repository Pattern

Every new table follows this exact pattern:

**1. Interface** (`repository/ManualJobConfigRepository.java`):
```java
public interface ManualJobConfigRepository {
    Optional<ManualJobConfigEntity> findById(String id);
    ManualJobConfigEntity save(ManualJobConfigEntity entity);
    void updateStatus(String id, String status);
}
```

**2. Implementation** (`repository/impl/ManualJobConfigRepositoryImpl.java`):
```java
@Repository
@RequiredArgsConstructor
@Slf4j
public class ManualJobConfigRepositoryImpl implements ManualJobConfigRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Optional<ManualJobConfigEntity> findById(String id) {
        try {
            return Optional.ofNullable(
                jdbcTemplate.queryForObject(
                    "SELECT * FROM MANUAL_JOB_CONFIG WHERE CONFIG_ID = ?",
                    new ManualJobConfigRowMapper(), id));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static class ManualJobConfigRowMapper implements RowMapper<ManualJobConfigEntity> {
        @Override
        public ManualJobConfigEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
            ManualJobConfigEntity entity = new ManualJobConfigEntity();
            entity.setConfigId(rs.getString("CONFIG_ID"));
            // ... map all columns
            return entity;
        }
    }
}
```

---

## Liquibase Migration Convention

Each user story gets its own folder. Format: `releases/usXXX/usXXX-NNN-description.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="...">

    <changeSet id="usXXX-NNN-description" author="claude">
        <addColumn tableName="EXISTING_TABLE">
            <column name="NEW_COLUMN" type="VARCHAR2(100)"/>
        </addColumn>
        <rollback>
            <dropColumn tableName="EXISTING_TABLE" columnName="NEW_COLUMN"/>
        </rollback>
    </changeSet>
</databaseChangeLog>
```

Register in `db.changelog-master.xml`:
```xml
<include file="releases/usXXX/usXXX-NNN-description.xml" relativeToChangelogFile="true"/>
```

**Rules:**
- Always include a `<rollback>` section
- Never modify existing changeset XML files — add new changesets only
- New tables: use Oracle-compatible DDL (`VARCHAR2`, `NUMBER`, `CLOB`, `DATE`)
- Liquibase must be **enabled** in all non-local profiles (`spring.liquibase.enabled=true`)

---

## Security & Roles

| Role | Description |
|------|-------------|
| `ROLE_ADMIN` | Full system access |
| `ROLE_MANAGER` | Management endpoints |
| `ROLE_JOB_VIEWER` | Read job configs and executions |
| `ROLE_JOB_CREATOR` | Create new job configs |
| `ROLE_JOB_MODIFIER` | Update/delete job configs |
| `ROLE_JOB_EXECUTOR` | Execute jobs via UI (`/api/v2/job-execution/`) |
| `ROLE_API_EXECUTOR` | Execute jobs via REST API (`/api/v1/jobs/`) |
| `ROLE_OPERATIONS_MANAGER` | Access monitoring dashboard |

**Adding a new secured endpoint:**
1. Add `@PreAuthorize("hasRole('ROLE_NAME')")` on the controller method
2. Add `requestMatchers("/api/path/**").hasRole("ROLE_NAME")` in `SecurityConfig.filterChain()`

**Local dev** (`-Dspring-boot.run.profiles=local`): `LocalSecurityConfig` disables all auth.

---

## E2E Testing (Playwright)

All features must have Playwright coverage. Tests live in `fabric-ui/e2e/`.

### Setup

```bash
# Required env file: fabric-ui/.env.playwright
BASE_URL=http://localhost:3000
E2E_USERNAME=testuser
E2E_PASSWORD=testpass1234

# Install deps (one-time)
cd fabric-ui && npm install && npx playwright install chromium
```

### Test structure

```typescript
// fabric-ui/e2e/feature-name.spec.ts
import { test, expect } from '@playwright/test';

test.describe('Feature Name', () => {
  test.beforeEach(async ({ page }) => {
    // Auth state is loaded from e2e/.auth/user.json (global-setup.ts)
    await page.goto('/feature-path');
  });

  test('should do expected thing', async ({ page }) => {
    await page.click('[data-testid="action-button"]');
    await expect(page.locator('[data-testid="result"]')).toBeVisible();
  });
});
```

**Rules:**
- Use `data-testid` attributes — never rely on CSS classes or text for selectors
- Add `data-testid` to new components as you build them
- Each new API-backed feature needs at least: happy path + unauthenticated (401) + wrong role (403)
- Regression suite runs after every issue: `npx playwright test` must pass 100%

---

## Coverage (JaCoCo)

Target: **≥80% line coverage** enforced by `mvn verify`.

```xml
<!-- fabric-api/pom.xml -->
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <executions>
        <execution>
            <id>check</id>
            <goals><goal>check</goal></goals>
            <configuration>
                <rules>
                    <rule>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.80</minimum>
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

Coverage report: `fabric-api/target/site/jacoco/index.html`

---

## DTO Conventions

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)  // omit null fields in response DTOs
public class SomeRequest {
    @NotBlank(message = "fieldName is required")
    private String fieldName;

    @NotEmpty
    private List<String> items;
}
```

- Request DTOs: use `@NotBlank`, `@NotNull`, `@NotEmpty` — validated with `@Valid` in controller
- Response DTOs: use `@JsonInclude(NON_NULL)` to keep responses clean
- Error responses: include `errorCode` (SCREAMING_SNAKE_CASE) and `message` fields

---

## Exception Handling

Custom exceptions extend `RuntimeException` and include an `errorCode`:

```java
throw JobExecutionApiException.notFound("EXECUTION_NOT_FOUND",
    "EXECUTION_NOT_FOUND: No execution found with id " + id);
```

`GlobalExceptionHandler` (`controller/GlobalExceptionHandler.java`) maps exceptions to HTTP responses. Add a new `@ExceptionHandler` method there for any new exception type.

---

## Commit Convention

```
feat(scope): short description
fix(scope): short description
test(scope): short description
refactor(scope): short description
docs(scope): short description
```

Examples:
- `feat(#35): add Job Execution REST API with webhook callbacks`
- `fix(#44): align JOB_PARAMETER_TEMPLATE table name between Liquibase and Java`
- `test(#43): integration tests for all 6 job execution endpoints`
- `docs(#46): remove hardcoded DB password, use env var injection`

Always reference the issue number. Closes go in the commit body: `Closes #44`.

---

## Known Pitfalls

- **No H2 in integration tests** — Tests connect to real Oracle on port 1522. H2 is removed. Start `fabric-oracle-free` Docker container before running tests.
- **Oracle pagination** — Use `FETCH FIRST ? ROWS ONLY`, not `LIMIT ?`. Oracle does not support `LIMIT`.
- **Test data isolation** — Prefix all test IDs with `TEST-` and use `DELETE WHERE ID LIKE 'TEST-%'` in `@AfterEach`. Never truncate shared tables.
- **`@WebMvcTest` fails** — `QuerySecurityConfig` (`@Profile("!local")`) eagerly connects to Oracle. Always use `@SpringBootTest` for controller/security tests.
- **`save()` returns entity, not void** — Use `when(repo.save(any())).thenReturn(null)` in Mockito, not `doNothing()`.
- **`MONITORING_ALERTS_SENT` is `CHAR(1)`** — Default `'N'` must be set before insert or a null constraint fires.
- **Liquibase must be enabled** — `spring.liquibase.enabled=true` for non-local profiles. Integration tests set it to `false` (schema already in Oracle).
- **No hardcoded credentials anywhere** — Use `${ENV_VAR}` with no default value in `application.yml`. Put defaults only in `application-local.properties` (gitignored).
- **LDAP disabled by default** — `fabric.security.ldap.enabled=false`. Set to `true` only when local OpenLDAP is running. Integration tests always set it to `false`.
