.. _aws-cross-protocol:

=========================
AWS cross-protocol traits
=========================

This specification defines traits that are not restricted to use in a single
AWS protocol, but are specific to the AWS suite of protocols.

.. contents:: Table of contents
    :depth: 2
    :local:
    :backlinks: none


.. _aws.protocols#httpContentMd5-trait:

--------------------------------------
``aws.protocols#httpContentMd5`` trait
--------------------------------------

Summary
    Indicates that an operation requires the Content-MD5 header set in its HTTP
    request.
Trait selector
    ``operation``
Value type
    Annotation trait.
See
    :rfc:`1864`

.. tabs::

    .. code-tab:: smithy

        @httpContentMd5
        operation PutSomething {
            input: PutSomethingInput,
            output: PutSomethingOutput
        }

.. note::

    While not specifically tied to a single AWS protocol, this trait is
    currently only utilized by Amazon S3, which utilizes the
    :ref:`aws-restxml-protocol`.
