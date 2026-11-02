# November 2026 dependency maintenance

| Input | Previous | Selected | Decision |
| --- | --- | --- | --- |
| Gradle Wrapper | 9.0.0 | 9.8.2 | Apply supported Gradle 9 fixes, including worker/daemon deserialization, repository TLS handling, dependency resolution and dependency-alignment reporting fixes. Preserve JVM 21 and configuration-cache settings. |
| IntelliJ Platform Gradle plugin | 2.9.0 | 2.19.0 | Apply current test/classpath, IDE resolution and verifier fixes. In particular, newer plugin versions correctly map Community verification targets to unified PyCharm for 2025.3+. |
| Compilation SDK | PC 2025.1 | PC 2025.1.6.2 | Apply the latest published maintenance release in the existing SDK branch. JetBrains' release metadata includes CVE-2026-59792. This SDK is used for source builds, not shipped as an IDE update. |
| Kotlin | 2.0.21 | 2.0.21 | Preserve the IDE 2024.3 Kotlin runtime boundary rather than unconditionally selecting a newer compiler. |
| kotlinx.serialization | 1.8.1 | 1.8.1 | Preserve the existing JSON format/runtime input during the category-editing repair; a newer runtime must first satisfy the IDE floor. |
| JUnit | 4.13.2 | 4.13.2 | Retain the current JUnit 4 release used by IDE fixtures. |
| actions/checkout | v1 / v2 | v7, immutable SHA | Apply current supported action runtime and authentication handling. |
| actions/setup-java | v1 | v6.0.1, immutable SHA | Apply current action runtime and explicitly select JDK 21, matching the existing JVM target. |
| Wrapper validation | legacy v1 action | gradle/actions v6.3.0, immutable SHA | Apply the maintained validation action. |

The IntelliJ Gradle 2.14 defaults do not change this project's explicit minimum IDE build or verifier
configuration. Its optional signing/publishing environment defaults do not authorize those tasks;
the workflows execute neither. The 2.19 classpath exclusions and per-IDE verifier runtimes are
accepted; explicit Java/Kotlin bytecode targets remain 21. No removed `setupDependencies` task,
GrammarKit generator, native variant or subscription-key feature is used here.

Newer Kotlin and serialization release lines are excluded from this bounded maintenance because
the plugin retains its IDE 2024.3 floor. No new paid feature, user-project dependency or JSON migration
is introduced. The build-only security fixes do not repair vulnerabilities in a user's installed IDE.

Sources: [Gradle 9.8.2 notes](https://docs.gradle.org/9.8.2/release-notes.html),
[Gradle 9 migration guide](https://docs.gradle.org/current/userguide/upgrading_version_9.html),
[IntelliJ Gradle release history](https://github.com/JetBrains/intellij-platform-gradle-plugin/releases),
[PyCharm 2025.1.6.2 notes](https://youtrack.jetbrains.com/articles/PY-A-233538593/PyCharm-2025.1.6.2-251.29188.66-build-Release-Notes),
[JetBrains release metadata](https://data.services.jetbrains.com/products/releases?code=PCC&latest=false&type=release),
[IDE Kotlin requirements](https://plugins.jetbrains.com/docs/intellij/using-kotlin.html).
