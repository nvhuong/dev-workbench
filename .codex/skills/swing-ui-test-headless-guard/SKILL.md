---
name: swing-ui-test-headless-guard
description: Use when adding or updating DevWorkbench Swing/TestNG UI tests that may run in headless CI or no-display Linux environments.
---

# Swing UI Test Headless Guard

Use this skill for Swing UI tests under `dev-workbench-app/src/test/java`.

## Default pattern

1. Put shared no-display skip logic in:
   `dev-workbench-app/src/test/java/com/nvhuong/devworkbench/test/AbstractSwingUiTest.java`
2. Make Swing/UI tests extend that base class.
3. Use `@BeforeClass` + `SkipException` to skip tests when:
   - `GraphicsEnvironment.isHeadless()` is `true`
   - or Linux has neither `DISPLAY` nor `WAYLAND_DISPLAY`

## Guardrails

- Keep the base class narrow. Only put environment-skip behavior there unless multiple UI tests clearly need more.
- Do not add this base class to non-UI tests.
- Prefer `SkipException` over brittle per-test conditionals.
- After changes, verify with:
  - `mvn -q -pl dev-workbench-app -am -DskipTests compile`
  - `mvn -q -pl dev-workbench-app -am -Dtest=<TestClass> -Dsurefire.failIfNoSpecifiedTests=false -Djava.awt.headless=true test`
