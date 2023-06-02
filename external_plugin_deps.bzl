load("//tools/bzl:maven_jar.bzl", "maven_jar")

def external_plugin_deps():
    maven_jar(
        name = "okio",
        artifact = "com.squareup.okio:okio:1.17.2",
        sha1 = "78c7820b205002da4d2d137f6f312bd64b3d6049",
    )

    maven_jar(
        name = "okhttp",
        artifact = "com.squareup.okhttp3:okhttp:3.14.9",
        sha1 = "3e6d101343c7ea687cd593e4990f73b25c878383",
    )

    maven_jar(
        name = "logging-interceptor",
        artifact = "com.squareup.okhttp3:logging-interceptor:3.14.9",
        sha1 = "7358b6fa1d6c1c8b8c01cb05acd74dbe6d680fb1",
    )

    maven_jar(
        name = "retrofit",
        artifact = "com.squareup.retrofit2:retrofit:2.8.2",
        sha1 = "8bdfa4e965d42e9156f50cd67dd889d63504d8d5",
    )

    maven_jar(
        name = "converter-moshi",
        artifact = "com.squareup.retrofit2:converter-moshi:2.8.2",
        sha1 = "7af80ce2fd7386db22e95aa5b69381099778c63b",
    )

    maven_jar(
        name = "moshi",
        artifact = "com.squareup.moshi:moshi:1.9.2",
        sha1 = "1b1336538beda8aa41cbfabf541b8a964e7f6045",
    )

    maven_jar(
        name = "auto-value-moshi-annotations",
        artifact = "com.ryanharter.auto.value:auto-value-moshi-annotations:1.0.0",
        sha1 = "91de87977b514a437ab746e3c1f08e7001249e82",
    )

    maven_jar(
        name = "auto-value-moshi-extension",
        artifact = "com.ryanharter.auto.value:auto-value-moshi-extension:1.1.0",
        sha1 = "e93cce5266cadebee814bf3725188d7d1d9714bb",
    )

    maven_jar(
        name = "auto-value-moshi-factory",
        artifact = "com.ryanharter.auto.value:auto-value-moshi-factory:1.1.0",
        sha1 = "c5e31ecf6705fec1c4e4aeeae5868036504eee53",
    )
