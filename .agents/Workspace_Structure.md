# Workspace Structure — Kanso-Launcher Multi-Agent Fleet

> Canonical directory conventions and structural truth for all agents operating in Kanso-Launcher.
> Maintained by the Coworker Agent.

---

## 1. Directory Topology

```text
Kanso-Launcher/
├── README.md                                    # Project description & overview
├── AGENTS.md                                    # Universal behavioral contract & global guardrails
└── .agents/                                     # Antigravity customization discovery root
    ├── Workspace_Structure.md                   # This structural truth map
    ├── tasks.md                                 # Task board (Pending → In Progress → Review → Complete)
    ├── comm.md                                  # Inter-agent communication log & gatekeeper audits
    ├── custom_agents_manifest.md                # Agent fleet matrix & tool mappings
    ├── rules/
    │   └── AGENTS.md                            # Directory & workspace rules
    ├── agents/                                  # 22 Standalone workspace agent definitions (.md)
    └── plugins/
        ├── custom-agent-fleet/                  # Antigravity Plugin Bundle (v2.0.0)
        │   ├── plugin.json                      # Validated plugin manifest
        │   ├── README.md                        # Plugin documentation & role index
        │   ├── assets/logo.svg                  # Vector constellation logo
        │   ├── rules/AGENTS.md                  # Always-on plugin behavioral rules
        │   ├── agents/                          # 22 Plugin-bundled agent definitions
        │   └── skills/                          # 71 Bundled domain skills (no BOM, strict validated)
        ├── superpowers/                         # Software engineering workflows plugin (v7.0.2)
        │   ├── plugin.json                      # Validated plugin manifest
        │   ├── rules/AGENTS.md                  # TDD & engineering discipline rules
        │   ├── hooks.json                       # SessionStart lifecycle hook
        │   └── skills/                          # 15 Engineering skills (TDD, review, debugging, planning)
        └── swarm/                               # Native Multi-Agent Swarm Plugin (v1.0.0)
            ├── plugin.json                      # Validated plugin manifest
            ├── assets/logo.svg                  # Swarm icon asset
            ├── sidecars/swarm_board/            # Interactive swarm board web UI sidecar
            └── skills/                          # 4 Swarm skills (swarm, monitor, narrator, pane)
```

---

## 2. Active Plugin Ecosystem

| Plugin Name | Version | Source Scope | Capabilities | Status |
| :--- | :--- | :--- | :--- | :--- |
| **custom-agent-fleet** | 2.0.0 | Workspace (`.agents/plugins/`) | 22 autonomous roles, 71 domain skills, rules, Stitch CLI & StitchMCP dual-mode | Active (Strict Passed) |
| **superpowers** | 7.0.2 | Workspace (`.agents/plugins/`) | TDD, systematic debugging, code review, git worktrees, session hooks | Active (Strict Passed) |
| **swarm** | 1.0.0 | Workspace (`.agents/plugins/`) | Swarm board sidecar UI, multi-agent parallel swarm orchestration, 4 skills | Active (Strict Passed) |
| **mobbin** | 1.5.0 | Partner Marketplace | Mobile and web design pattern search & references | Active |
| **notion** | 0.1.0 | Partner Marketplace | Knowledge capture, specs, meeting notes, database management | Active |
| **google-developer-knowledge** | 1.0.3 | Official Marketplace | Google Cloud, Android, Firebase, and Gemini documentation retrieval | Active |

---

## 3. Specialized Multi-Agent Roster (22 Roles)

1. `coder`: Source implementation, TDD, refactoring.
2. `planner`: System architecture, task decomposition, SOC2/GDPR compliance.
3. `designer`: UI/UX design, Stitch CLI & StitchMCP dual-mode generation, Mobbin references.
4. `security_auditor`: Read-only gatekeeper, OWASP Top 10, secret detection.
5. `logic_auditor`: Read-only gatekeeper, edge condition verification, invariants.
6. `coworker`: Workspace indexing (GitIngest/CodeGraph), structural truth, git operations.
7. `researcher`: Deep iterative codebase exploration, developer knowledge retrieval.
8. `opencode_deployer`: Swarm automation, headless CLI pipelines.
9. `embedded_hardware_engineer`: Bare-metal firmware, Cortex-M, DMA/cache coherency.
10. `pcb_designer`: KiCad schematics, layout, DRC/ERC, manufacturing exports (Konnect).
11. `ai_motion_graphics_creator`: Remotion animations, AI video cuts, caption sync.
12. `scientific_researcher`: Academic literature review, empirical data modeling.
13. `backend_architect`: Distributed microservices, high-throughput APIs, event pipelines.
14. `frontend_architect`: WebGL 3D web, Radix/Tailwind design systems, WCAG 2.2.
15. `firmware_security_analyst`: Binary firmware audits, bus protocols, packet capture analysis.
16. `cloud_security_specialist`: Cloud pentesting (AWS/GCP/Azure), BOLA/IDOR fuzzing.
17. `compliance_officer`: Governance-by-design, STRIDE threat modeling, license audits.
18. `orchestrator`: Fleet coordinator, tasks.md state, inter-agent handoffs.
19. `multimedia_creative_designer`: Generative algorithmic art, 2D vector graphics.
20. `penetration_tester`: Multi-vector ethical security assessments.
21. `reverse_engineer`: Binary disassembly, decompilation, protocol extraction.
22. `rag_engineer`: Vector databases, chunking strategies, embeddings.
