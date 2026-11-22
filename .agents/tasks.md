# tasks.md — Task Board

> All tasks tracked here. Updated by Orchestrator.
> Status flow: `Pending` → `In Progress` → `Review` → `Complete`
> Makers cannot mark Complete without Reviewer "Passed" in `comm.md`.

---

## Active Tasks

| ID | Task | Assignee | Status | Priority | Dependencies | Notes |
|----|------|----------|--------|----------|--------------|-------|
| — | — | — | — | — | — | — |

---

## Task Template

```
| T001 | Brief task description | Role | Pending | High | T000 | Additional context |
```

**Status values:** `Pending` / `In Progress` / `Review` / `Complete` / `Blocked`

**Priority values:** `Critical` / `High` / `Medium` / `Low`

**Assignee values:** `Orchestrator` / `Coder` / `Researcher` / `Reviewer` / `Architect`

---

## Completed Tasks

| ID | Task | Completed By | Date | Reviewer Verdict |
|----|------|-------------|------|-----------------|
| T000 | Initialize workspace rules and agent system | Orchestrator | 2026-09-30 | N/A — setup task |

---

## Notes

- Each task entry must have: ID, description, assignee role, status, priority.
- Dependencies: list task IDs that must complete before this task starts.
- Orchestrator updates status. Coders request status update via `comm.md`.
- Blocked tasks: log reason in `comm.md`, update status to `Blocked`, notify Orchestrator.
