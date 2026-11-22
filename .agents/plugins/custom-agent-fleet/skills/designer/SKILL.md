---
name: designer
description: >-
  Workflow and operating procedure for the designer agent. Activate this skill when delegating tasks to the designer agent or when acting as the designer.
---

# Workflow: UI & Frontend Design

## 1. Overview
This workflow governs the creation of User Interfaces, wireframes, and frontend components by the Designer Agent, utilizing the Claude-Design canonical rules and the **Stitch MCP** server for robust, system-driven design generation.

## 2. Stitch MCP Integration (Primary Design Engine)
The Designer Agent MUST utilize the `StitchMCP` tools to generate, manage, and iterate on designs.
- **Initialization:** Use `create_project` to establish a new design workspace.
- **Design Systems:** Use `create_design_system` or `create_design_system_from_design_md` to establish the foundational rules (colors, typography, spacing). Always apply the design system to screens using `apply_design_system`.
- **Screen Generation:** Use `generate_screen_from_text` to draft UI screens. Use `generate_variants` to explore layout options.
- **Iteration:** Use `edit_screens` for targeted modifications and `list_screens` / `get_screen` to review current state.
- **Standalone DC Output:** When raw files are needed, deliver conceptual designs as standalone `.dc.html` files, combining markup, logic, and styling.

## 3. Legal, Compliance, and Accessibility Guardrails
To ensure the app "doesn't get sued" and remains ethically compliant, the Designer MUST verify and implement the following 20 checkpoints in all designs:
1. **Privacy Policy:** Include clear links in footers and forms.
2. **Terms of Service:** Ensure TOS links are accessible during onboarding/checkout.
3. **Refund Policy:** State clearly on pricing and checkout screens.
4. **Cookie Policy:** Link to cookie usage details.
5. **Cookie Consent Banner:** Design explicit, non-intrusive consent banners (no assumed consent).
6. **Check Form Consents:** Add explicit opt-in checkboxes for marketing and data collection.
7. **No Unnecessary Data:** Only design form fields for strictly required information.
8. **Audit Third-Party SDKs:** Visually indicate or document where 3rd-party tracking occurs.
9. **Remove Dark Patterns:** No trick questions, hidden cancel buttons, or forced continuity.
10. **Remove Hidden Fees:** Display total costs transparently upfront.
11. **Remove Fake Reviews:** Do not generate or design placeholders for fabricated testimonials.
12. **Remove Unsupported Claims:** Do not design banners with unverified "Best in the world" style claims.
13. **Accessibility Alt Text:** Ensure all images have `alt` attributes defined in the markup/design.
14. **Fix Color Contrast:** Ensure foreground/background colors meet WCAG AA standards minimum.
15. **Keyboard Navigation:** Design visible focus states for all interactive elements.
16. **Add Business Details:** Include physical address and contact info in footers where applicable.
17. **Age Consent:** Include age-gating or parental consent checks for kids' data.
18. **Unsubscribe Links:** Include clear, one-click unsubscribe links in any designed email templates.
19. **License Fonts/Images:** Only use open-source or explicitly licensed fonts and imagery.
20. **Data Deletion Request:** Design a clear UI path in user settings for account and data deletion.

## 4. Avoiding "Vibe-Coded" UI (The Master Checklist)
A "vibe-coded" UI occurs when an AI generates a statistically average "modern" design that mimics aesthetics without functional intent. Designer Agents must actively identify and remove the following red flags and apply the core fixes.

### 4.1 Visual Red Flags
1. **The Signature Purple Problem:** No random purple gradient hero sections, neon purple text shadows, purple hover fills, or purple glow drop shadows unless the brand explicitly calls for it.
2. **Sparkle Icons Everywhere:** Do not use sparkle emojis/icons on buttons, heroes, pricing cards, and footers. One sparkle is max; twenty is a vibe-coded diagnosis.
3. **Hover Animations on Every Card:** Do not overuse hover effects (cards lifting aggressively, rotating slightly, moving and breaking alignment, flashlight shadows, bouncing buttons). Keep hover subtle.
4. **Emojis as UI Elements:** No emojis instead of proper SVG icons. No emojis inside headings, on buttons, in the footer, or as pricing bullets.
5. **Fake Testimonials:** Do not generate AI avatars named "Sarah P." with generic quotes ("Helped me so much"), no job titles, no links, or repeated wording.
6. **Social Icons that do Nothing:** Do not output Instagram icons leading to `#`, Twitter leading to `twitter.com`, or LinkedIn leading to a 404. Test every link.
7. **Massive Icons with Tiny Text:** Avoid inverted visual hierarchies (e.g., huge 48px icons with afterthought text).
8. **Generic Fonts with No Rhythm:** If using Inter, Poppins, Montserrat, or Roboto, establish a clear type ramp. Avoid thick headings paired with excessively light body text, inconsistent line heights, and lack of spacing rhythm.
9. **Semi-Transparent Headers:** Avoid blur backgrounds with thin borders and low contrast text interacting poorly with scrolling content.
10. **Bad Animations:** Remove mismatched Lottie animations, wiggle effects, bounce overshoot, cards popping into place with no easing, scroll stutter, and animations triggered too early.

### 4.2 Structural Red Flags
1. **No Loading States:** When an async action occurs, buttons must visually shift to a loading indicator. Prevent white gaps while data loads; use skeleton screens and progress indicators.
2. **Inconsistent Component Placement:** Do not randomly change button sizes, padding, text alignment, or container widths across pages.
3. **Slow Server Actions:** Design to prevent pages hanging or buttons freezing during operations.
4. **Misaligned Grids:** Ensure cards are aligned, spacing is perfectly even, margins do not collapse unintentionally, and sticky elements do not drift.
5. **Too Many Different Border Radiuses:** Standardize radiuses. Do not mix 4px, 12px, 32px buttons, circular avatars, and square images randomly.

### 4.3 Content, Copy & Technical Red Flags
1. **Slightly Off Copyright Text:** Do not write "All right reversed", "Copyright 2024 YourSiteName", "Created by yourbrand", or "Made by Me". Be precise and professional.
2. **Meaningless Taglines:** Never use generic filler ("Build your dreams", "Launch faster", "Create without limits", "Where ideas become reality", "The future of something"). Tighten copy to a single clear promise.
3. **Overloaded Hero Sections:** Do not throw sparkles, emojis, gradients, two buttons, animated cards, background images, and Lottie animations into one hero section. Reduce visual novelty and increase clarity.
4. **Missing Meta Tags:** Ensure the HTML has an OpenGraph image, a specific title (not just "Home"), and a proper description. Include a favicon.
5. **Broken Responsiveness:** Text must not overflow, cards must not stack weirdly, and the layout must not collapse on mobile. Fix responsiveness before polishing desktop.
6. **Non-functional Interactive Elements:** Carousels must slide. Tabs must switch. Accordions must open and close. Modals must close. The dark mode toggle must work.

### 4.4 The 10 Core Fixes (Mandatory Application)
1. Establish a strict **4-point or 8-point spacing system** and use it everywhere for margins, padding, and gaps.
2. Pick **one font pair** (one heading, one body), define a type ramp, and stick to it.
3. Standardize **border radiuses**.
4. **Remove most animations**. Keep movement tied to user intent.
5. Create **one elevation (shadow) style** and reuse it.
6. Fix **responsiveness** before polishing desktop.
7. Add **loading states everywhere**.
8. Tighten copy to a **single clear promise**.
9. **Test every button**, link, and social icon to ensure they function.
10. **Reduce visual novelty** and increase clarity.

## 5. Aggressive Batching & Refinement
- **Batching:** When indexing a UI kit or reading existing components, batch all tool calls into a single turn. Emit all parallel edits simultaneously.
- **Versioning:** For major architectural shifts, duplicate the file (`component_v2.dc.html`) or generate a new screen variant in Stitch MCP to allow comparative review.



