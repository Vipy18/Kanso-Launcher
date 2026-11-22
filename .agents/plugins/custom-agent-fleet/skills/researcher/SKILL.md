---
name: researcher
description: >-
  Workflow and operating procedure for the researcher agent. Activate this skill when delegating tasks to the researcher agent or when acting as the researcher.
---

# Workflow: Research-First Content Generation

## 1. Overview
When tasked with generating knowledge deliverables (reports, specifications, spreadsheets, decks), agents must decouple the *gathering* of facts from the *formatting* of the output, heavily inspired by Perplexity's deep research methodology.

## 2. Phase 1: Deep Fact-Gathering
- **The Mandate:** Model priors are stale. Assume all API signatures, package versions, and external facts have changed.
- **Execution:**
  1. Use `search_web` and `read_url_content` to retrieve primary sources.
  2. For codebase context, use `codegraph_explore` and `ingest_repository` before guessing file locations.
  3. Execute searches in parallel batches (up to 3 distinct queries).
- **Prohibition:** Do NOT load or read formatting skills (e.g., docx/pdf generation) during this phase.

## 3. Phase 2: Triangulation & Verification
- Cross-reference critical claims (pricing, CVEs, breaking changes) against at least two independent primary sources.
- Prioritize official documentation domains over secondary aggregators or SEO spam.
- If sources conflict, document both and evaluate authority.

## 4. Phase 3: Synthesis & Deliverable Execution
- Once all substantive data is secured and verified, read the required formatting skill.
- Apply the data to the format.
- **Citation Requirement:** Inject inline citations immediately following every factual claim `[Source](URL)`. Never relegate citations to a disconnected bibliography.



