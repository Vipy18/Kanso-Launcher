# Antigravity_Setup.md — Conversation Summary

**Conversation:** Antigravity Setup (ID: 8c4fb715-d4fd-48a0-a466-3e90cb8dd0ee)
**Date:** 2026-09-30
**Agent Role:** Orchestrator

---

## What Was Done

Created a complete AGY-native rules and skills system for the `d:/PRJ/Agents Prompt Fix` workspace by analyzing leaked system prompts from Anthropic (Claude Opus 4.6, Claude Code), OpenAI (Codex, Plan Mode), and Perplexity (deep-research, perplexity-ai).

### Source Analysis Summary

| Source | Key Patterns Extracted |
|--------|----------------------|
| Claude Opus 4.6 | Act-before-clarify, capability check before refusing, memory boundaries, tone rules |
| Claude Code (Opus 4.6) | Engineering judgment, reversibility protocol, 4-type memory system, end-to-end completion |
| Codex Full | Frontend/backend guidance, autonomy, parallel tool use, no premature abstraction |
| Codex Plan Mode | 3-phase planning (explore → intent → implementation), non-mutating phase separation, decision-complete spec |
| Perplexity Deep Research | Skill activation pattern, parallel search, cross-validation, inline citation discipline |
| Perplexity AI | Response formatting, headers, lists, follow-up protocol |

### Files Created

**Root rules:**
- `AGENTS.md` — always-on workspace behavioral contract

**Agent system (`Agents/`):**
- `agent_roles.md` — role registry (Orchestrator, Coder, Researcher, Reviewer, Architect)
- `Agents_Rules.md` — universal + per-role behavioral contracts
- `Workspace_Structure.md` — directory conventions
- `tasks.md` — task board
- `comm.md` — inter-agent communication log
- `Antigravity_Setup.md` — this file

**Skills (`.agents/skills/`):**
- `deep-research/SKILL.md` — Perplexity-inspired: parallel search, cross-validate, cite inline
- `plan-mode/SKILL.md` — Codex-inspired: 3-phase, non-mutating, decision-complete
- `code-review/SKILL.md` — Claude Code-inspired: security-first, severity-ordered, file:line refs
- `multi-agent-orchestration/SKILL.md` — full coordination protocol: decompose → delegate → review gate
- `memory-log/SKILL.md` — Claude Code memory types: user/feedback/project/reference

---

## Design Decisions

1. **AGENTS.md at workspace root** — AGY auto-loads this as always-on rules. No global config changes.
2. **Skills over always-on rules for workflows** — progressive disclosure prevents context bloat; skills only load when triggered.
3. **5 roles** (Orchestrator, Coder, Researcher, Reviewer, Architect) — broader than teamwork-preview's 3 to cover design and research phases explicitly.
4. **Reviewer gate enforced structurally** — tasks.md + comm.md protocol makes it impossible to mark Complete without Passed verdict.
5. **Perplexity citation style** — applied to research-type tasks only (deep-research skill), not universally.
6. **All existing global rules preserved** — AGENTS.md extends, not replaces, the global user rules.

---

## How to Use the System

**Open workspace in AGY:** `AGENTS.md` auto-loads. All behavioral rules active.

**Trigger skills:**
- Type "research [topic]" → `deep-research` activates
- Type "/plan [task]" → `plan-mode` activates
- Type "review this" → `code-review` activates
- Type "orchestrate" or "/teamwork" → `multi-agent-orchestration` activates
- Type "remember this" → `memory-log` activates

**Multi-agent flow:**
1. Orchestrator reads `tasks.md` + `comm.md`
2. Delegates to roles via `comm.md`
3. Coders implement, log completion in `comm.md`
4. Reviewer audits, logs Passed/Failed in `comm.md`
5. Orchestrator marks Complete in `tasks.md`
