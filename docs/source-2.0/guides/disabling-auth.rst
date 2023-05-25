.. _disabling-auth:

========================
Disabling Authentication
========================

Services should, in general, have authentication configured. However, there are
times when authentication may need to be disabled for the service or for a
number of operations within a service. For example, you may want to expose
to expose a public route for users to use for logging in that does not
require authentication.

This guide demonstrates how to disable authentication on both a per-operation
and a service-wide basis.

--------------------------------------
Disabling Authentication for a Service
--------------------------------------

In Smithy, a service’s authentication schemes are determined by the presence
of a trait with the :ref:`@authDefinition <authDefinition-trait>` meta trait.
See :ref:`Authentication Traits <authentication-traits>` for more details on
defining and using authentication schemes. If you do not want your service to
have authentication, do not apply any authentication traits to the service
shape.

The following example shows a service with no auth trait configured:

.. code-block:: smithy

    $version: "2.0"

    namespace com.example

    service MyService {
        version: "2020-08-31",
        operations: [
            MyOperation,
        ]
    }

    operation MyOperation {}


-----------------------------------------
Disabling Authentication for an Operation
-----------------------------------------

If you have an individual operation or set of operations that should not be
called with any authentication scheme, you can disable authentication by
applying the :ref:`@auth <auth-trait>` trait to the operation with an empty
list as the trait value.

The following example shows a service whose default authentication scheme is
:ref:`@httpBearerAuth <httpBearerAuth-trait>`. Authentication is disabled for a
single operation, ``UnauthenticatedOperation`` in that service:

.. code-block:: smithy

    $version: "2.0"

    namespace com.example

    @httpBearerAuth
    service AuthenticatedService {
        version: "2020-08-31",
        operations: [
            MyOperation,
        ]
    }

    @auth([])
    operation UnauthenticatedOperation {}

.. note::
    Disabling authentication for an operation is distinct from applying the
    :ref:`@optionalAuth <optionalAuth-trait>` trait to an operation. An
    operation with the ``@optionalAuth`` trait _must_ be callable both with and
    without authentication.
