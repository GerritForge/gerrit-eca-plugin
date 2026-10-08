load(
    "@com_googlesource_gerrit_bazlets//:gerrit_plugin.bzl",
    "gerrit_plugin",
    "gerrit_plugin_tests",
)

PLUGIN = "gerrit-eca-plugin"

gerrit_plugin(
    srcs = glob(["src/main/java/**/*.java"]),
    manifest_entries = [
        "Implementation-Title: Eclipse ECA validation",
        "Implementation-URL: https://review.gerrithub.io/admin/repos/GerritForge/gerrit-eca-plugin",
        "Gerrit-PluginName: eca-validation",
        "Gerrit-Module: org.eclipse.foundation.gerrit.validation.ECAValidationModule",
    ],
    plugin = PLUGIN,
    resources = glob(["src/main/resources/**/*"]),
)

gerrit_plugin_tests(
    srcs = glob(["src/test/java/**/*Test.java"]),
    plugin = PLUGIN,
)
