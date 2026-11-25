---
name: Tactile Cyber-Mecha HUD
colors:
  surface: '#121315'
  surface-dim: '#121315'
  surface-bright: '#38393b'
  surface-container-lowest: '#0d0e10'
  surface-container-low: '#1b1c1e'
  surface-container: '#1f2022'
  surface-container-high: '#292a2c'
  surface-container-highest: '#343537'
  on-surface: '#e3e2e4'
  on-surface-variant: '#d4c4ac'
  inverse-surface: '#e3e2e4'
  inverse-on-surface: '#303032'
  outline: '#9d8f78'
  outline-variant: '#504533'
  surface-tint: '#fdbb12'
  primary: '#ffd586'
  on-primary: '#412d00'
  primary-container: '#f5b400'
  on-primary-container: '#654900'
  inverse-primary: '#7b5900'
  secondary: '#bdf4ff'
  on-secondary: '#00363d'
  secondary-container: '#00e3fd'
  on-secondary-container: '#00616d'
  tertiary: '#ffd0ca'
  on-tertiary: '#690003'
  tertiary-container: '#ffa99d'
  on-tertiary-container: '#a00007'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#ffdea4'
  primary-fixed-dim: '#fdbb12'
  on-primary-fixed: '#261900'
  on-primary-fixed-variant: '#5d4200'
  secondary-fixed: '#9cf0ff'
  secondary-fixed-dim: '#00daf3'
  on-secondary-fixed: '#001f24'
  on-secondary-fixed-variant: '#004f58'
  tertiary-fixed: '#ffdad5'
  tertiary-fixed-dim: '#ffb4aa'
  on-tertiary-fixed: '#410001'
  on-tertiary-fixed-variant: '#930005'
  background: '#121315'
  on-background: '#e3e2e4'
  surface-variant: '#343537'
typography:
  display-hero:
    fontFamily: Space Grotesk
    fontSize: 56px
    fontWeight: '700'
    lineHeight: 56px
    letterSpacing: -0.04em
  display-hero-mobile:
    fontFamily: Space Grotesk
    fontSize: 44px
    fontWeight: '700'
    lineHeight: 44px
    letterSpacing: -0.04em
  headline-lg:
    fontFamily: Space Grotesk
    fontSize: 28px
    fontWeight: '600'
    lineHeight: 34px
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Space Grotesk
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 26px
    letterSpacing: -0.01em
  body-lg:
    fontFamily: Geist
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 22px
  body-md:
    fontFamily: Geist
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  label-hud-lg:
    fontFamily: Space Mono
    fontSize: 13px
    fontWeight: '700'
    lineHeight: 16px
    letterSpacing: 0.08em
  label-hud-sm:
    fontFamily: Space Mono
    fontSize: 10px
    fontWeight: '400'
    lineHeight: 12px
    letterSpacing: 0.12em
  label-telemetry:
    fontFamily: Space Mono
    fontSize: 8px
    fontWeight: '700'
    lineHeight: 10px
    letterSpacing: 0.15em
rounded:
  sm: 0.5rem
  DEFAULT: 1rem
  md: 1.5rem
  lg: 2rem
  xl: 3rem
  full: 9999px
spacing:
  gutter: 0.75rem
  gutter-sm: 0.5rem
  margin: 1rem
  margin-mobile: 0.75rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 0.75rem
  space-lg: 1rem
  space-xl: 1.5rem
  space-2xl: 2rem
---

## Brand & Style

This design system establishes a hybrid aesthetic uniting physical tactile neumorphism with high-precision Mecha/Cyberpunk telemetry. Designed for an advanced Android system interface, it balances physical weight with digital diagnostics.

The interface evokes calibrated military hardware, precision telemetry instruments, and tactile console decks. It counters flat, immaterial digital trends by delivering deep physical presence through soft extruded volumes, milled inset wells, and precise HUD overlays. 

Key stylistic pillars:
- **Tactile Neumorphism:** Dual-source ambient illumination creates soft convex extrusions and milled concave receptacles on matte composite substrate.
- **Cyber-Industrial Accents:** Technical telemetry indices, dot-matrix status displays, micro HUD corner registration brackets (`⌜ ⌝`), and chamfered capsule envelopes.
- **Dynamic Organic Partitioning:** Top-deck organic color blocking sweeps across mechanical grid structures, separating critical environmental metrics from interactive controls.

## Colors

The palette is rooted in an ultra-low-sheen dark matte carbon foundation, paired with high-visibility hazard indicators and luminescence-driven telemetry:

- **Carbon Foundation (`#111214`):** Non-reflective substrate absorbing glare while providing high contrast for tactical indicators. Surface variants include `#181A1D` for raised extruded hulls and `#0B0C0D` for recessed channels.
- **Signal Amber (`#F5B400`):** Primary command accent used for hero clock blocks, active toggle switches, primary battery gauges, and operational warnings.
- **Cyberpunk Cyan (`#00E5FF`):** Secondary telemetry frequency used for live network transfers, low-latency sync states, and targeting micro-reticles.
- **Mecha Crimson (`#FF3B30`):** Alert tier for urgent system warnings, critical power states, and high-priority action overrides.
- **Muted Carbon Slate (`#8A909A`):** Low-contrast telemetry typography, inactive radio states, and passive structural grid markers.

## Typography

The typographic hierarchy combines utilitarian industrial geometry with tactical monospace instrumentation:

- **Hero & Headers (Space Grotesk):** Provides structural solidity for primary data hubs, time readouts, and module designations. Large numeric indicators emulate discrete multi-segment physical displays.
- **Interface Body (Geist):** Unadorned, neutral legibility for descriptive copy, secondary metadata, and contextual launcher settings.
- **HUD Labels & Telemetry (Space Mono):** Fixed-pitch data typography set in full caps with generous tracking. Used across hardware status readings, sub-badge diagnostic units (`KB/S`, `RAM`, `HUMIDITY`), and grid coordinates.
- **Dot-Matrix Simulation:** Numerical clocks and active connectivity matrices utilize strict character block grids with `letter-spacing: 0.1em` to retain industrial authenticity.

## Layout & Spacing

The spatial architecture is built on a 4-column mobile fluid grid with compact structural gutters, optimized for one-handed thumb interaction within launcher environments:

- **Grid Alignment:** Primary widgets span full (4 col), half (2 col), or irregular 3:1 asymmetric splits to balance hero metrics with rapid toggle clusters.
- **Tactile Padding:** Component interiors maintain strict `space-md` or `space-lg` internal breathing room around extruded triggers, ensuring physical separation between interactive touch targets.
- **Zonal Partitioning:** 
  - **Top Deck (0–28% Y-axis):** Organic color-blocked command zone with curved bottom sweep for primary clock, weather, and system state.
  - **Telemetry Deck (28–84% Y-axis):** Modular grid hosting extruded control tiles, sensor feeds, and media playback pods.
  - **Dock Deck (84–100% Y-axis):** Floating tactile dock capsule anchored above standard OS gesture insets.

## Elevation & Depth

Visual hierarchy is communicated through directional light physics modeling a top-left (135°) diffuse illumination source:

- **Extruded Deck Surfaces (Convex Neumorphism):** Interactive cards, tactile dials, and module containers elevate from the `#111214` plane using dual-source ambient shadows:
  - Highlight: `-6px -6px 14px rgba(255, 255, 255, 0.04)`
  - Shadow: `8px 8px 18px rgba(0, 0, 0, 0.75)`
- **Recessed Wells & Insets (Concave Neumorphism):** Search capsules, unselected toggle bases, and gauge tracks sit depressed into the chassis:
  - Inset Shadow: `inset 4px 4px 8px rgba(0, 0, 0, 0.8), inset -3px -3px 6px rgba(255, 255, 255, 0.03)`
- **Mecha Reticles & Ghost HUD Borders:** Translucent borders (`1px solid rgba(255, 255, 255, 0.06)`) provide precise structural containment without breaking neumorphic softness.
- **Laser Edge Indicators:** Active toggle states introduce an internal glow via `0 0 12px rgba(245, 180, 0, 0.35)` or cyan telemetry bleed.

## Shapes

The design system employs an ergonomic shape vocabulary inspired by extruded flight-stick controls, industrial casing, and mecha armor plating:

- **Capsule & Stadium Geometry:** System toggles, dock containers, and quick dials adopt pill geometry (`border-radius: 9999px` or `rounded-xl`) to maximize tactile touch affordance.
- **Angled / Chamfered Pods:** Complex widgets (such as network rate meters or secondary status badges) integrate 45-degree diagonal junctions and asymmetric pill orientations.
- **Scalloped Weather Badge:** Multi-cusp gear/flower curvature with 12 evenly distributed soft scallops, evoking mechanical instrumentation dials and analog gaskets.
- **HUD Registration Marks:** Micro-scale corner brackets (`border-top`, `border-left` at 1.5px) punctuate media and telemetry widgets, establishing geometric contrast against the curved hulls.

## Components

### Hardware Toggles & Action Buttons
- **Circular Tactile Buttons:** Extruded convex discs (`48px` to `56px`) with soft dual shadows. When active, transition to solid Signal Amber (`#F5B400`) with rich dark carbon iconography, or Mecha Crimson for alerts.
- **Recessed Pill Switches:** Concave carbon stadium track holding an extruded circular slider. When engaged, the slider illuminates with dynamic amber backlighting.

### Telemetry Pods & Custom Cards
- **Media HUD Card:** Dual-shadow rounded hull featuring corner HUD registration brackets (`⌜ ⌝` in `#33373E`), an inset album artwork square with subtle rim lighting, dot-matrix track designations, and mini circular playback buttons (`32px`).
- **Network & RAM Meter:** Tilted stadium pill card incorporating a 270-degree radial gauge with circular tracker strokes in Signal Amber or Cyberpunk Cyan.
- **Scalloped Weather Dial:** 12-lobed soft tactile badge floating via deep perimeter ambient shadow. Centered weather symbol with monospace humidity/temperature micro-readouts below.

### Search Bar & Input Fields
- **Milled Search Receptacle:** Concave elongated pill recessed into the carbon substrate (`inset 3px 3px 6px rgba(0,0,0,0.9)`). Displays an amber or cyan magnifying reticle with low-contrast placeholder text in `Space Mono`.

### Navigation Dock Capsule
- **Tactile Launcher Dock:** Floating capsule surface spanning the lower screen width. Houses 4 to 5 inset circular app receptacles with monochrome vector glyphs that elevate into extruded soft buttons when touched.

### Calendar & Status Ribbons
- **Horizontal Day Strip:** Pill-shaped unified module with recessed active day indicator. Active date is enclosed in a solid Signal Amber circle with high-contrast dark numerals; inactive days maintain soft monospace labels.