Configuring @PLUGIN@
====================

## Enabling ECA validation

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
