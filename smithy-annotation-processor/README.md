# Smithy Annotation Processor

This module is a library that can be used to create Annotation processors 
that execute Smithy build plugins. It is primarily intended for executing
build plugins that generate Java code.

The `SmithyAnnotationProcessor` base class provided by this library 
can be extended to define an annotation processor that executes a 
single [Smithy-Build plugin](https://smithy.io/2.0/guides/building-models/build-config.html#plugins)
and writes any generated Java source files or `META-INF/*` files to the correct
output locations.

