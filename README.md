gerrit-eca-plugin
=================

A Gerrit plugin for controlling pushes to eclipse.org repositories.

Provides an implementation of [Gerrit](https://code.google.com/p/gerrit/) 3.2's CommitValidationListener interface that imposes the following restrictions:

* A project committer can push a commit on behalf of themselves or any other project committer
* A project committer can push a commit on behalf of a contributor if:
    * The contributor has a valid ECA at the time of the push; and
    * The commit message contains a "Signed-off-by:" statement with credentials matching those of the commit author
* A contributor can push a commit if:
    * They have a valid ECA at the time of the push;
    * The commit's author credentials match the user identity;
    * The commit message contains a "Signed-off-by:" statement with credentials matching those of the commit author

An individual is assumed to be a committer if they have PUSH access to the Gerrit project (repository).

An individual is assumed to have a ECA on file if they are a member of the ECA group (currently hardcoded; a future version will make this configurable).

For more information, please see [ECA in the Ecipse Wiki](http://wiki.eclipse.org/ECA).

Troubleshooting
===============

Push that should otherwise be accepted is being rejected:
* They may be pushing more than one commit. Compare the commit id from the error message with that of the commit; do they match?  

Individual is a committer, but is being rejected:
* Is the email address they're committing with the same as the email address in LDAP?
* Does the corresponding project group have push access on the Gerrit repository?

Individual is not a committer, but has a ECA and is being reject:
* Is the email address they're committing with the same as the email address in LDAP?
* Is the individual in the "Has ECA" LDAP group?
* Is the ECA associated with the right user id?

How to build
============

The plugin can only be built in tree mode, by cloning
Gerrit and the `gerrit-eca-plugin` plugin code, and checking them out on the desired branch.

Example of cloning Gerrit and `gerrit-eca-plugin` for a build:

```
git clone https://gerrit.googlesource.com/gerrit
git clone https://review.gerrithub.io/GerritForge/gerrit-eca-plugin

cd gerrit/plugins
ln -s ../../gerrit-eca-plugin .
rm external_plugin_deps.bzl
ln -s gerrit-eca-plugin/external_plugin_deps.bzl .
```

Example of building the `gerrit-eca-plugin` plugin:

```
cd gerrit
bazel build plugins/gerrit-eca-plugin
```

The `gerrit-eca-plugin.jar` plugin is generated to
`bazel-bin/plugins/gerrit-eca-plugin/gerrit-eca-plugin.jar`.

Code Style
==========

To format Java source code, this plugin uses the
[google-java-format](https://github.com/google/google-java-format)
tool (version 1.7), and to format Bazel `BUILD`, `WORKSPACE` and `.bzl` files the
[buildifier](https://github.com/bazelbuild/buildtools/tree/master/buildifier)
tool (version 4.0.0).
