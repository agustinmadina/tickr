import org.gradle.api.provider.Provider
import org.gradle.plugin.use.PluginDependency

plugins {
    `kotlin-dsl`
}

// The convention plugins apply these to consuming modules and reference their extension types, so
// their implementation artifacts must be on this build's compile classpath. Each is derived from
// the version-catalog plugin marker so versions are never duplicated here.
fun plugin(dependency: Provider<PluginDependency>): Provider<String> =
    dependency.map { "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version.requiredVersion}" }

dependencies {
    implementation(plugin(libs.plugins.androidKotlinMultiplatformLibrary))
    implementation(plugin(libs.plugins.kotlinMultiplatform))
    implementation(plugin(libs.plugins.composeMultiplatform))
    implementation(plugin(libs.plugins.composeCompiler))
}
