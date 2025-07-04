Configuring @PLUGIN@
====================

## Enabling ECA validation

The ECA validation can be enabled as a commit validation listener and/or as a submit requirement.

### Commit Validation Listener

It can be configured per project whether the ECA validation
is enabled or not. To enable the ECA validation for a project
the project must have the following entry in its
`project.config` file in the `refs/meta/config` branch:

```ini
  [plugin "@PLUGIN@"]
    enabled = true
```

If `plugin.@PLUGIN@.enabled` is not specified in the `project.config`
file the value is inherited from the parent project. If it is not
set on any parent project the ECA validation is disabled for this
project.

### Submit requirement

The plugin exposes a custom operand `has:signed_eca-validation`, which can be used in submit
requirement definition to enforce contributor agreement validation.
For example the following submit requirement could be defined:

```
[submit-requirement "ECA-Signed-SR"]
	description = Ensure committer has signed Contributor Agreement
	submittableIf = has:signed_eca-validation
	canOverrideInChildProjects = false
```
