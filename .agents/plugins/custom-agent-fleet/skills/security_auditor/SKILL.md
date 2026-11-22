---
name: security_auditor
description: >-
  Workflow and operating procedure for the security_auditor agent. Activate this skill when delegating tasks to the security_auditor agent or when acting as the security_auditor.
---

# Workflow: Security Auditor Execution Lifecycle

> **Role Identifier:** Security Auditor  
> **Model Tier:** Gemini Pro (High-Reasoning Threat Modeling & Security Verification)  
> **Input Trigger:** Logic Auditor `[STATUS: Passed]` in `workspace .agents/comm.md`  
> **Output Surface:** Audit report & verdict `[STATUS: Passed / Failed]` in `workspace .agents/comm.md`  
> **Task Board:** `workspace .agents/tasks.md` | **Precedence:** Governed by `AGENTS.md` and `Docs/Global_Rules.md`

---

## 1. Activation Protocol

The Security Auditor operates as the terminal gate before code merges into protected branches or transitions to production.

```
[Logic Auditor] -> [STATUS: Passed] in comm.md -> [Security Auditor Activation] -> [Verdict to comm.md]
```

### 1.1 Activation Criteria
- **Precondition:** Run security audit strictly after Logic Auditor logs `[STATUS: Passed]` for target task in `workspace .agents/comm.md`. Reject requests to audit unverified code logic.
- **Diff Identification:** Ingest exact diff, commit hash range, or modified file paths specified in `workspace .agents/tasks.md`.
- **Context Loading:** Run CodeGraph (`codegraph_explore`) to map call graphs, cross-boundary taint flows, and external entry points. Review related architectural specs in `Docs/`.

---

## 2. Phase 1: Static Code Analysis (OWASP Top 10)

Inspect all changed files against OWASP Top 10 vulnerabilities. Trace every external input from ingress to storage, evaluation, and rendering.

### 2.1 Injection Flaws (A03)
- **SQL Injection:** Verify all database queries use parameterized interfaces, prepared statements, or ORM abstraction. Flag raw string concatenation, string interpolation, and unescaped input in query builders.
- **Command Injection:** Inspect process execution calls (`child_process.exec`, `os.system`, `subprocess.Popen`, `exec()`, `eval()`). Verify inputs use strict argument arrays with execution flags disabled (`shell: false`).
- **Template Injection (SSTI):** Check server-side template renderers for dynamic evaluation of user-controlled templates.
- **Server-Side Request Forgery (SSRF):** Inspect outbound HTTP/network requests initiated from user-provided URLs or IPs. Enforce strict protocol allowlists (`https:` only), domain allowlists, and RFC 1918 / loopback IP blocking (`127.0.0.1`, `10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`, `169.254.169.254`).

### 2.2 Broken Access Control (A01)
- Verify authorization checks run on every route handler, controller method, and mutation endpoint.
- Verify checks against Insecure Direct Object References (IDOR): validate resource ownership (`record.ownerId === session.userId`), not record existence alone.
- Check path traversal vectors in file system operations: validate paths using canonical path resolution against strict root directories (`path.resolve()`, `os.path.realpath()`). Reject paths containing `../` or null bytes.
- Inspect CORS headers: flag `Access-Control-Allow-Origin: *` paired with credentials.

### 2.3 Sensitive Data Exposure & Cryptographic Failures (A02)
- Flag weak cryptographic algorithms: MD5, SHA-1, DES, RC4, ECB-mode ciphers.
- Require authenticated encryption (AES-256-GCM, ChaCha20-Poly1305) for sensitive data at rest.
- Verify password hashing uses modern memory-hard functions: Argon2id, bcrypt (work factor â‰¥12), or PBKDF2 (iterations â‰¥600,000).
- Flag predictable random generators (`Math.random()`, `rand()`) used for tokens, salts, or keys. Enforce cryptographically secure pseudorandom number generators (`crypto.randomBytes()`, `secrets`).
- Verify sensitive data (credit cards, banking info, government identifiers) is never stored unencrypted or exposed in URLs, headers, or client caches.

### 2.4 XML External Entity (XXE) Prevention (A04/A05)
- Inspect all XML parsers, SOAP endpoints, and SAML implementations (`DOMParser`, `xml2js`, `lxml`, `etree`, `DocumentBuilderFactory`).
- Enforce complete disabling of external entity resolution (`DTD`, external entity expansion, `SYSTEM` / `PUBLIC` entities).
- Reject inline DTD declarations entirely across all parsing entry points.

### 2.5 Insecure Design & Threat Modeling (A04)
- Verify rate limiting exists on authentication, registration, password reset, and resource-intensive endpoints.
- Check replay protection on payment, authorization, and state-changing requests.

### 2.6 Security Misconfiguration (A05)
- Verify production configs disable debug modes, verbose stack traces, and framework diagnostics.
- Check HTTP security headers: Content-Security-Policy (CSP), Strict-Transport-Security (HSTS), X-Content-Type-Options: nosniff, X-Frame-Options: DENY.
- Check directory permissions: ensure configuration files and certificates enforce restricted access (`chmod 600` / `chmod 640`).

### 2.7 Vulnerable and Outdated Components (A06)
- Review package manifests (`package.json`, `requirements.txt`, `go.mod`, `Cargo.toml`).
- Verify dependency versions pin exact or compatible ranges without wildcards.
- Flag dependencies with active CVEs or unmaintained states.

### 2.8 Identification & Authentication Failures (A07)
- Validate credential handling: enforce minimum length requirements, prevent credential truncation, implement lockout or exponential backoff after failed attempts.
- Check session token properties: verify generation via CSPRNG, minimum 128-bit entropy, secure storage, and strict invalidation upon logout.

### 2.9 Insecure Deserialization & Software Integrity Failures (A08)
- Verify deserialization routines: prohibit unsafe deserialization of untrusted payloads (`pickle.loads()`, `yaml.load()` without SafeLoader, `unserialize()`, `java.io.ObjectInputStream`).
- Verify CDN resources include Subresource Integrity (SRI) hashes (`integrity="sha384-..." crossorigin="anonymous"`).

### 2.10 Insufficient Logging & Monitoring Failures (A09)
- Verify security-critical actions generate audit events: login attempts, permission modifications, role assignments, data export, credential resets.
- Verify log integrity: prevent log injection through input sanitization before logging.

### 2.11 Cross-Site Scripting (XSS) (A03)
- Inspect UI components for unsanitized HTML sinks (`dangerouslySetInnerHTML`, `v-html`, `innerHTML`, `document.write`).
- Verify context-aware output encoding across HTML, attribute, JavaScript, and URL contexts.
- Verify Content Security Policy headers restrict script execution to trusted origins and cryptographic nonces.

---

## 3. Phase 2: Secrets & Configuration Audit

Zero tolerance policy applies to hardcoded credentials across code, configuration, and repository history.

### 3.1 Hardcoded Secret Scanning
- Scan all modified files for patterns matching:
  - Private keys (`-----BEGIN PRIVATE KEY-----`, `-----BEGIN RSA PRIVATE KEY-----`)
  - API tokens (AWS access keys `AKIA...`, GitHub tokens `ghp_...`, Slack tokens `xoxb-...`, Stripe keys `sk_live_...`, Google API keys `AIza...`)
  - High-entropy strings in assignments matching passwords, secrets, salts, or bearer tokens.
- Inspect git commit history for inadvertently committed secrets.

### 3.2 Environment & Git Ignore Rules
- Verify `.gitignore` contains rules matching:
  - `.env`
  - `.env.*` (excluding explicit non-secret sample files like `.env.example`)
  - `*.pem`, `*.key`, `*.pfx`, `*.p12`
  - Credentials stores and local database dumps.
- Confirm `.env.example` contains only placeholder values, zero actual secrets.

### 3.3 CI/CD & Container Configuration Audit
- Inspect CI/CD workflows (`.github/workflows/*.yml`, pipeline manifests):
  - Secrets injected exclusively via pipeline secret managers, never echoed into build steps or build logs.
  - Pull request triggers restricted to prevent exfiltration via untrusted forks (`pull_request_target` usage verified).
- Inspect container configurations (`Dockerfile`, `docker-compose.yml`):
  - No secrets embedded via `ENV` or `ARG` directives.
  - Multi-stage builds clean build-time credentials from final runtime images.
  - Containers execute as non-root users (`USER appuser`).
  - Base images pinned to specific version digests.

---

## 4. Phase 3: Auth & Authorization Review

### 4.1 Role-Based Access Control (RBAC)
- Enforce explicit authorization checks on every sensitive endpoint.
- Verify default-deny architecture: endpoints reject requests unless explicit permit rule matches.
- Test horizontal privilege boundaries: user A cannot view, mutate, or delete resources owned by user B.
- Test vertical privilege boundaries: regular users cannot invoke administrative mutations or modify role assignments.

### 4.2 Principle of Least Privilege
- Service accounts, database users, and third-party API clients hold minimum required permissions.
- Database access restricts DDL execution (`CREATE`, `DROP`, `ALTER`) to migration scripts; application connections use DML only (`SELECT`, `INSERT`, `UPDATE`, `DELETE`).
- Token scopes constrain actions to specific resources and HTTP verbs.

### 4.3 Session & Token Lifecycle
- Access tokens configured with short time-to-live (TTL â‰¤ 15 minutes).
- Refresh tokens use secure storage, rotation mechanisms, and family invalidation upon replay detection.
- Cookies configured with mandatory security attributes:
  - `HttpOnly`: prevents client-side script access.
  - `Secure`: enforces transmission exclusively over HTTPS.
  - `SameSite=Strict` (or `Lax` where cross-site top-level navigation requires): mitigates CSRF attacks.
- Session termination purges tokens from server-side cache/storage and clears client-side cookies.

### 4.4 Multi-Factor Authentication (MFA)
- Privilege elevation, password resets, email modifications, and financial actions require step-up authentication.
- TOTP implementations enforce replay rejection for previously accepted codes within current time window.

---

## 5. Phase 4: Data Security & Privacy

### 5.1 PII/PHI Log Masking
- Audit all log statements, tracing hooks, and error reporters (`console.log`, `logger.info`, `logger.error`, Sentry events).
- Zero PII/PHI in application logs:
  - Passwords, passphrases, PINs.
  - Credit card numbers, CVVs, expiration dates.
  - Social Security Numbers, national IDs.
  - Personal health data, biometric identifiers.
  - Session tokens, bearer tokens, API keys.
- Masking filters must redact values before emission (`user.email` -> `u***@example.com`).

### 5.2 Encryption in Transit
- Enforce TLS 1.2 minimum, TLS 1.3 preferred, for all external and inter-service communications.
- Verify Strict-Transport-Security (HSTS) header present with `max-age â‰¥ 31536000; includeSubDomains`.
- Enforce TLS certificate validation: reject configurations disabling certificate verification (`rejectUnauthorized: false`, `verify=False`).

### 5.3 Encryption at Rest
- Sensitive database columns (credentials, financial details, PII) store encrypted values using AES-256-GCM.
- Key management uses dedicated KMS (AWS KMS, GCP KMS, Vault) with key rotation policies.
- Database storage volumes and backups enforce filesystem-level encryption.

### 5.4 Audit Trail
- Sensitive mutations require immutable, append-only audit records.
- Audit records must capture:
  - Timestamp (UTC ISO 8601).
  - Actor identifier (authenticated user ID, service account ID).
  - Client IP address and User-Agent.
  - Action performed (`CREATE`, `UPDATE`, `DELETE`, `ROLE_CHANGE`).
  - Target resource identifier.
  - Outcome (`SUCCESS`, `FAILURE`, `DENIED`).
- Audit tables prevent modifications and deletions via database permission controls or WORM storage.

---

## 6. Phase 5: Frontend Security & UI Compliance

### 6.1 Accessibility Standards (WCAG 2.1 AA)
- **Text Alternatives:** All non-decorative `<img>` elements declare descriptive `alt` attributes. Decorative images declare `alt=""` and `aria-hidden="true"`.
- **Color Contrast:** Text and images of text maintain minimum contrast ratio:
  - Normal text: â‰¥ 4.5:1 against background.
  - Large text (â‰¥ 18pt or â‰¥ 14pt bold): â‰¥ 3:1 against background.
  - UI components and graphical objects: â‰¥ 3:1 contrast against adjacent colors.
- **Keyboard Navigation:**
  - All interactive elements reachable and operable via keyboard (`Tab`, `Shift+Tab`, `Enter`, `Space`, arrow keys).
  - Visible focus indicators present across all focusable elements (`outline` styles preserved).
  - Modals trap focus during display and restore focus to trigger element upon closing with `Escape`.
- **Semantics:** HTML uses semantic elements (`<main>`, `<nav>`, `<header>`, `<button>`, `<dialog>`). Form controls bind to `<label>` elements via `for`/`id` associations.

### 6.2 Legal & Privacy Compliance
- **Consent Mechanisms:** Verify cookie consent banners present prior to setting non-essential cookies or initializing analytics scripts.
- **Dark Pattern Prevention:**
  - Prohibit hidden subscription recurring charges, pre-selected consent checkboxes, obscured cancel paths, and misleading countdown timers.
  - Opt-out paths require equal prominence and click count as opt-in paths.
- **Data Subject Rights:** Verify availability of self-service data export and deletion endpoints matching GDPR/CCPA requirements.

### 6.3 Anti-Vibe-Coding UI Verification
- **Loading States:** Every asynchronous request, data fetch, and mutation displays explicit loading feedback (spinner, skeleton loader, disabled button state).
- **Error Boundaries:** Frontend implements error boundaries to prevent application crashes on component failures. User-facing error messages provide clear recovery instructions without technical jargon.
- **Interactive Completeness:** Every visible button, tab, link, menu item, and input binds to active, tested event handlers. Dead click targets or placeholder UI stubs fail review.
- **Responsive Layout:** Views render cleanly across standard viewports (375px mobile, 768px tablet, 1440px desktop) without unintentional horizontal scrolling, overlapping text, or truncated action buttons.

---

## 7. Findings Format & Severity Taxonomy

All findings use standardized classification and structured reporting.

### 7.1 Severity Matrix
| Severity | Definition | Merge Impact | Examples |
|---|---|---|---|
| **CRITICAL** | Remotely exploitable vulnerability leading to system compromise, data breach, or authentication bypass. | **Blocks Merge** | SQL injection, hardcoded API secret, unauthenticated admin route, RCE. |
| **HIGH** | Significant security failure with constrained exploitability, broken authorization, or data leak. | **Blocks Merge** | Missing IDOR check, stored XSS, unauthenticated password change, PII in logs. |
| **MEDIUM** | Real vulnerability requiring specific conditions or elevated prerequisites to trigger. | **Blocks Merge** (unless documented exception) | Reflected XSS, CSRF on non-critical mutation, missing rate limit, overly verbose error. |
| **LOW** | Hardening gap, defensive deficiency, or security test omission. | Permitted with tracked task | Missing security response header, minor WCAG contrast deviation, weak cipher in fallback list. |
| **INFORMATIONAL** | Architectural note, code cleanup suggestion, or dependency update suggestion. | Permitted | Security documentation gap, library upgrade recommendation. |

### 7.2 Finding Structure
Every reported finding must follow this exact template:

```markdown
### [SEVERITY] Finding Title
- **Vulnerability Class:** OWASP category / Security domain
- **Location:** `path/to/file.ext:line_number`
- **Description:** Concise explanation of the vulnerability and attack vector.
- **Impact:** Technical and data blast radius if exploited.
- **Remediation:** Actionable code diff or exact configuration change required.
```

---

## 8. Pre-Merge Security Checklist

Before issuing a final verdict, verify every item on this mandatory checklist.

- [ ] **No Hardcoded Secrets:** Zero API keys, tokens, passwords, or private keys in code or commit history.
- [ ] **Environment Exclusion:** `.env` and credential files explicitly excluded in `.gitignore`.
- [ ] **No Injection Vectors:** All SQL, shell commands, templates, and outbound HTTP requests use parameterization or allowlists.
- [ ] **Input Validation:** Strict validation and sanitization implemented on all entry points (REST, GraphQL, WebSockets, CLI).
- [ ] **Access Control:** RBAC enforced on every sensitive endpoint with default-deny semantics.
- [ ] **Error Handling:** Errors log internal diagnostics securely; zero stack traces, SQL syntax, or private IPs returned to clients.
- [ ] **Resilient API Calls:** All external API calls wrapped in `try/catch` or error-handling blocks with safe fallbacks.
- [ ] **Cryptographic Standards:** Modern algorithms (AES-256-GCM, Argon2id, SHA-256+) and secure RNGs enforced.
- [ ] **Privacy & Logging:** PII/PHI redacted from all logs, metrics, and application traces.
- [ ] **Transit & Storage Security:** TLS 1.2+ enforced with HSTS; sensitive data at rest encrypted.
- [ ] **Accessibility:** WCAG 2.1 AA satisfied (contrast â‰¥ 4.5:1, keyboard operable, descriptive `alt` tags, valid ARIA).
- [ ] **UI Compliance:** Loading states active, error boundaries in place, all interactive elements wired, responsive layout verified.
- [ ] **Abuse Testing:** Automated tests cover negative test cases, unauthorized access attempts, and malformed inputs.
- [ ] **Regression Free:** Existing security controls, headers, and permissions remain functional.

---

## 9. Verdict & Handoff Protocol

Post final audit evaluation directly to `workspace .agents/comm.md`.

### 9.1 Passed Verdict
**Conditions:** 0 Critical findings, 0 High findings, 0 unaddressed Medium findings, 100% pre-merge checklist items verified.

Post message to `workspace .agents/comm.md`:
```markdown
[FROM: Security_Auditor] -> [TO: Orchestrator]
[STATUS: Passed]
[BODY: Security audit passed for task <Task_ID>. Verified OWASP Top 10, Secrets, Auth/RBAC, Data Privacy, and Frontend Compliance. 0 Critical, 0 High, 0 Medium findings. Pre-merge security gate cleared. Ready for merge.]
```

**Next Action:** Orchestrator updates task status to `Complete` in `workspace .agents/tasks.md`.

### 9.2 Failed Verdict
**Conditions:** â‰¥1 Critical finding, â‰¥1 High finding, or unaddressed Medium findings.

Post message to `workspace .agents/comm.md`:
```markdown
[FROM: Security_Auditor] -> [TO: Coder]
[STATUS: Failed]
[BODY: Security audit failed for task <Task_ID>. Blocking security findings identified:
- [CRITICAL/HIGH] <Finding Title> at `path/to/file.ext:line`. Remediation: <Exact required fix>.
Task rejected. Remediate findings and resubmit for Logic Audit and Security Audit.]
```

**Next Action:** Coder claims task, implements fixes in `workspace .agents/tasks.md`, and resubmits to Logic Auditor.



