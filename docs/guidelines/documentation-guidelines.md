# Documentation Guidelines

**Audience:** anyone adding or changing documentation in this repository.
**Goal:** docs that are easy to find, trustworthy, and cheap to keep current.

## Docs-as-code

Documentation lives in the repo as Markdown and follows the same flow as code:

- Edit docs in the **same pull request** as the change they describe — a feature isn't done until its docs match.
- Docs are reviewed in PRs like code.
- Use **relative Markdown links** between docs so they work on GitHub and in editors.
  When you move a file, update every inbound link.

## The `docs/` taxonomy

Today, `docs/` mixes two kinds of content at its top level — user-facing how-tos and architectural notes — alongside this `guidelines/` sub-folder:

| Path                       | Holds                                                | Examples                                                                   |
|----------------------------|------------------------------------------------------|----------------------------------------------------------------------------|
| `docs/*.md` (top level)    | User-facing how-tos and architecture notes           | [`UseCases.md`](../UseCases.md), [`reminder_flow.md`](../reminder_flow.md) |
| `docs/*.png`, `docs/*.svg` | Screenshots and figures referenced by the docs above | `medicine_calendar.png`, `tag.svg`                                         |
| `docs/guidelines/`         | Contributor & agent standards (this folder)          | this guide, [`kotlin-android.md`](kotlin-android.md)                       |

The taxonomy will grow as the repo does.
Plausible future sub-folders, modeled on common open-source patterns — add them only when there is real content to place:

- `docs/architecture/` — design and rationale (system layout, module map, threading model). `reminder_flow.md` would fit here once a second architectural doc
  exists.
- `docs/operations/` — release / build / signing playbooks.
- `docs/reports/` — dated, point-in-time evidence (audits, test reports).
- `docs/archive/` — superseded material kept for reference.

Rules of thumb:

- **Index every active doc** in the nearest `README.md` (e.g. [`docs/guidelines/README.md`](README.md)).
- The root [`README.md`](../../README.md) is the product-facing overview.
  Cross-reference, don't duplicate.

## Choose the right type of doc (Diátaxis)

Be clear which of the four kinds you're writing — mixing them makes docs hard to use:

- **Tutorial** (learning) — a guided first run.
- **How-to** (a task) — "How to add a new flavor", "How to run instrumented tests locally".
- **Reference** (lookup) — config keys, build commands, module map.
- **Explanation** (understanding) — design rationale, trade-offs.

If a section starts answering a different question than its heading promises, split it.

## Style

Follow the spirit of the [Google developer documentation style guide](https://developers.google.com/style):

- Active voice, second person ("Run …", not "The script should be run …").
- Plain, concise language; define jargon and expand acronyms on first use (GMS, FOSS, AGP, KSP).
- Lead with the point.
  Prefer short sentences, lists, and tables over long paragraphs.
- Show, don't tell: a code block or command beats a paragraph describing it.
- Use the repo's existing structural patterns where they fit: **audience tables** at the top of guides, **enforcement / source-of-truth callouts** for
  mechanically-checked rules, and **dated `Sources` sections** for re-auditable external references.

## Language

Developer- and code-facing docs are written in **English** — guidelines, architecture notes, API/code comments, and these standards.

User-facing strings in the app (`app/src/main/res/values*/strings.xml`) are translated separately; see [`AGENTS.md`](../../AGENTS.md) for the locale list and
Weblate workflow.
Don't mix languages mid-document.
Keep technical terms and identifiers in English even when discussing localization.

## Links and naming

- File names are **kebab-case** (`jetpack-compose.md`, `kotlin-android.md`).
- Dated artifacts use an ISO date suffix: `name-YYYY-MM-DD.md`.
  These are snapshots — add a new dated file rather than rewriting history.
- Keep relative links valid when reorganizing: after moving files, re-resolve every inbound link.

## Maintenance and archiving

- When a doc is superseded, **move it to `docs/archive/`** with a one-line pointer to its replacement — don't delete history, and don't leave stale guidance
  live.
- Date-stamp evidence docs and don't edit them after the fact; supersede instead.
- **No secrets or real user data in docs** — same rule as code (see [`AGENTS.md` → AI hygiene](../../AGENTS.md#ai-hygiene)).
  Reference config keys by name, never paste real values.
- Each guide carries a `_Last reviewed: YYYY-MM-DD._` footer; bump it when you revisit the content and re-confirm the linked sources.

## Sources

- [Diátaxis](https://diataxis.fr/) — the four documentation types.
- [Google developer documentation style guide](https://developers.google.com/style) — 2025.
- [Write the Docs](https://www.writethedocs.org/guide/) — docs-as-code and user-centered principles.

_Last reviewed: 2026-05-26._
