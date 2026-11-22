---
name: stitch-ui-design
description: >-
  Expert guide for creating effective prompts and generating production UI using Google Stitch via both Stitch CLI and StitchMCP. Use when designing web/mobile interfaces, generating screens, exploring variants, or exporting design code.
---

# Stitch UI Design & Generation (CLI + MCP Dual Mode)

Comprehensive guide for designing, generating, and iterating user interfaces using Google Stitch via the **Stitch CLI** and **StitchMCP**.

## Dual-Mode Architecture: CLI vs. MCP

Stitch is accessible via two complementary interfaces:

| Surface | Tool / Command | Best Used For |
| :--- | :--- | :--- |
| **Stitch CLI** | stitch generate, stitch download, stitch capture, stitch serve via un_command | Scripted builds, batch variant generation, downloading self-contained HTML/PNG assets to disk, live preview servers, page captures. |
| **StitchMCP** | generate_screen_from_text, generate_variants, download_file | Direct in-conversation generation, conversational iterative edits, design system creation and application. |

Agents can use **either standalone** or **both in tandem** (e.g., generate screens via MCP, then export assets to disk via Stitch CLI stitch download).

---

## 1. Stitch CLI Standalone Workflow

The stitch CLI (v0.12.0+) is installed globally and executable via un_command.

### Core CLI Commands

`ash
# 1. List existing projects and screens
stitch find projects --json
stitch find screens --project <project_id> --json

# 2. Generate a screen in a new project
stitch generate screen --new-project "Analytics Dashboard" \
  --prompt "Modern SaaS revenue dashboard with MRR metrics, trend charts, and transaction table in dark mode" \
  --device DESKTOP

# 3. Generate a screen in an existing project
stitch generate screen --project <project_id> \
  --prompt "Settings profile modal with avatar upload, notification toggles, and API keys" \
  --device DESKTOP

# 4. Generate variants from an existing screen
stitch generate variant --project <project_id> --screen <screen_id> \
  --prompt "Refine color palette to high-contrast emerald accents and compact card layouts" \
  --count 3 --creative-range REFINE --aspects LAYOUT,COLOR_SCHEME

# 5. Download generated HTML & screenshot PNG to disk
stitch download screen <screen_id> --project <project_id>

# 6. Capture existing live URL or local HTML file into Stitch Canvas
stitch capture https://example.com/pricing -o pricing-capture.html
stitch capture ./dist/index.html

# 7. Launch local preview dev server
stitch serve --project <project_id>

# 8. Print canonical web canvas URL
stitch url screen <screen_id> --project <project_id>
`

### Options & Flags
- --device: DESKTOP or MOBILE.
- --model: GEMINI_3_8_FLASH or GEMINI_3_5_FLASH_LITE.
- --creative-range: REFINE (subtle polish), EXPLORE (balanced alternatives), REIMAGINE (bold divergence).
- --aspects: Comma-separated: LAYOUT, COLOR_SCHEME, IMAGES, TEXT_FONT, TEXT_CONTENT.
- --json: Return clean JSON output for programmatic downstream processing.

---

## 2. StitchMCP In-Chat Workflow

When interacting within tool turns:

1. **Create Project:** call_mcp_tool(ServerName: "StitchMCP", ToolName: "create_project", Arguments: { title: "App Name" })
2. **Generate Screen:** call_mcp_tool(ServerName: "StitchMCP", ToolName: "generate_screen_from_text", Arguments: { projectId: "...", prompt: "...", deviceType: "DESKTOP" })
3. **Explore Variants:** call_mcp_tool(ServerName: "StitchMCP", ToolName: "generate_variants", Arguments: { projectId: "...", screenId: "...", prompt: "...", variantCount: 3 })
4. **Edit Screen:** call_mcp_tool(ServerName: "StitchMCP", ToolName: "edit_screens", Arguments: { projectId: "...", screenId: "...", prompt: "..." })
5. **Download Assets:** call_mcp_tool(ServerName: "StitchMCP", ToolName: "download_file", Arguments: { url: "..." })

---

## 3. High-Conversion Art-Direction Prompting

Stitch produces the highest fidelity UI when prompts follow the **Context + Layout + Components + Aesthetic** formula:

### The 4-Part Prompt Structure
1. **Context & Role:** State who uses the screen and its primary user objective.
2. **Layout Architecture:** Specify visual layout (e.g., sticky sidebar, 12-column grid, split pane, hero header).
3. **Core Components:** List discrete interactive controls (stat cards, data tables, filter chips, dropdowns, avatar stacks).
4. **Visual Aesthetic & Styling:** Dictate color palette (exact hex or semantic tokens), typography mood, surface treatments (glassmorphism, flat minimalism, border contrast), and spacing density.

### Example Prompt Templates

#### SaaS Analytics Dashboard (Desktop)
`	ext
B2B enterprise analytics dashboard for financial ops teams.
Layout: 240px fixed dark sidebar navigation, sticky top bar with tenant switcher and date range picker, main canvas with 4-card metric summary row, followed by 2-column split (60% line chart for MRR velocity, 40% doughnut chart for churn breakdown), and bottom paginated audit table.
Aesthetic: Deep neutral slate background (#0F172A), border-stroke cards with subtle gradient fills, vibrant electric violet (#8B5CF6) and mint green (#10B981) data lines, compact tabular typography with mono figures.
`

#### Mobile Fintech Onboarding (Mobile)
`	ext
Mobile biometric login and identity verification step for a modern neo-bank.
Layout: Top status bar, back arrow and step indicator (3 of 4), centered hero illustration with animated security shield, headline 'Verify Your Identity', secondary explainer paragraph, primary full-width FaceID trigger button, fallback 'Enter PIN' link.
Aesthetic: Clean iOS Cupertino aesthetic, pure white background (#FFFFFF), deep midnight blue typography (#0A192F), subtle pill shadow on primary button with soft haptic glow, 16px safe-area margins.
`

---

## 4. Downstream Code Handoff

Once a screen is generated in Stitch:
1. Run stitch download screen <screen_id> --project <project_id> to save screen.html and screenshot.png locally.
2. Frontend agents (rontend_architect, coder) inspect the downloaded HTML/CSS structure.
3. Deconstruct components into React/Tailwind/Radix primitives, adhering to adix-ui-design-system and 	ailwind-design-system standards.