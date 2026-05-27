# Development Guidelines

The canonical, detailed standards for working on medTimer — for human contributors and AI coding agents alike.

These documents explain the **why** and the agreed conventions.
The terse, machine-facing summary in [`AGENTS.md`](../../AGENTS.md) points here rather than duplicating the detail.
When guidance conflicts, the per-tool config files (Android Lint via `app/build.gradle.kts`, SonarQube via `sonar-project.properties`, IDE Kotlin code style)
are the source of truth — these docs explain and extend them.

Guidelines are written in **English**.

## Index

| Document                                                   | Audience         | Purpose                                                                                |
|------------------------------------------------------------|------------------|----------------------------------------------------------------------------------------|
| [documentation-guidelines.md](documentation-guidelines.md) | All contributors | How to write and organize docs — docs-as-code, Diátaxis, style, file naming            |
| [kotlin-android.md](kotlin-android.md)                     | All contributors | Kotlin + Android conventions: MVVM, Hilt, Room, Coroutines/Flow, multi-module, flavors |
| [jetpack-compose.md](jetpack-compose.md)                   | All contributors | Jetpack Compose conventions (target standard — Compose not yet adopted)                |
| [testing.md](testing.md)                                   | All contributors | JVM unit, instrumented, fuzz, and Compose testing patterns and what to prioritize      |

AI hygiene (boundaries, secrets, privacy, attribution) lives in [`AGENTS.md`](../../AGENTS.md#ai-hygiene) rather than in this directory — it's short enough to
sit alongside the operational summary the agent loads every session.

## How to use these

- Read the relevant coding + testing guide before your first non-trivial contribution.
- Treat the **Enforcement** section of each coding guide as non-negotiable — it mirrors what CI and Android Lint check.
- Each guide ends with a dated **Sources** section so the external baselines can be re-audited for freshness.

See also: [project README](../../README.md) · [`AGENTS.md`](../../AGENTS.md) · [`CONTRIBUTING.md`](../../CONTRIBUTING.md).
