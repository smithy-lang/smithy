.. _smoke-tests:

===========
Smoke Tests
===========

Smoke tests are small, simple tests intended to uncover large issues by ensuring
core functionality works as expected. In Smithy, these tests are used to make
sure clients can successfully make a request to a live service, and that the
service responds with the right kind of response.


--------
Overview
--------

This specification defines a single trait in the ``smithy.test`` namespace that is
used to make basic assertions about how the service should respond to a given
request from the client.

:ref:`smithy.test#smokeTests <smokeTests-trait>`
    Defines a set of test cases to send to a live service to ensure that a
    client can successfully connect to a service and get the right kind of
    response.

This trait can be used by client code generators to generate test cases that test
generated clients against live services to ensure core functionality is working,
and continues to work as the client and service evolve.

.. smithy-trait:: smithy.test#smokeTests
.. _smokeTests-trait:

----------
smokeTests
----------

Summary
    The ``smokeTests`` trait is used to define a set of test cases to send
    to a live service to ensure that a client can successfully connect to
    a service and receives the right kind of response.
Trait selector
    .. code-block:: none

        operation
Value type
    ``list`` of :ref:`SmokeTestCase <SmokeTestCase-struct>` structures

The ``smokeTests`` trait is a list of :ref:`SmokeTestCase <SmokeTestCase-struct>` structures.

.. _SmokeTestCase-struct:

SmokeTestCase
=============

A structure defining a smoke test case.

.. list-table::
    :header-rows: 1
    :widths: 10 25 65

    * - Property
      - Type
      - Description
    * - id
      - ``string``
      - **Required**. The identifier of the test case. This identifier may be
        used by smoke test implementations to generate test case names. The
        provided ``id`` MUST match Smithy's :token:`smithy:Identifier` ABNF. No
        two test cases can share the same ID, including test cases defined for
        other operations bound to the same service.
    * - params
      - ``document``
      - Defines the input parameters used to generate the request. These
        parameters MUST be compatible with the input of the operation.

        Parameter values that contain binary data MUST be defined using values
        that can be represented in plain text as the plain text representation
        (for example, use ``"foo"`` and not ``"Zm9vCg=="``).
    * - vendorParams
      - ``document``
      - Defines vendor-specific parameters that are used to influence the
        request. For example, some vendors might utilize environment variables,
        configuration files on disk, or other means to influence the
        serialization formats used by clients or servers.

        If ``vendorParamsShape`` is set, these parameters MUST be compatible
        with that shape's definition.
    * - vendorParamsShape
      - ``string``
      - The ID of the shape that should be used to validate the ``vendorParams``
        member contents.

        If set, the parameters in ``vendorParams`` MUST be compatible with this
        shape's definition.
    * - expect
      - :ref:`Expectation <Expectation-union>`
      - **Required**. Defines the kind of response that is expected from the
        service call.
    * - tags
      - ``[string]``
      - Attaches a list of tags that can be used to categorize and group
        test cases. If a test case uses a feature that requires special
        configuration, it should be tagged.

.. _Expectation-union:

Expectation
-----------

A union describing the different kinds of expectations that can be made for a
test case. Exactly one member must be set.

.. list-table::
    :header-rows: 1
    :widths: 10 25 65

    * - Property
      - Type
      - Description
    * - success
      - ``Unit``
      - Indicates that the call is expected to not throw an error. No other
        assertions are made about the response.
    * - failure
      - :ref:`FailureExpectation <FailureExpectation-struct>`
      - Indicates that the call is expected to throw an error.

.. _FailureExpectation-struct:

FailureExpectation
~~~~~~~~~~~~~~~~~~

.. list-table::
    :header-rows: 1
    :widths: 10 25 65

    * - Property
      - Type
      - Description
    * - errorId
      - ``string``
      - Indicates that the call is expected to throw a specific type of error
        matching the targeted shape. If not specified, the error can be of
        any type.

Smoke tests example
===================

The following example defines smoke test cases for an operation which should
return a specific ``InvalidMessageError`` response when given an invalid input,
and return a successful response otherwise.

.. code:: smithy

    $version: "2"

    namespace smithy.example

    use smithy.test#smokeTests

    @smokeTests(
        [
            {
                id: "FooSuccess"
                params: {bar: "2"}
                expect: {
                    success: {}
                }
            }
            {
                id: "FooInvalidMessageError"
                params: {bar: "föö"}
                expect: {
                    failure: {errorId: InvalidMessageError}
                }
            }
        ]
    )
    operation Foo {
        input := {
            bar: String
        }
        errors: [
            InvalidMessageError
        ]
    }

    @error("client")
    structure InvalidMessageError {}
