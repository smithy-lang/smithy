# Smithy JMESPath

This is an implementation of a [JMESPath](https://jmespath.org/) parser
written in Java. It's not intended to be used at runtime and does not include
an interpreter. It doesn't implement functions. Its goal is to parse
JMESPath expressions, perform static analysis on them, and provide an AST
that can be used for code generation.
