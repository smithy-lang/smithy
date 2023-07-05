.. _using-code-generation:

=====================
Using Code Generation
=====================

One of Smithy's greatest strengths is code generation. Smithy models can be used to
generate clients and servers in a variety of programming languages. The generated
code takes care of low level details like endpoints, serialization, deserialization,
and more.

In this guide, we will use the Smithy model we created in the
:doc:`Quick start guide </quickstart>` to generate
a TypeScript client that knows how to communicate with the weather service. For a
list of code generators, see `Awesome Smithy <https://github.com/smithy-lang/awesome-smithy#client-code-generators>`_.

.. toctree::
    :maxdepth: 1
    :caption: Using Code Generation guide

    update-model
    generating-a-client
