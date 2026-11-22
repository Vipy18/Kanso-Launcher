# Antigravity Multi-Agent Fleet: Comprehensive Custom Agents & Skill Mappings

This configuration records all specialized Antigravity custom agents, their assigned skills, and required MCP server tooling across all engineering, security, and multimedia domains.

---

## Complete Role & Capability Routing Matrix

| Domain | Subagent Name | Core Mandate | Assigned Skills | Mandated MCP Servers |
| :--- | :--- | :--- | :--- | :--- |
| **RAG & Vector Retrieval** | `rag_engineer` | Vector search, semantic chunking, embedding models, vector index tuning | `rag-engineer`, `rag-implementation`, `vector-database-engineer`, `vector-index-tuning`, `embedding-strategies`, `context7-auto-research`, `python-pro` | **google-developer-knowledge**, **Notion MCP** |
| **Reverse Engineering & Protocol Analysis** | `reverse_engineer` | Binary decompilation, control flow recovery, protocol extraction | `reverse-engineer`, `protocol-reverse-engineering`, `binary-analysis-patterns`, `arm-cortex-expert`, `firmware-analyst`, `wireshark-analysis` | **codegraph**, network analysis tools |
| **Penetration Testing & Defensive Security** | `penetration_tester` | Attack surface mapping, API fuzzing, cloud & web pentest checklists | `pentest-checklist`, `pentest-commands`, `burp-suite-testing`, `cloud-penetration-testing`, `aws-penetration-testing`, `sqlmap-database-pentesting`, `api-fuzzing-bug-bounty`, `vulnerability-scanner`, `stride-analysis-patterns`, `threat-modeling-expert`, `api-security-best-practices` | **codegraph** (taint flow analysis) |
| **Systems & Polyglot Programming** | `systems_programmer` / `coder` | High-performance backend, clean code, multi-language engineering | `clean-code`, `python-pro`, `typescript-pro`, `rust-pro`, `golang-pro`, `cpp-pro`, `systems-programming-rust-project`, `backend-architect`, `senior-fullstack`, `test-driven-development`, `systematic-debugging`, `executing-plans`, `coder` | **github-mcp-server**, native build/test CLI tools |
| **UI/UX & Design Systems** | `designer` | Vibe-Coded UI, 20-point anti-lawsuit compliance, design systems | `ui-ux-pro-max`, `ui-ux-designer`, `frontend-design`, `stitch-ui-design`, `tailwind-design-system`, `web-design-guidelines`, `radix-ui-design-system`, `wcag-audit-patterns`, `designer`, `generative_ui`, `redesign-screen`, `search`, `best-practices` | **StitchMCP** (`generate_screen_from_text`, `create_design_system`, `edit_screens`), **Mobbin** (`search_screens`, `search_flows`) |
| **Multimedia, 2D/3D & Motion** | `multimedia_creative_designer` | Code-driven video, motion graphics, Three.js 3D, generative canvas | `3d-web-experience`, `threejs-skills`, `remotion-best-practices`, `audio-transcriber`, `fal-audio`, `canvas-design`, `algorithmic-art`, `web-artifacts-builder`, `figma-automation`, `ai-video-editor`, `faceless-shorts` | **StitchMCP**, **Mobbin** |
| **Embedded & Hardware Engineering** | `embedded_hardware_engineer` / `pcb_designer` | Schematics, PCB routing, DRC/ERC, ARM Cortex firmware | `arm-cortex-expert`, `firmware-analyst`, `pcb_designer`, `agy-customizations`, `systematic-debugging` | **konnect** (`open_schematic_viewer`, `add_design_rule`), **kicad-mcp-server** (`run_drc`, `run_erc`, `auto_route`, `export_gerber`, `export_bom`), **klipper-hardware**, **klipper-docs** |
| **Cybersecurity & SAST Auditing** | `cybersecurity_specialist` / `security_auditor` | SAST, threat modeling, secret leakage, OWASP Top 10 | `security-scanning-security-sast`, `threat-modeling-expert`, `vulnerability-scanner`, `production-code-audit`, `api-security-best-practices`, `reverse-engineer`, `binary-analysis-patterns`, `stride-analysis-patterns`, `wireshark-analysis`, `security_auditor`, `verification-before-completion` | **codegraph** (taint propagation & call graphs) |
| **Logic & Invariant Audits** | `logic_auditor` | 8 Deep Finder Angles, edge cases, state machine verification | `logic_auditor`, `verification-before-completion`, `systematic-debugging`, `receiving-code-review`, `error-debugging-error-analysis` | **codegraph** (dynamic hop verification) |
| **Scientific Computing Specialist** | `science_analyst` | Physics, math, numerical modeling, scientific data analysis | `claude-scientific-skills`, `deep-research`, `python-pro`, `algorithmic-art` | Python runtime, **google-developer-knowledge**, **Notion MCP** |
| **Technical Researcher** | `researcher` | Deep research, automated query decomposition, literature synthesis | `deep-research`, `context7-auto-research`, `research-engineer`, `wiki-researcher`, `researcher`, `retrieving-developer-knowledge`, `notion-research-documentation`, `plain-writing` | **google-developer-knowledge**, **Notion MCP**, **klipper-docs** |
| **Repository Maintainer** | `coworker` | Structural indexing, call paths, repo synchronization, git flows | `coworker`, `using-git-worktrees`, `finishing-a-development-branch` | **gitingest** (`ingest_repository`), **codegraph** (`codegraph_explore`), **github-mcp-server** (`create_pull_request`, `push_files`) |
| **Architecture & Strategic Planner** | `planner` | System specs, atomic decomposition, compliance by design | `planning-with-files`, `concise-planning`, `planner`, `writing-plans`, `brainstorming`, `notion-spec-to-implementation`, `notion-database-designer` | **codegraph**, **Notion MCP** |
| **Fleet Swarm Deployer** | `opencode_deployer` | Headless OpenCode daemons, multi-agent pipelines | `opencode_deployer`, `dispatching-parallel-agents`, `subagent-driven-development`, `using-git-worktrees` | OpenCode CLI (`opencode serve`, `opencode run`), Gemini Flash & NVIDIA Nemotron tiers |

---

## Inter-Agent Communication & Verification Protocols

1. **Central Communication Bus (`workspace .agents/comm.md`)**:
   - All subagents announce work status and verification verdicts via standard block syntax:
     ```markdown
     [FROM: Agent_Name] -> [TO: Target_Agent / ALL]
     [STATUS: Information | Request | Urgent | Passed | Failed]
     [BODY: Specific details, paths, or instructions]
     ```
2. **Gatekeeper Protocol (`workspace .agents/tasks.md`)**:
   - Implementation agents submit code in `Review` status.
   - Dual-signoff from `logic_auditor` and `cybersecurity_specialist` (`security_auditor`) is mandatory before Orchestrator closes tasks as `Complete`.
   - Security and reverse engineering audits are strictly non-destructive and evidence-based.
