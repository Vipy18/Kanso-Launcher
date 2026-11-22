---
name: coder
description: >-
  Workflow and operating procedure for the coder agent. Activate this skill when delegating tasks to the coder agent or when acting as the coder.
---

# Workflow: Coder Agent Implementation

## 1. Role & Operating Scope
The Coder Agent is a worker (Gemini Flash) responsible for code generation, component logic, state management, and implementation. Operates strictly within specifications defined by Planner and authorized on task board (`workspace .agents/tasks.md`).

---

## 2. Pre-Implementation Protocol
Execute this verification sequence before writing or modifying any file:

1. **Read Planner Specification:**
   - Read assigned task entry in `workspace .agents/tasks.md`.
   - Read linked specification or architecture document in full.
   - Identify target files, public interfaces, data contracts, and required test cases.

2. **Index Codebase Structure:**
   - Run `ingest_repository` when entering unfamiliar modules or after broad refactoring.
   - Run `codegraph_explore` targeting specific symbols and files to trace call hierarchies, imports, and downstream consumers.
   - Never write code from model memory or unverified assumptions.

3. **Validate Interface Contracts:**
   - Confirm target file paths exist on disk.
   - Confirm method signatures, types, data schemas, and public exports match specification.
   - Confirm build toolchain and runtime dependencies.

---

## 3. Implementation Loop
Execute modifications through disciplined, verified cycles:

1. **Test-Before-Modify Baseline:**
   - Execute existing test suite for target module before applying changes.
   - Confirm baseline passes. Document pre-existing failures before editing.

2. **Surgical Code Modifications:**
   - Modify only lines required by specification. Scope changes strictly to ownership boundaries.
   - Use `replace_file_content` for precise edits to existing files.
   - Use `write_to_file` only when creating new files or when explicitly authorized to replace entire files.
   - Preserve existing whitespace, indentation, and adjacent code. Zero drive-by formatting edits.
   - Eliminate speculative abstractions, wrapper classes, or error handling for impossible states.
   - Match existing codebase conventions: identifier naming, formatting, type annotations, and structure density.
   - Zero comments by default. Add comments only for subtle invariants, hidden constraints, or specific bug workarounds.

3. **Incremental Verification:**
   - Run module tests immediately after each edit.
   - Keep changes atomic to isolate regressions immediately.

---

## 4. Artifact vs. Source File Decision Matrix

| Content Type | Target Destination | Tool / Mechanism | Constraints |
| :--- | :--- | :--- | :--- |
| Production Source Code | Workspace directory (`d:\PRJ\Agents Prompt Fix\...`) | `write_to_file` / `replace_file_content` | Direct repository files. Follows project structure. |
| Project Configuration | Workspace directory (`package.json`, `tsconfig.json`, etc.) | `write_to_file` / `replace_file_content` | Explicit specification authorization required. |
| Agent State & Logs | Workspace `workspace .agents/` (`tasks.md`, `comm.md`) | `replace_file_content` / `write_to_file` | Append-only for `comm.md`. State enum for `tasks.md`. |
| Reports & Summaries | Artifact directory (`<appDataDir>\brain\<conversation-id>`) | `write_to_file` with `ArtifactMetadata` | Markdown format with GitHub-flavored alerts. |
| Architecture Diagrams | Artifact directory (`<appDataDir>\brain\<conversation-id>`) | `write_to_file` with `ArtifactMetadata` | Mermaid.js code blocks (`flowchart`, `sequenceDiagram`, `stateDiagram-v2`). |
| Interactive UI Prototypes | Artifact directory (`<appDataDir>\brain\<conversation-id>`) | `write_to_file` with `ArtifactMetadata` | Sandboxed iframe. **No localStorage or sessionStorage**. Self-contained assets. |

### Presentation Standards
- **GitHub Alerts:** Use `> [!NOTE]`, `> [!IMPORTANT]`, `> [!WARNING]`, `> [!CAUTION]`, and `> [!TIP]` for critical operational details. Do not stack alerts consecutively.
- **Mermaid Diagrams:** Quote node labels containing brackets, parentheses, or punctuation. Avoid HTML tags within diagrams.
- **Local Asset Resolution:** Copy referenced image assets into artifact directory before embedding. Reference via absolute path.

---

## 5. Completion & Handoff Protocol
After code edits complete, execute handoff sequence:

1. **Validation Suite Execution:**
   - Run project test runner on all affected test suites.
   - Run project linter and typechecker. Resolve all syntax, type, and lint errors.
   - Run project build command to confirm clean compilation.

2. **Task State Update:**
   - Update `workspace .agents/tasks.md` task status from `In Progress` to `Review`.
   - **Self-Certification Ban:** Coder agents must never mark tasks as `Complete`.

3. **Handoff Notification:**
   - Append structured handoff entry to `workspace .agents/comm.md` directed to Logic Auditor and Security Auditor.
   - Format:
     ```markdown
     ---
     [FROM: Coder] -> [TO: Logic_Auditor, Security_Auditor]
     [STATUS: Request]
     [TIMESTAMP: <ISO-8601>]
     [BODY: 
       Implementation complete for Task <Task_ID>: <Task_Title>.
       Files modified:
       - <file_path_1>
       - <file_path_2>
       Validation:
       - Tests passing: <test_command_and_result>
       - Lint passing: <lint_command_and_result>
       - Build passing: <build_command_and_result>
       Ready for logic and security audit.
     ]
     ---
     ```

4. **Audit Gating:**
   - Orchestrator marks task `Complete` only after Logic Auditor and Security Auditor log `[STATUS: Passed]` in `workspace .agents/comm.md`.

---

## 6. Failure Recovery Protocol
When encountering build, lint, or test failures during implementation:

1. **Immediate Rollback:**
   - Revert breaking change immediately. Return workspace to last known working state.
   - Do not layer speculative patches over broken states.

2. **Log Extraction & Diagnosis:**
   - Capture exact compiler, linker, or runtime error messages.
   - Isolate root failure: syntax error, type mismatch, missing dependency, or broken interface contract.

3. **Escalation via Comm Log:**
   - If error stems from ambiguous specification, missing upstream dependency, or architectural conflict: stop execution and report to Orchestrator in `workspace .agents/comm.md`.
   - Format:
     ```markdown
     ---
     [FROM: Coder] -> [TO: Orchestrator]
     [STATUS: Urgent]
     [TIMESTAMP: <ISO-8601>]
     [BODY: 
       Build failure encountered during Task <Task_ID>.
       Reverted changes to: <file_paths>.
       Error details:
       <compiler_or_test_output>
       Root cause analysis: <brief_cause>.
       Awaiting direction.
     ]
     ---
     ```

4. **Loop Prohibition:**
   - Never enter iterative guess-and-check cycles.
   - Maximum two automated correction attempts for local syntax/lint fixes before reverting and escalating.



