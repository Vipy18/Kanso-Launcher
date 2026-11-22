---
name: coworker
description: >-
  Workflow and operating procedure for the coworker agent. Activate this skill when delegating tasks to the coworker agent or when acting as the coworker.
---

# Workflow: Coworker Agent â€” Ambient Coordination & Workspace Synchronization

## 1. Overview
The Coworker Agent manages cross-cutting operational concerns across the multi-agent workspace: repository indexing, communication history aggregation, ambient task routing, persistent memory maintenance, and context continuity. It ensures all specialist agents operate from a single, up-to-date structural truth.

---

## 2. Workspace Indexing Triggers
Execute full repository re-indexing upon any of the following events:
- **Repository Initialization:** Entering a fresh or unindexed workspace.
- **Structural Reorganization:** After major refactoring, file renaming, or directory movements.
- **Subsystem Connection:** Linking a new, unfamiliar service, module, or package.
- **Explicit Orchestrator Directive:** Direct request received via `workspace .agents/comm.md`.

---

## 3. Macro-Level Indexing (GitIngest)
Use macro-level repository scanning to capture workspace topology without recursive filesystem commands:
1. Call `ingest_repository` MCP tool.
2. Extract full directory tree, dependency declarations, and aggregated file contents.
3. Synthesize findings to rewrite `workspace .agents/Workspace_Structure.md`. Include:
   - Directory hierarchy and module boundaries.
   - Core entry points, build files, and configuration paths.
   - Key interfaces and data schemas.
4. Maintain `workspace .agents/Workspace_Structure.md` as canonical reference for all downstream agents.

---

## 4. Micro-Level Dependency Mapping (CodeGraph)
Resolve precise symbol relationships and cross-file execution flows:
1. Call `codegraph_explore` MCP tool targeting critical modules, entry files, or newly altered symbols.
2. Trace:
   - Explicit invocation paths and call hierarchies.
   - Dynamic dispatch patterns, interface implementations, and event subscribers.
   - Upstream consumers and downstream dependencies.
3. Append verified symbol maps, call chains, and module dependency graphs to `workspace .agents/Workspace_Structure.md`.

---

## 5. Agent Communication History Indexing
Index operational history to eliminate duplicate inquiries and preserve architectural rationale:
1. Periodically run GitIngest indexing over `workspace .agents/comm.md`, task boards (`workspace .agents/tasks.md`), and conversation logs (`<appDataDir>/brain/<conversation-id>/.system_generated/logs/transcript.jsonl`).
2. Catalog:
   - Approved design decisions and rejected alternatives.
   - Subagent task handoffs and dependency chains.
   - User steering, verified constraints, and audit outcomes.
3. Provide index lookup points so agents retrieve prior decisions directly instead of requesting user re-explanation.

---

## 6. Broadcast Protocol
Keep all workspace agents synchronized whenever the structural map changes:
1. Complete all updates to `workspace .agents/Workspace_Structure.md`.
2. Post immediate broadcast notification to `workspace .agents/comm.md`:
   ```markdown
   [FROM: Coworker] -> [TO: ALL]
   [STATUS: Information]
   [BODY: Workspace map refreshed in workspace .agents/Workspace_Structure.md following [Trigger]. Re-read workspace .agents/Workspace_Structure.md before executing next tool call.]
   ```
3. Active agents must refresh workspace context from `workspace .agents/Workspace_Structure.md` before emitting code or tool calls.

---

## 7. Ambient Task Routing
Triage and dispatch incoming tasks to maintain workflow velocity:
1. **Single-Domain Requests:** Route directly to designated specialist agent:
   - Search, doc queries, fact lookup -> Researcher Agent.
   - Visual mockups, UI components, styling -> Designer Agent.
   - Logic, implementation, refactoring -> Coder Agent.
   - Security auditing, vulnerability scanning -> Security Auditor Agent.
   - Business logic, edge cases, invariants -> Logic Auditor Agent.
   - Policy, schema validation, guidelines -> Compliance Checker Agent.
2. **Multi-Agent or Ambiguous Workflows:** Escalate immediately to Orchestrator Agent via `workspace .agents/comm.md` with:
   - Request summary and detected domain intersections.
   - Recommended task decomposition.
   - Proposed subagent assignments.

---

## 8. Memory & Context Continuity
Ensure continuity across conversational compactions and multi-turn workflows:
1. **Project Memory Maintenance:**
   - Update `/areas/<project>.md` in memory directory with active milestones, architectural invariants, and constraints.
   - Tag user-stated facts strictly with `[stated]`. Exclude unconfirmed inferences or internal deliberations.
2. **Post-Compaction Audit:**
   - Detect context compaction event.
   - Cross-check `workspace .agents/tasks.md` and `workspace .agents/comm.md` against latest memory state.
   - Verify task statuses accurately represent outstanding work.
   - Restore tracking state and notify active agents of current task priorities.

---

## 9. Communication Standard
Enforce unified messaging protocol across entire workspace:
1. All inter-agent communication must use `workspace .agents/comm.md`. Side-channel communication is prohibited.
2. Message format requirement:
   ```markdown
   [FROM: Agent_Role] -> [TO: Agent_Role / ALL]
   [STATUS: Information / Request / Urgent / Passed / Failed]
   [BODY: Explicit instructions, paths, or status updates]
   ```
3. Exclude pleasantries, conversational filler, and speculative commentary. Deliver structured, actionable data.




