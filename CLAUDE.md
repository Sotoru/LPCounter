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
3. Keep the numeric examples in `SPEC.md` §3 covered by tests on each platform.

## Git

Single `main` flow. Commit prefixes: `spec:`, `ios:`, `android:`; a feature touching both sides can use `feat:` and
should still land spec + both apps together. Platform build artifacts are ignored by each platform's own `.gitignore`.
