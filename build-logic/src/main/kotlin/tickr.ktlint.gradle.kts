import org.jlleitschuh.gradle.ktlint.KtlintExtension

// Formatting is not a matter of opinion in this project: it is checked in CI and fails the build,
// so nobody spends a review comment on an import order.
//
// Applied through a convention plugin per module rather than from an `allprojects` block in the
// root build, which is incompatible with Gradle's Isolated Projects.

apply(plugin = "org.jlleitschuh.gradle.ktlint")

configure<KtlintExtension> {
    version.set("1.5.0")
    android.set(true)
    outputToConsole.set(true)
    ignoreFailures.set(false)
    filter {
        exclude { it.file.path.contains("/generated/") }
        exclude { it.file.path.contains("/build/") }
    }
}
