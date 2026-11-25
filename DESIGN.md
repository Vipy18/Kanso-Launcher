# Kanso-Launcher Design System (`DESIGN.md`)

> **Environment Note:** Google Stitch MCP is not connected in this container; per `main-agent-prompt.md` Rule 1, this document specifies the complete design tokens, custom geometry math, and exact Stitch screen generation prompts for all 5 screens.

---

## 1. Visual Identity

- **System Name:** Mecha-Industrial Neumorph with Bold Color-Blocking
- **Application:** `Kanso-Launcher` (`com.kanso.launcher`)
- **Primary Visual Principle:** Machined hardware surfaces paired with one dominant dynamic accent S-curve, recessed inset telemetry wells, soft dual-source neumorphic bevels, and crisp 5×7 square dot-matrix typography.

---

## 2. Color Tokens

### Dynamic Accent Swatches

| Name | Hex | Role |
|---|---|---|
| Signal Orange (Default) | `#FF4500` | Top S-curve color-block, active toggles, progress accents, A–Z indicator |
| Crimson | `#E62E2D` | Preset industrial swatch |
| Hazard Amber | `#F2B705` | Preset industrial swatch |
| Mecha Cyan | `#00B8D9` | Preset industrial swatch |
| Acid Lime | `#84CC16` | Preset industrial swatch |
| Cobalt Steel | `#2563EB` | Preset industrial swatch |
| Custom HSV / HEX | Dynamic | User picker with automatic WCAG relative-luminance `onAccent` (`#111112` or `#FFFFFF`) |

### Light & Dark Surface Tokens

| Token | Light Mode (Matte Off-White) | Dark Mode (Matte Carbon AMOLED) |
|---|---|---|
| `canvasBg` | `#EAECEF` | `#121214` |
| `surfaceRaised` | `#F0F2F5` | `#1C1D21` |
| `surfaceInset` | `#E2E5EA` | `#141518` |
| `shadowLight` | `#FFFFFF` @ 90%, blur `12.dp`, offset `(-5, -5).dp` | `#2C2E35` @ 60%, blur `8.dp`, offset `(-3, -3).dp` |
| `shadowDark` | `#C5CAD3` @ 85%, blur `14.dp`, offset `(+6, +6).dp` | `#050506` @ 90%, blur `12.dp`, offset `(+5, +5).dp` |
| `darkModuleBg` | `#1A1B1E` | `#18191C` |
| `inkPrimary` | `#111112` | `#F2F3F5` |
| `inkSecondary` | `#6B7078` | `#8E929C` |

---

## 3. Custom Geometry & Math

1. **Curved Top Color-Block (`ColorBlockHeaderShape`)**:
   - Full-width header covering top edge `(0, 0)` to `(W, 0)`.
   - Left contour drops to `yLeft = 0.37f * H` with rounded bottom-left chamfer (`r = 28.dp`), runs horizontally to `x ≈ 0.44f * W`, then sweeps upward via a smooth cubic Bézier S-curve (`C1 = (0.51W, yLeft)`, `C2 = (0.48W, yRight)`, `P2 = (0.59W, yRight)`) to `yRight = 0.27f * H`, continuing right to `(W, yRight)` with a subtle upward flare along the right edge.
2. **Neumorphic Bevel (`Modifier.kansoNeumorphic(raised, cornerRadius)`)**:
   - Raised: Draws two offset blurred rounded-rect layers via native `android.graphics.Paint.setShadowLayer` (specular top-left highlight + deep bottom-right shadow) beneath the `surfaceRaised` fill and 0.75dp crisp bevel rim.
   - Inset: Draws `surfaceInset` fill with clipped top-left inner dark shadow and bottom-right inner specular edge.
   - Press Interaction: Animates scale via `spring()` to `0.96f`, compresses shadow depth by 55%, and fires `HapticFeedbackType.TextHandleMove`.
3. **5×7 Pure-Kotlin Dot-Matrix Clock (`DotMatrixClock`)**:
   - 5-column × 7-row boolean bitmaps for `0`–`9` drawn as uniform square pixels (`pitch = pixelSize + gap`).
   - Stacks `HH` over `MM` separated by two centered 5-dot `+` diamond/cross glyphs.
4. **8-Lobe Scalloped Weather Badge (`ScallopedShape`)**:
   - Parametric polar curve `r(θ) = R_base + A * cos(8 * θ)` with `A ≈ 0.065 * R_base` sampled smoothly into a closed Compose `Path`.
5. **Diagonal Network Telemetry Card**:
   - Stadium pill container rotated `-45°` (`bottom-left` to `top-right`), counter-rotating internal content `+45°` so `X.XX KB/s` sits upright in the upper-right lobe and the circular progress ring anchors the lower-left lobe.

---

## 4. Google Stitch Generation Prompts (All 5 Screens)

1. **Home Screen (Light Mode):**
   *"Android home screen launcher in Mecha-Industrial Neumorph style on matte off-white #EAECEF canvas. Bold Signal Orange #FF4500 curved S-block across top 37% left curving up to 27% right. Left orange area shows black 5x7 square dot-matrix stacked time 21 over 03 with twin + markers and 'Today Monday, Aug 18'. Right orange area holds matte dark #1A1B1E status pill with circular orange speaker button. Tucked below right curve sits a recessed stadium battery pill with circular dot-matrix halftone pattern on left and '29% Battery'. Middle-left has vertical micro dot-scale beside a -45 degree tilted stadium network speed card ('1.18 KB/s' top-right, circular charcoal gauge bottom-left). Middle-right has dark dual pill (red play triangle + Wi-Fi arcs) over twin red circles (up-down arrows, Bluetooth). Lower-left raised neumorphic Media HUD card with rounded-square album art and tiny prev/next circles. Lower-right 8-lobe scalloped weather badge ('Weather 20°', umbrella icon, 'HUMIDITY 86%'). Full-width 7-day calendar pill with dark active day circle. Bottom floating #1A1B1E capsule dock with 4 white monochrome icons (phone, messages, browser, camera). No home search bar."*
2. **Home Screen (Dark Mode):**
   *"Same exact Kanso-Launcher layout on matte carbon AMOLED #121214 canvas, #1C1D21 raised neumorphic cards with subtle #2C2E35 top-left rim highlight and #050506 deep drop shadows, #141518 inset wells, #F2F3F5 primary ink, keeping vibrant #FF4500 top S-curve and accent highlights."*
3. **App Drawer (Full-Screen Grid/List):**
   *"Full-screen Kanso-Launcher App Drawer with matching curved #FF4500 top header holding recessed dark 'Search Apps...' pill, dot-matrix Grid/List toggle button, and Settings gear. Directly below curve sits micro-telemetry bar '48 Apps Installed | RAM: 4.2GB Free'. 4-column grid of tactile neumorphic rounded-square app icon wells and thin right-edge vertical A-Z scrubber rail with floating orange letter indicator."*
4. **Lockscreen & Always-On Display:**
   *"Pure #000000 AMOLED Always-On Display and Lockscreen companion with thin minimalist #FF4500 accent curve along top edge, oversized crisp white 5x7 dot-matrix stacked clock, condensed date, thin circular battery arc, subtle notification dots, compact media bar, and tactile bottom swipe-up unlock pill."*
5. **Kanso Settings & Permissions Health Center:**
   *"Kanso-Launcher Settings screen in Mecha-Industrial Neumorph style with curved accent top bar, segmented Light/Dark/System pill toggle, Solid Matte vs Wallpaper switch, 6 industrial accent color swatches with interactive HSV/HEX picker and live S-curve mini-preview, Icon Pack manager with 4-icon previews, Dock/Drawer options, and Permissions Health Center with live green/orange status badges."*
