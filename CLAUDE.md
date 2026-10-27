# CLAUDE.md

Monorepo of two native apps for the same product: a Yu-Gi-Oh life-point counter.

- `ios/` — SwiftUI. See `ios/CLAUDE.md`.
- `android/` — Jetpack Compose. See `android/CLAUDE.md`.
- `SPEC.md` — shared specification. **Single source of truth.**

## Parity rule

The two apps must keep the same UI components and the same features. Only the implementation differs:
each uses native components and best practices of its platform (Apple HIG / Material 3), never a
cross-platform layer and never one platform imitating the other's look.

When changing behaviour or UI:
1. Update `SPEC.md` first (rules, examples, component table).
2. Implement it in **both** `ios/` and `android/`, or record the gap in `SPEC.md` → "Divergenze da chiudere".
3. Keep the numeric examples in `SPEC.md` §3 covered by tests on each platform (`android/` JVM tests, `ios/LPCounterTests`).

## Git

Branches: work on `develop`, `main` is the released line. Platform build artifacts are ignored by each platform's own `.gitignore`.

- Every commit is a Conventional Commit (`feat`, `fix`, `perf`, `refactor`, `style`, `build`, `chore`, `docs`, `test`),
  also on `develop`. Scope = `spec`, `ios`, `android`, or a feature (`feat(ios): ...`, `docs(spec): ...`).
  A change touching both apps and the spec omits the platform scope (`feat(history): ...`) and lands all three together.
- Release by fast-forward: checks green on both apps, then `git push origin develop:main`.
- Tag only `main`. After the push, compute the bump from `git log <last-vX.Y.Z>..main`, highest wins:
  `!` or `BREAKING CHANGE:` → major, `feat` → minor, `fix`/`perf` → patch, other types → no tag.
  Annotated tag `vX.Y.Z`, then `git push origin vX.Y.Z`. First tag is `v1.0.0`.
- The version lives only in tags; the apps' own build version numbers are bumped by hand when shipping to a store.

## Tasks

The task list lives in Notion, not in the repo: page "LPCounter" under Personale / Progetti
(https://app.notion.com/p/3fe3da5e7bfd811180b5e9b9c6303c2f). Read it with the Notion MCP before choosing what to work on,
and update it (check off / add items) when a task is done or discovered.
