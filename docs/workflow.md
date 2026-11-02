# Workflow configuration

The globally installed `develop-intellij-plugin` skill defines the development process.

## Project

- Prettify Python, a Free Python symbol-folding plugin with configurable mappings.
- Repository: `meanmail-dev/prettify-python-plugin`; authoritative ref: `origin/master`.
- Release authority: `https://plugins.jetbrains.com/plugins/list?pluginId=ru.meanmail.plugins.prettify-python`.
- Product history and notes: `CHANGES.md`; descriptions: `README.md` and `description.html`.
- Version syntax: year.minor; next major after published `2025.2` is `2026.0`.
  Update `gradle.properties` and add a top unreleased `CHANGES.md` section for a behavior change.
  No paid product descriptor or release date field is configured.
- External portfolio registration: Not configured. Local task queue and roadmap: Not configured.

## Verification

- CI-required: `xvfb-run -a ./gradlew test build buildPlugin verifyPlugin` on GitHub-hosted Ubuntu 24.04,
  using JDK 21. Gradle resolves dependencies before executing the gate. This repository is public;
  the organization's private runners intentionally do not accept public repositories.
  Swing settings tests use a virtual display; the test JVM explicitly disables headless mode.
- CI-required: Gradle Wrapper validation in `gradle-wrapper-validation.yml`.
- Local-diagnostic dependency resolution: `./gradlew dependencies` before a focused local Gradle check.
- Local-diagnostic focused test: `./gradlew test --tests "dev.meanmail.prettifypython.settings.PrettifySettingsComponentTest"`.
- Local-required: `git diff --check`, changed XML parsing, official wrapper distribution/JAR checksum checks.
- Local-required native package: `./gradlew buildPlugin` when CI has no retained distribution.
  Use the configured PyCharm 2025.1.6.2 SDK and JDK 21; newer SDKs can require a newer Kotlin compiler.
  Record the native package hash. A locally built artifact is not the CI binary.
  With a previously running Kotlin daemon on JDK 25, use JDK 21 and
  `-Pkotlin.compiler.execution.strategy=in-process` for package preparation.
- The verifier explicitly covers the IDE 243 floor (PC 2024.3.6.1) in addition to recommended current IDEs.
- Separate lint/typecheck commands and generated sources: Not configured.
- Never run `gradlew clean` without the owner's permission.

## Native Remote Development

- Owner: Local-required through the shared `ide-test-mcp` installation. A reviewed isolated installation
  of the same implementation can qualify a required adapter while preserving a dirty local checkout.
- Runtime: unmodified IDEA 2025.3.6, build 253.33813.25, bundled JBR 21, macOS arm64;
  the controller itself uses the installed JDK 25 and its matching 253 Driver/agent.
- Official PythonCore 253.33813.55 belongs on the backend; Python for JetBrains Client
  253.30387.20 (`com.intellij.python.frontend`) belongs on the frontend.
- Product placement: backend. Use isolated localhost monolith/split fixtures with 1024 MiB heaps.
- Fixture: a synthetic Python file with foldable operators and a separate ordinary text file.
  Verify native settings category editing, individual mapping editing/cancellation, mapping persistence,
  useful folding, repeat use, exact source Save/Undo/Redo, close/reopen and same-client reconnect.
- On macOS IDE 253, open the native Settings menu entry and observe the `Settings` dialog. Locate the
  Prettify Python page through its search field. Use `tree_activate` for category double-clicks; individual
  mappings can be selected and opened with the actual Edit toolbar button. The runner's `open_dialog`
  tree selector sends one click. Confirm applied values in the owned settings XML as well as the UI.
- These Swing tree controls belong to the monolithic UI. The 253 split frontend mirrors the page through
  a Lux panel: use an observed `lux_render` image and its fresh one-use token for `lux_click`, and the
  Lux `open_dialog` selector for Edit Mapping. Verify the visible remote dialog and cancellation;
  backend semantic clicks alone do not establish frontend pointer forwarding.
- Compare the absent-product ordinary-editor control. Warm up for 60 seconds and sample each JVM for
  30 seconds; CPU below 20% of one core, backend/monolith RSS below 4 GiB, frontend below 2 GiB.
- Require at least 4 GiB free before package preparation/launch. Retain package hashes, scoped logs,
  native operation results and resource observations in the private task evidence.

## Publication and invariants

- Candidate branches may be pushed for approved work. Publish reviewed exact-head-green changes through a PR
  into `master`, assigned to the owner. A merge runs the same verification as its PR; these workflows contain
  no signing, publication, release, deployment or artifact upload lane. Inspect the actual workflows before
  reusing tree-equivalent candidate evidence. Marketplace publication always needs separate approval.
- Preserve the IDE 2024.3 floor, JVM 21, optional Python integration, Free mappings, JSON compatibility,
  folding semantics and source bytes. Use `ApplicationManager` scheduling for IDE thread transitions.
- Repository-specific verification improvements belong here and in the existing workflows/tests, using the
  same reviewed publication path. Shared-process improvements belong to the globally installed skill source.
