load("@com_googlesource_gerrit_bazlets//:gerrit_plugin.bzl", "gerrit_plugin")
load("@rules_java//java:defs.bzl", "java_library", "java_plugin")
load("@rules_jvm_external//:defs.bzl", "artifact")

ECA_PLUGIN_DEPS = "eca_plugin_deps"

gerrit_plugin(
    srcs = glob(["src/main/java/**/*.java"]),
    ext_deps = [
        "com.squareup.moshi:moshi",
        "com.squareup.okhttp3:logging-interceptor",
        "com.squareup.okhttp3:okhttp",
        "com.squareup.okio:okio",
        "com.squareup.retrofit2:converter-moshi",
        "com.squareup.retrofit2:retrofit",
    ],
    ext_repo = ECA_PLUGIN_DEPS,
    manifest_entries = [
        "Implementation-Title: Eclipse ECA validation",
        "Implementation-URL: https://review.gerrithub.io/admin/repos/GerritForge/gerrit-eca-plugin",
        "Gerrit-PluginName: eca-validation",
        "Gerrit-Module: org.eclipse.foundation.gerrit.validation.ECAValidationModule",
    ],
    plugin = "gerrit-eca-plugin",
    resources = glob(["src/main/resources/**/*"]),
    deps = [":auto-value-moshi-library"],
)

# compile-only annotation plus the AutoValue Moshi processor. neverlink keeps
# the annotation and the whole processor closure off the plugin runtime
# classpath, so none of it is packaged into gerrit-eca-plugin.jar. the
# generated adapters need only moshi at runtime.
java_library(
    name = "auto-value-moshi-library",
    exported_plugins = [":auto-value-moshi-factory-plugin"],
    neverlink = 1,
    exports = [artifact(
        "com.ryanharter.auto.value:auto-value-moshi-annotations",
        repository_name = ECA_PLUGIN_DEPS,
    )],
)

java_plugin(
    name = "auto-value-moshi-factory-plugin",
    processor_class = "com.ryanharter.auto.value.moshi.factory.AutoValueMoshiAdapterFactoryProcessor",
    deps = [artifact(
        a,
        repository_name = ECA_PLUGIN_DEPS,
    ) for a in [
        "com.google.auto.value:auto-value",
        "com.ryanharter.auto.value:auto-value-moshi-annotations",
        "com.ryanharter.auto.value:auto-value-moshi-extension",
        "com.ryanharter.auto.value:auto-value-moshi-factory",
        "com.squareup:javapoet",
        "com.squareup.moshi:moshi",
        "com.squareup.okio:okio",
    ]],
)
