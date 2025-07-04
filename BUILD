load("@rules_java//java:defs.bzl", "java_library", "java_plugin")
load("//tools/bzl:plugin.bzl", "gerrit_plugin")

gerrit_plugin(
    name = "gerrit-eca-plugin",
    srcs = glob(["src/main/java/**/*.java"]),
    manifest_entries = [
        "Implementation-Title: Eclipse ECA validation",
        "Implementation-URL: https://review.gerrithub.io/admin/repos/GerritForge/gerrit-eca-plugin",
        "Gerrit-PluginName: eca-validation",
        "Gerrit-Module: org.eclipse.foundation.gerrit.validation.ECAValidationModule",
    ],
    resources = glob(["src/main/resources/**/*"]),
    deps = [
        ":auto-value-moshi-library",
        "@converter-moshi//jar",
        "@logging-interceptor//jar",
        "@moshi//jar",
        "@okhttp//jar",
        "@okio//jar",
        "@retrofit//jar",
    ],
)

java_library(
    name = "auto-value-moshi-library",
    exported_plugins = [
        ":auto-value-moshi-factory-plugin",
    ],
    exports = [
        "@auto-value-moshi-annotations//jar",
    ],
)

java_plugin(
    name = "auto-value-moshi-factory-plugin",
    processor_class = "com.ryanharter.auto.value.moshi.factory.AutoValueMoshiAdapterFactoryProcessor",
    deps = [
        "@auto-value-moshi-annotations//jar",
        "@auto-value-moshi-extension//jar",
        "@auto-value-moshi-factory//jar",
        "@auto-value//jar",
        "@javapoet//jar",
        "@moshi//jar",
        "@okio//jar",
    ],
)
