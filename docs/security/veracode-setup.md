# Veracode Enterprise Security Integration — Setup Guide

This guide covers the one-time setup required to enable the three Veracode
workflows in `.github/workflows/`:

| Workflow | File | Purpose |
|----------|------|---------|
| Pipeline Scan (SAST) | `veracode-pipeline-scan.yml` | Fast SAST gate on every PR (~60 s) |
| Policy Scan (SAST) | `veracode-policy-scan.yml` | Full authoritative SAST on main merge + weekly |
| SCA (Dependencies) | `veracode-sca.yml` | Open-source dependency CVE scan on PRs + weekly |

---

## 1. Prerequisites

- A Veracode subscription with **Static Analysis** and **SCA** enabled
- Admin or Security Lead access to the Veracode Application Security Platform
- Admin access to the GitHub repository (Settings)
- Corporate proxy details if the runner exits via a proxy

---

## 2. Veracode Credentials

### 2a. API Credentials (Static Analysis)

Used by: `veracode-pipeline-scan.yml`, `veracode-policy-scan.yml`

1. Log into [analysiscenter.veracode.com](https://analysiscenter.veracode.com)
2. Click your name → **API Credentials**
3. Click **Generate API Credentials**
4. Copy the **API ID** and **API Key** — you will not see the key again

### 2b. SCA Agent Token

Used by: `veracode-sca.yml`

1. In the Veracode platform: **Integrations** → **Agent-Based SCA**
2. Select **+ Add Agent** → choose **GitHub Actions**
3. Copy the `SRCCLR_API_TOKEN` shown — you will not see it again

---

## 3. GitHub Secrets

Go to: **GitHub repo → Settings → Secrets and variables → Actions → Secrets**

Add the following repository secrets:

| Secret name | Value | Used by |
|-------------|-------|---------|
| `VERACODE_API_ID` | API ID from step 2a | Pipeline Scan, Policy Scan |
| `VERACODE_API_KEY` | API Key from step 2a | Pipeline Scan, Policy Scan |
| `SRCCLR_API_TOKEN` | SCA token from step 2b | SCA workflow |

> **Security note:** Never put these values in YAML files, environment files,
> or commit messages. GitHub Secrets are encrypted at rest and masked in logs.

---

## 4. GitHub Variables

Go to: **GitHub repo → Settings → Secrets and variables → Actions → Variables**

### Required

| Variable name | Example value | Used by |
|---------------|---------------|---------|
| `VERACODE_APP_NAME` | `fabric-platform` | Policy Scan |

The application name must match exactly what you create in the Veracode platform
(step 5). If the profile does not exist, `createprofile: true` in the workflow
will create it automatically on first scan.

### Optional (enterprise proxy)

If your GitHub Actions runners exit via a corporate proxy:

| Variable name | Example value | Used by |
|---------------|---------------|---------|
| `VERACODE_PROXY_HOST` | `proxy.corp.example.com` | All three workflows |
| `VERACODE_PROXY_PORT` | `8080` | All three workflows |

Leave these **unset** (not empty, just absent) if no proxy is needed. The
workflows check `vars.VERACODE_PROXY_HOST != ''` before constructing the proxy
URL, so absent variables work correctly.

---

## 5. Veracode Application Profile

The policy scan requires an application profile in the Veracode platform.

1. In Veracode: **Applications** → **+ New Application**
2. Set **Application Name** to match `VERACODE_APP_NAME` (e.g. `fabric-platform`)
3. Set **Business Criticality** → `Very High` (fintech/banking classification)
4. Assign the application to the appropriate **Team** (for scan notifications)
5. Under **Policy**, select the policy that matches your organization's security
   requirements (e.g. `Veracode Recommended High + SCA Recommended`)

The pipeline scan does not require an application profile — it runs stand-alone
and reports results only to GitHub.

---

## 6. First Scan Checklist

After setting up secrets and variables, trigger a manual run to verify:

```
GitHub → Actions → Veracode Policy Scan (Full SAST) → Run workflow
```

Expected first-run behavior:
- [ ] Build artifact step succeeds (JAR + frontend ZIP created)
- [ ] Scan is submitted to Veracode platform (check Scans tab)
- [ ] Scan completes within 90 minutes (scantimeout setting)
- [ ] Results appear in Veracode platform under the application profile
- [ ] `notify-on-failure` job only runs if policy is violated

For the SCA scan:
```
GitHub → Actions → Veracode SCA (Dependency Scan) → Run workflow
```

- [ ] Maven dependencies resolved and scanned
- [ ] npm dependencies resolved and scanned
- [ ] Results appear in Veracode SCA dashboard

---

## 7. Suppressing False Positives

### SAST (Pipeline / Policy Scan)

Pipeline scan findings can be filtered using a `baseline.json` file committed to
the repository root. Generate one after the first clean scan:

```bash
# After a passing pipeline scan, download results.json from the workflow artifact
# then convert it to a baseline:
java -jar pipeline-scan.jar --baseline_file baseline.json --file results.json
git add baseline.json
git commit -m "chore: add Veracode pipeline scan baseline"
```

Policy scan mitigations are applied in the Veracode platform UI by the security
team (not via code). Each mitigation requires:
- Technique (Mitigate by Design / Mitigate by Network Environment / etc.)
- Justification explaining why the finding does not represent actual risk
- Approver sign-off from a security lead

### SCA (Open-Source Dependencies)

SCA findings are managed in the Veracode SCA dashboard. For dependencies that
cannot be updated immediately:
1. In Veracode SCA → select the vulnerability
2. Click **Ignore** → provide justification and expiry date
3. Ignored findings no longer fail the scan gate

For Maven dependencies, the OWASP suppression file at
`fabric-core/fabric-api/src/main/resources/owasp-suppressions.xml` controls the
**local build gate** (separate from Veracode's gate). Both suppression mechanisms
should be kept consistent.

---

## 8. Scan Schedule Summary

| Scan | Trigger | Estimated duration | Failure action |
|------|---------|--------------------|----------------|
| Pipeline SAST | Every PR / push to main | ~60 seconds | Blocks merge |
| Policy SAST | Merge to main + Sunday 02:00 UTC | 30–90 minutes | Opens GitHub issue |
| SCA (both) | Every PR / push to main + Monday 07:00 UTC | ~5 minutes | Blocks PR / opens issue |
| OWASP (Maven) | Every `mvn verify` | ~5 min first run, fast cached | Fails Maven build |
| npm audit | `npm build` / `npm test` | < 30 seconds | Fails npm build |

---

## 9. Troubleshooting

### `SRCCLR_API_TOKEN not found` or `401 Unauthorized` (SCA)
- Verify the `SRCCLR_API_TOKEN` secret is set (not a variable — it must be a secret)
- Token may have been regenerated in Veracode platform; re-copy and update the secret

### Policy scan `appname not found`
- Verify `VERACODE_APP_NAME` variable matches exactly the application profile name in Veracode
- If `createprofile: true` is set, the profile is created on first scan — check Veracode Applications tab

### Scan timeout after 90 minutes
- Increase `scantimeout` in `veracode-policy-scan.yml` (max 120 minutes for most plans)
- Large codebases may need `scantimeout: 120`

### Fork PRs skipped
- By design — fork PRs cannot access repository secrets. The scan runs only for
  PRs from branches within the same repository.

### Corporate proxy required
- Set `VERACODE_PROXY_HOST` and `VERACODE_PROXY_PORT` as GitHub Variables (not secrets)
- For SCA, `SRCCLR_PROXY` is also constructed from these same variables
