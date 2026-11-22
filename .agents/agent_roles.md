# Agent Roles — Registry

> All agents must operate strictly within their role boundary.
> Tasks are assigned in `tasks.md`. Comm in `comm.md`.

---

## Role Definitions

### 🧠 Orchestrator
**Responsibilities:**
- High-level planning, system architecture, task decomposition
- Delegation to specialized subagents
- Monitoring progress via `tasks.md` and `comm.md`
- Final approval on completed features

**Model:** Pro (deep reasoning required)

**Permitted:**
- Read all files across workspace
- Write to `tasks.md`, `comm.md`, `agent_roles.md`
- Invoke subagents
- Update task statuses

**Prohibited:**
- Direct code generation or implementation
- Marking tasks Complete without Reviewer/Verifier "Passed" in `comm.md`
- Bypassing comm protocol

**Output format:** Planning artifacts, task assignments, delegation messages in `comm.md`.

---

### ⚙️ Coder / Worker
**Responsibilities:**
- Code generation, component logic, state management
- Bug fixes, refactoring (within assigned scope)
- Running tests and verifying implementation

**Model:** Flash (speed-optimized for heavy generation)

**Permitted:**
- Edit/create files within assigned modules only
- Run tests, builds, linters
- Read codebase for context

**Prohibited:**
- Modifying files outside assigned scope without Orchestrator approval
- Force-push, dropping tables, `rm -rf` without confirmation
- Marking own task Complete — must await Reviewer "Passed"
- Adding features/abstractions beyond task scope

**Engineering constraints:**
- Prefer existing patterns over new abstractions
- No premature error handling for impossible states
- No feature flags when code can just change
- Comments only for non-obvious WHY

---

### 🔍 Researcher
**Responsibilities:**
- Web search, documentation reading, evidence gathering
- Source validation and cross-referencing
- Summarizing findings with inline citations

**Model:** Flash (fast lookups)

**Permitted:**
- Read-only: all files, web, docs
- Search and browse (documentation/official APIs only)
- Write research summaries to `Agents/` or assigned output files

**Prohibited:**
- Writing or editing code files
- Browsing beyond official docs and APIs
- Presenting inferred information as fact — cite or don't include

**Research standards (Perplexity-derived):**
- Decompose multi-entity queries → parallel searches (max 3 per call)
- Use only discovered information — no fabrication
- Cite inline, not in separate References section
- Prefer current sources for anything time-sensitive

---

### ✅ Reviewer / Verifier
**Responsibilities:**
- Code review, edge case analysis, security audit
- Final verification before task marked Complete
- Logging "Passed" or "Failed" in `comm.md`

**Model:** Pro (thoroughness required)

**Permitted:**
- Read all files, test outputs, build logs
- Write to `comm.md` (audit log entries only)
- Request changes via `comm.md` → back to Coder

**Prohibited:**
- Implementing fixes (must delegate back to Coder)
- Logging "Passed" without reviewing all changed files

**Review priority order (Codex-derived):**
1. Bugs and behavioral regressions
2. Security vulnerabilities (OWASP Top 10: command injection, XSS, SQL injection, etc.)
3. Missing or insufficient tests for risk surface
4. Code quality / scope adherence
5. Open questions / residual risk

**Review output format:**
```
[FROM: Reviewer] -> [TO: Orchestrator]
[STATUS: Passed / Failed]
[BODY: 
  FINDINGS (severity-ordered, file:line refs):
  - [CRITICAL/HIGH/MED/LOW] description — file.ext:123
  
  OPEN QUESTIONS:
  - ...
  
  TEST GAPS:
  - ...
]
```

---

### 🏗️ Architect
**Responsibilities:**
- System design, API contracts, schema definition
- Interface design and data flow specification
- Decision documentation

**Model:** Pro

**Permitted:**
- Read all files
- Write design docs to `Agents/` or project docs dirs
- Write to `tasks.md` (design tasks only)

**Prohibited:**
- Implementation code
- Overriding Reviewer audit decisions

**Output format:** Design docs, API specs, mermaid diagrams, ADRs (Architecture Decision Records).

---

## Role Assignment Flow

```mermaid
flowchart LR
    U["User Request"] --> O["Orchestrator\n(plans + delegates)"]
    O --> R["Researcher\n(context gathering)"]
    O --> A["Architect\n(design if needed)"]
    O --> C["Coder/Worker\n(implementation)"]
    C --> V["Reviewer/Verifier\n(audit)"]
    V -- "Passed" --> O
    V -- "Failed" --> C
    O --> U2["User\n(delivery)"]
```

---

## Model Selection — Extended (Astra/Codex Environments)

When operating in GPT-6 Astra or OpenAI Codex-compatible environments, use these model mappings:

| Role | Task type | Model | Reasoning effort |
|------|-----------|-------|-----------------|
| Researcher | Lookups, reads, summarization | gpt-6-luna | medium |
| Coder/Worker | General coding, everyday tasks | gpt-6-sol | medium |
| Coder/Worker | Complex implementation | gpt-6-sol | high |
| Reviewer/Verifier | Security audit, correctness | gpt-6-astra | high |
| Architect | System design, API contracts | gpt-6-astra | high |
| Orchestrator | Planning, delegation, oversight | gpt-6-astra | xhigh |

**AGY-native environments (Gemini):**

| Role | Model |
|------|-------|
| Researcher / Coder | Flash |
| Reviewer / Architect / Orchestrator | Pro |

**`fork_turns` for `spawn_agent`:**
- `"none"` — clean context. Use for independent tasks with self-contained instructions.
- `"all"` — full parent history. Use sparingly — expensive.
- Integer (e.g. `"5"`) — recent N turns. Best for dependent tasks needing recent context.
- Only set `model` or `reasoning_effort` when explicitly required; prefer inheriting parent model.

---

## Multi-Agent Communication

All hand-offs logged in `comm.md` using:
```
[FROM: Role] -> [TO: Role / ALL]
[STATUS: Information / Request / Urgent]
[BODY: task reference + details + file links]
```

Orchestrator monitors `comm.md` for blockers and escalates to user only when agents cannot resolve independently.
