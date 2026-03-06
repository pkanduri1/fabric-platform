# Fabric Platform — Claude Instructions

## Project Overview

Enterprise-grade Spring Boot batch processing platform for financial data pipelines. Provides REST APIs for job configuration, execution, scheduling, and monitoring. Used by operations teams to manage Shaw→C360 data migrations and other batch ETL workflows.

**GitHub:** `pkanduri1/fabric-platform`
**Main application:** `fabric-core/fabric-api` (Spring Boot, port 8080)
**Frontend:** `fabric-ui/` (React 18, port 3000)
**Active branches:** `batch-enhancements` (main dev branch), feature branches off it

---

## Implementation Process (Per Issue)

Follow these steps in order for every feature or bug fix:

| # | Step | What happens |
|---|------|--------------|
| 1 | **Explore** | Read relevant source files — understand existing patterns before touching code |
| 2 | **Task list** | Break issue into concrete subtasks using TodoWrite |
| 3 | **Tests first** | Write failing tests before writing implementation |
| 4 | **Implement** | Write minimal code to make the tests pass |
| 5 | **Test run** | `mvn test -pl fabric-api` — all tests must pass |
| 6 | **Spec review** | Tick off every acceptance criterion from the issue |
| 7 | **Code review** | No SQL injection, no hardcoded credentials, follows patterns below |
| 8 | **Commit** | Conventional commit message with issue number |

---

## 5 Architecture Principles

Check every change against these:

1. **JdbcTemplate, not JPA** — All DB access uses `JdbcTemplate` + `RowMapper`. No Hibernate, no `@Entity`, no Spring Data repositories. This is intentional for performance and SQL control.

2. **Repository interface + impl** — Every table has an interface (e.g. `ManualJobConfigRepository`) and a `*RepositoryImpl` class. Never put SQL in services. Never skip the interface.

3. **Thin controllers** — Controllers only validate input (`@Valid`), call a service method, and return `ResponseEntity`. No business logic in controllers.

4. **Profile separation** — `@Profile("local")` code never runs in production. `@Profile("!local")` code (including `SecurityConfig`, `QuerySecurityConfig`) is always active in tests unless explicitly overridden.

5. **Liquibase for all schema changes** — Never modify `schema.sql` for production changes. Every column, table, or index goes through a new Liquibase changeset in `releases/usXXX/`.

---

## Build & Test Commands

All Maven commands run from `fabric-core/` directory:

```bash
# Run all tests (main command)
mvn test -pl fabric-api

# Run a specific test class
mvn test -pl fabric-api -Dtest="ClassName"

# Run a specific test method
mvn test -pl fabric-api -Dtest="ClassName#methodName"

# Full build (skip tests)
mvn clean install -DskipTests -pl fabric-api

# Run application locally
mvn spring-boot:run -pl fabric-api -Dspring-boot.run.profiles=local
```

**Test target:** All tests pass, 0 failures. Coverage ≥80%.

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
    security/            # JWT filter, entry point, SecurityConfig
    exception/           # Custom exception hierarchy

  fabric-api/src/main/resources/
    application.yml                    # Main config
    application-local.properties       # Local dev overrides
    db/changelog/db.changelog-master.xml  # Liquibase root
    db/changelog/releases/             # Migrations by user story

  fabric-api/src/test/resources/
    schema-h2.sql        # H2-compatible table definitions (add new tables here)
    data-h2.sql          # Seed data for integration tests

  fabric-api/src/test/java/com/fabric/batch/
    integration/         # @SpringBootTest + H2 integration tests
    service/             # Mockito unit tests
    controller/          # Security slice tests

fabric-ui/               # React frontend (separate npm project)
docs/plans/              # Feature plans (YYYY-MM-DD-feature.md)
```

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

**Oracle vs H2 SQL notes:**
- Use `LIMIT ?` not `FETCH FIRST ? ROWS ONLY` (H2 tests use `LIMIT`)
- Use `CURRENT_TIMESTAMP` not `SYSDATE`
- H2 `MERGE INTO table KEY (col) VALUES (...)` for upsert in tests
- New columns added via Liquibase, not by modifying existing XML changesets

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

Always include a `<rollback>` section. Always add `IF NOT EXISTS` to H2 `schema-h2.sql` when adding new tables.

---

## Integration Test Pattern

The `@WebMvcTest` slice CANNOT be used — `QuerySecurityConfig` (`@Profile("!local")`) eagerly connects to Oracle at context startup. All controller/security tests must use `@SpringBootTest` + `@TestConfiguration` providing H2 datasource beans.

**Use a unique H2 database name per test class** to avoid `DataSourceInitializer` PK conflicts when multiple tests share `data-h2.sql` inserts.

```java
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:mytest;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=password",
    "spring.datasource.primary.url=jdbc:h2:mem:mytest;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.primary.driver-class-name=org.h2.Driver",
    "spring.datasource.primary.username=sa",
    "spring.datasource.primary.password=password",
    "spring.datasource.readonly.url=jdbc:h2:mem:mytest;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.readonly.driver-class-name=org.h2.Driver",
    "spring.datasource.readonly.username=sa",
    "spring.datasource.readonly.password=password",
    "spring.jpa.hibernate.ddl-auto=none",
    "spring.liquibase.enabled=false",
    "spring.sql.init.mode=never",
    "fabric.security.csrf.enabled=false"
})
class MyFeatureIntegrationTest {

    @TestConfiguration
    static class H2TestDataSourceConfig {

        @Bean(name = "dataSource") @Primary
        public DataSource dataSource() {
            return DataSourceBuilder.create().driverClassName("org.h2.Driver")
                .url("jdbc:h2:mem:mytest;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
                .username("sa").password("password").build();
        }

        @Bean(name = "readOnlyDataSource")
        public DataSource readOnlyDataSource() {
            return DataSourceBuilder.create().driverClassName("org.h2.Driver")
                .url("jdbc:h2:mem:mytest;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
                .username("sa").password("password").build();
        }

        @Bean(name = "jdbcTemplate") @Primary
        public JdbcTemplate jdbcTemplate(@Qualifier("dataSource") DataSource ds) {
            return new JdbcTemplate(ds);
        }

        @Bean(name = "readOnlyJdbcTemplate")
        public JdbcTemplate readOnlyJdbcTemplate(@Qualifier("readOnlyDataSource") DataSource ds) {
            return new JdbcTemplate(ds);
        }

        @Bean(name = "readOnlyDataSourceHealthIndicator")
        public QuerySecurityConfig.ReadOnlyDataSourceHealthIndicator healthIndicator(
                @Qualifier("readOnlyDataSource") DataSource ds) {
            return new QuerySecurityConfig.ReadOnlyDataSourceHealthIndicator(ds);
        }

        @Bean
        public DataSourceInitializer dataSourceInitializer(@Qualifier("dataSource") DataSource ds) {
            DataSourceInitializer init = new DataSourceInitializer();
            init.setDataSource(ds);
            ResourceDatabasePopulator pop = new ResourceDatabasePopulator();
            pop.addScript(new ClassPathResource("schema-h2.sql"));
            pop.addScript(new ClassPathResource("data-h2.sql"));
            init.setDatabasePopulator(pop);
            return init;
        }
    }
}
```

**Existing H2 DB names in use** (don't reuse these):
- `testdb` — `SourceSystemValidationIntegrationTest`
- `execdb` — `JobExecutionApiIntegrationTest`
- `sectest` — `JobExecutionApiControllerSecurityTest`

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

**Adding a new secured endpoint:**
1. Add `@PreAuthorize("hasRole('ROLE_NAME')")` on the controller method
2. Add `requestMatchers("/api/path/**").hasRole("ROLE_NAME")` in `SecurityConfig.filterChain()`

**Local dev** (`-Dspring-boot.run.profiles=local`): `LocalSecurityConfig` disables all auth.

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
```

Examples:
- `feat(#35): add Job Execution REST API with webhook callbacks`
- `fix(#42): resolve LIMIT vs FETCH FIRST H2 compatibility`
- `test(#43): integration tests for all 6 job execution endpoints`
- `feat(us036): add batch retry configuration endpoint`

Always reference the issue number. Closes go in the commit body: `Closes #35`.

---

## Oracle DB (Local Dev)

- Driver: `ojdbc11`, thin mode (no Instant Client needed)
- DSN: `jdbc:oracle:thin:@localhost:1521/ORCLPDB1`
- Schema: configured via `spring.datasource.primary.username`
- Credentials: `application-local.properties` (gitignored)
- `QuerySecurityConfig` test: `SELECT 1 FROM DUAL` (Oracle-only — don't use in H2 tests)

---

## Known Pitfalls

- **`@WebMvcTest` fails** — `QuerySecurityConfig` (`@Profile("!local")`) eagerly connects to Oracle. Always use `@SpringBootTest` + `H2TestDataSourceConfig` (see pattern above).
- **H2 `data-h2.sql` PK conflicts** — Each test class needs its own H2 database name. The `DataSourceInitializer` re-runs `data-h2.sql` on every context load, causing PK violations if shared.
- **`save()` returns entity, not void** — Use `when(repo.save(any())).thenReturn(null)` in Mockito, not `doNothing()`.
- **`findRecentApiExecutions` uses `LIMIT ?`** — H2-compatible. Oracle uses `FETCH FIRST ? ROWS ONLY` but tests run against H2.
- **`MONITORING_ALERTS_SENT` is `CHAR(1)`** — Default `'N'` must be set before insert or a null constraint fires.
