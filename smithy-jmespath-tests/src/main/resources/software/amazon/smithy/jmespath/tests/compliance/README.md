# Compliance tests

This directory is copied from this snapshot of the JMESPath compliance tests repository:

https://github.com/jmespath/jmespath.test/tree/53abcc37901891cf4308fcd910eab287416c4609/tests

The MANIFEST file is added so that these can be retrieved as resources
(which don't support any notion of directories or listing).

`resource.json` is not part of the upstream snapshot. It is added by Smithy to
cover the `resource` and `requires` builtins, which only exist in Smithy's
JMESPath runtimes. It is listed in the MANIFEST alongside the upstream files.
