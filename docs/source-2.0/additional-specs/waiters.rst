.. _waiters:

=======
Waiters
=======

Waiters are a client-side abstraction used to poll a resource until a desired
state is reached, or until it is determined that the resource will never
enter into the desired state. This is a common task when working with
services that are eventually consistent like Amazon S3 or services that
asynchronously create resources like Amazon EC2. Writing logic to
continuously poll the status of a resource can be cumbersome and
error-prone. The goal of waiters is to move this responsibility out of
customer code and onto service teams who know their service best.

For example, waiters can be used in code to turn the workflow of waiting
for an Amazon EC2 instance to be terminated into something like the
following client pseudocode:

.. code-block:: java

    InstanceTerminatedWaiter waiter = InstanceTerminatedWaiter.builder()
            .client(myClient)
            .instanceIds(Collections.singletonList("i-foo"))
            .totalAllowedWaitTime(10, Duration.MINUTES)
            .wait();


.. smithy-trait:: smithy.waiters#waitable
.. _smithy.waiters#waitable-trait:

``smithy.waiters#waitable`` trait
=================================

Summary
    Indicates that an operation has various named "waiters" that can be used
    to poll a resource until it enters a desired state.
Trait selector
    ``operation :not(-[input, output]-> structure > member > union[trait|streaming])``

    (Operations that do not use :ref:`event streams <event-streams>` in their input or output)
Value type
    A ``map`` of :ref:`waiter names <waiter-names>` to
    :ref:`Waiter structures <waiter-structure>`.

The following example defines a waiter that waits until an Amazon S3 bucket
exists:

.. code-block:: smithy
    :emphasize-lines: 4

    $version: "2"
    namespace com.amazonaws.s3

    use smithy.waiters#waitable

    @waitable(
        BucketExists: {
            documentation: "Wait until a bucket exists"
            acceptors: [
                {
                    state: "success"
                    matcher: {
                        success: true
                    }
                }
                {
                    state: "retry"
                    matcher: {
                        errorType: "NotFound"
                    }
                }
            ]
        }
    )
    operation HeadBucket {
        input: HeadBucketInput
        output: HeadBucketOutput
        errors: [NotFound]
    }

Applying the steps defined in `Waiter workflow`_ to the above example,
a client performs the following steps:

1. A ``HeadBucket`` operation is created, given the necessary input
   parameters, and sent to the service.
2. If the operation completes successfully, the waiter transitions to the
   ``success`` state and terminates. This is defined in the first acceptor
   of the waiter that uses the ``success`` matcher.
3. If the operation encounters an error named ``NotFound``, the waiter
   transitions to the ``retry`` state.
4. If the operation fails with any other error, the waiter transitions to
   the ``failure`` state and terminates.
5. The waiter is in the ``retry`` state and continues at step 1 after
   delaying with exponential backoff until the total allowed time to wait
   is exceeded.


.. _waiter-names:

Waiter names
------------

Waiter names MUST be defined using UpperCamelCase and only contain
alphanumeric characters. That is, waiters MUST adhere to the following
ABNF:

.. code-block:: abnf

    waiter-name: upper-alpha *(ALPHA / DIGIT)
    upper-alpha: %x41-5A ; A-Z

.. seealso:: :ref:`waiter-best-practices` for additional best practices
    to follow when naming waiters.

Each waiter in the :ref:`closure of a service <service-closure>` MUST have
a case-insensitively unique waiter name. This limitation helps make it
easier to both understand a service and to generate code for a service
without needing to consider duplicate waiter names across operations.


Waiter workflow
===============

Implementations MUST require callers to provide the total amount of time
they are willing to wait for a waiter to complete. Requiring the caller
to set a deadline removes any surprises as to how long a waiter can
potentially take to complete.

While the total execution time of a waiter is less than the allowed time,
waiter implementations perform the following steps:

1. Call the operation the :ref:`smithy.waiters#waitable-trait` is attached
   to using user-provided input for the operation. Any errors that can be
   encountered by the operation must be caught so that they can be inspected.
2. If the total time of the waiter exceeds the allowed time, the waiter
   SHOULD attempt to cancel any in-progress requests and MUST transition to a
   to a terminal ``failure`` state.
3. For every :ref:`acceptor <waiter-acceptor>` in the waiter:

   1. If the acceptor :ref:`matcher <waiter-matcher>` is a match, transition
      to the :ref:`state <waiter-acceptor-state>` of the acceptor.
   2. If the acceptor transitions the waiter to the ``retry`` state, then
      continue to step 5.
   3. Stop waiting if the acceptor transitions the waiter to the ``success``
      or ``failure`` state.

4. If none of the acceptors are matched *and* an error was encountered while
   calling the operation, then transition to the ``failure`` state and stop
   waiting.
5. Transition the waiter to the ``retry`` state, follow the process
   described in :ref:`waiter-retries`, and continue to step 1.


.. _waiter-retries:

Waiter retries
--------------

Waiter implementations MUST delay for a period of time before attempting a
retry. The amount of time a waiter delays between retries is computed using
exponential backoff with jitter through the following algorithm:

* Let ``attempt`` be the number of retry attempts.
* Let ``attemptCeiling`` be the computed number of attempts necessary before
  ``delay`` with exponential backoff exceeds ``maxDelay``. This is necessary
  to prevent integer overflows for larger numbers of retries.
* Let ``minDelay`` be the minimum amount of time to delay between retries in
  seconds, specified by the ``minDelay`` property of a
  :ref:`waiter <waiter-structure>` with a default of 2.
* Let ``maxDelay`` be the maximum amount of time to delay between retries in
  seconds, specified by the ``maxDelay`` property of a
  :ref:`waiter <waiter-structure>` with a default of 120.
* Let ``random`` be a function that returns a random value between two
  inclusive integers.
* Let ``log`` be a function that returns the natural logarithm for an integer.
* Let ``maxWaitTime`` be a user-provided amount of time in seconds a user is
  willing to wait for a waiter to complete.
* Let ``remainingTime`` be the computed amount of seconds remaining before the
  waiter has exceeded ``maxWaitTime``.

.. code-block:: python

    attemptCeiling = (log(maxDelay / minDelay) / log(2)) + 1

    if attempt > attemptCeiling:
        delay = maxDelay
    else:
        delay = minDelay * 2 ** (attempt - 1)

    delay = random(minDelay, delay)

    if remainingTime - delay <= minDelay:
        delay = remainingTime

If the computed ``delay`` subtracted from ``remainingTime`` is less than
or equal to ``minDelay``, then set ``delay`` to ``remainingTime`` and
perform one last retry. This prevents a waiter from waiting needlessly
only to exceed ``maxWaitTime`` before issuing a final request.

Using the default ``minDelay`` of 2, the default ``maxDelay`` of 120, a caller
provided ``maxWaitTime`` of 300 (5 minutes), and assuming that requests
complete in 0 seconds (for example purposes only), delays might be computed as
follows:

.. list-table::
    :header-rows: 1

    * - Retry ``attempt``
      - ``delay``
      - Cumulative time
      - ``remainingTime``
    * - 1
      - 2
      - 2
      - 298
    * - 2
      - 3
      - 5
      - 295
    * - 3
      - 6
      - 11
      - 289
    * - 4
      - 6
      - 17
      - 283
    * - 5
      - 22
      - 39
      - 261
    * - 6
      - 62
      - 101
      - 199
    * - 7
      - 43
      - 144
      - 156
    * - 8
      - 24
      - 168
      - 132
    * - 9
      - 71
      - 239
      - 61
    * - 10
      - 42
      - 281
      - 19
    * - 11
      - 9
      - 290
      - 10
    * - 12
      - 6
      - 296
      - 4
    * - 13 (last attempt)
      - 4
      - 300
      - N/A

.. note::

    Because waiters use jitter, waiters might use different delays than the
    example table above.


Why exponential backoff with jitter?
------------------------------------

`Exponential backoff with full jitter`_ is used as opposed to other retry
strategies like linear backoff because it should work for most use cases,
balancing the cost to the caller spent waiting on a resource to stabilize,
the cost of the service in responding to polling requests, and the overhead
associated with potentially violating a service level agreement and getting
throttled. Waiters that poll for resources that quickly stabilize will
complete within the first few calls, whereas waiters that could take hours
to complete will send fewer requests as the number of retries increases.

By generally increasing the amount of delay between retries as the number of
retry attempts increases, waiters will not overload services with unnecessary
polling calls, and it protects customers from violating service level
agreements that could counter-intuitively cause waiters to take longer to
complete or even fail due to request throttling. By using introducing
randomness with jitter, waiters will retry slightly more aggressively to
improve the time to completion while still maintaining the general increase
in delay between retries.

Note that linear backoff is still possible to configure with waiters. By
setting ``minDelay`` and ``maxDelay`` to the same value, a waiter will retry
using linear backoff.


.. _waiter-structure:

Waiter structure
================

A *waiter* defines a set of acceptors that are used to check if a resource
has entered into a desired state.

.. list-table::
    :header-rows: 1
    :widths: 10 25 65

    * - Property
      - Type
      - Description
    * - documentation
      - ``string``
      - Documentation about the waiter defined using CommonMark_.
    * - acceptors
      - ``[`` :ref:`Acceptor structure <waiter-acceptor>` ``]``
      - **Required**. An ordered array of acceptors to check after executing
        an operation. The list of ``acceptors`` MUST contain at least one
        acceptor with a ``success`` state transition.
    * - minDelay
      - ``integer``
      - The minimum amount of time in seconds to delay between each retry.
        This value defaults to ``2`` if not specified. If specified, this
        value MUST be greater than or equal to 1 and less than or equal to
        ``maxDelay``.
    * - maxDelay
      - ``integer``
      - The maximum amount of time in seconds to delay between each retry.
        This value defaults to ``120`` if not specified (2 minutes). If
        specified, this value MUST be greater than or equal to 1.
    * - ``deprecated``
      - ``boolean``
      - Indicates if the waiter is considered deprecated. A waiter SHOULD
        be marked as deprecated if it has been replaced by another waiter or
        if it is no longer needed (for example, if a resource changes from
        eventually consistent to strongly consistent).
    * - ``tags``
      - ``[string]``
      - A list of tags associated with the waiter that allow waiters to be
        categorized and grouped.


.. _waiter-acceptor:

Acceptor structure
==================

.. list-table::
    :header-rows: 1
    :widths: 10 25 65

    * - Property
      - Type
      - Description
    * - state
      - ``string``
      - **Required**. The state the acceptor transitions to when matched. The
        string value MUST be a valid :ref:`AcceptorState enum <waiter-acceptor-state>`.
    * - matcher
      - :ref:`Matcher structure <waiter-matcher>`
      - **Required.** The matcher used to test if the resource is in a state
        that matches the requirements needed for a state transition.


.. _waiter-acceptor-state:

AcceptorState enum
==================

Acceptors cause a waiter to transition into one of the following states:

.. list-table::
    :header-rows: 1
    :widths: 20 80

    * - Name
      - Description
    * - success
      - The waiter successfully finished waiting. This is a terminal state
        that causes the waiter to stop.
    * - failure
      - The waiter failed to enter into the desired state. This is a terminal
        state that causes the waiter to stop.
    * - retry
      - The waiter will retry the operation. This state transition is
        implicit if no accepter causes a state transition.


.. _waiter-matcher:

Matcher union
=============

A *matcher* defines how an acceptor determines if it matches the current
state of a resource. A matcher is a union where exactly one of the following
members MUST be set:

.. list-table::
    :header-rows: 1
    :widths: 10 25 65

    * - Property
      - Type
      - Description
    * - output
      - :ref:`PathMatcher structure <waiter-PathMatcher>`
      - Matches on the successful output of an operation using a
        JMESPath_ expression. This matcher MUST NOT be used on operations
        with no output. This matcher is checked only if an operation
        completes successfully.
    * - inputOutput
      - :ref:`PathMatcher structure <waiter-PathMatcher>`
      - Matches on both the input and output of an operation using a JMESPath_
        expression. Input parameters are available through the top-level
        ``input`` field, and output data is available through the top-level
        ``output`` field. This matcher is checked only if an operation
        completes successfully.
    * - success
      - ``boolean``
      - When set to ``true``, matches when an operation returns a successful
        response. When set to ``false``, matches when an operation fails with
        any error. This matcher is checked regardless of if an operation
        succeeds or fails with an error.
    * - errorType
      - ``string``
      - Matches if an operation returns an error of an expected type. If an
        absolute :ref:`shape ID <shape-id>` is provided, the error is
        matched only based on the name part of the shape ID. A relative shape
        name MAY be provided to match errors that are not defined in the
        model.

        The ``errorType`` matcher SHOULD refer to errors that are associated
        with an operation through its ``errors`` property, though some
        operations might need to refer to framework errors or lower-level
        errors that are not defined in the model.


.. _waiter-PathMatcher:

PathMatcher structure
=====================

The ``output`` and ``inputOutput`` matchers test the result of a JMESPath_
expression against an expected value. These matchers are structures that
support the following members:

.. list-table::
    :header-rows: 1
    :widths: 10 25 65

    * - Property
      - Type
      - Description
    * - path
      - ``string``
      - **Required.** A JMESPath expression applied to the input or output
        of an operation.
    * - expected
      - ``string``
      - **Required.** The expected return value of the expression.
    * - comparator
      - ``string``
      - **Required.** The comparator used to compare the result of the
        ``expression`` with the ``expected`` value. The string value MUST
        be a valid :ref:`PathComparator-enum`.


JMESPath data model
-------------------

The data model exposed to JMESPath_ for input and output structures is
converted from Smithy types to `JMESPath types`_ using the following
conversion table:

.. list-table::
    :header-rows: 1

    * - Smithy type
      - JMESPath type
    * - blob
      - string (base64 encoded)
    * - boolean
      - boolean
    * - byte
      - number
    * - short
      - number
    * - integer
      - number
    * - long
      - number [#fnumbers]_
    * - float
      - number
    * - double
      - number
    * - bigDecimal
      - number [#fnumbers]_
    * - bigInteger
      - number [#fnumbers]_
    * - string
      - string
    * - timestamp
      - number [#ftimestamp]_
    * - document
      - any type
    * - list and set
      - array
    * - map
      - object
    * - structure
      - object [#fstructure]_
    * - union
      - object [#funion]_

Footnotes
~~~~~~~~~

.. [#fnumbers] ``long``, ``bigInteger``, ``bigDecimal`` are exposed as
   numbers to JMESPath. If a value for one of these types truly exceeds
   the value of a double (the native numeric type of JMESPath), then
   querying these types in a waiter is a bad idea.
.. [#ftimestamp] ``timestamp`` values are represented in JMESPath expressions
   as epoch seconds with optional decimal precision. This allows for
   timestamp values to be used with relative comparators like ``<`` and ``>``.
.. [#fstructure] Structure members are referred to by member name and not
   the data sent over the wire. For example, the :ref:`jsonname-trait` is not
   respected in JMESPath expressions that select structure members.
.. [#funion] ``union`` values are represented exactly like structures except
   only a single member is set to a non-null value.


JMESPath static analysis
------------------------

Smithy implementations that can statically analyze JMESPath expressions
MAY emit a :ref:`validation event <validation>` with an event ID of
``WaitableTraitJmespathProblem`` and a :ref:`severity of DANGER <severity-definition>`
if one of the following problems are detected in an expression:

1. A JMESPath expression does not return a value that matches the expected
   return type of a :ref:`PathComparator-enum`
2. A JMESPath expression attempts to extract or operate on invalid model data.

If such a problem is detected but is intentional, a
:ref:`suppression <suppression-definition>` can be used to ignore the error.


.. _PathComparator-enum:

PathComparator enum
===================

Each ``PathMatcher`` structure contains a ``comparator`` that is used to
check the result of a JMESPath expression against an expected value. A
comparator can be set to any of the following values:

.. list-table::
    :header-rows: 1
    :widths: 20 60 20

    * - Name
      - Description
      - Required JMESPath return type
    * - stringEquals
      - Matches if the return value of a JMESPath expression is a string
        that is equal to an expected string.
      - ``string``
    * - booleanEquals
      - Matches if the return value of a JMESPath expression is a boolean
        that is equal to an expected boolean. The ``expected`` value of a
        ``PathMatcher`` MUST be set to "true" or "false" to match the
        corresponding boolean value.
      - ``boolean``
    * - allStringEquals
      - Matches if the return value of a JMESPath expression is an array that
        contains at least one value, and every value in the array is a string
        that equals an expected string.
      - ``array`` of ``string``
    * - anyStringEquals
      - Matches if the return value of a JMESPath expression is an array and
        any value in the array is a string that equals an expected string.
      - ``array`` of ``string``


Waiter examples
===============

This section provides examples for various features of waiters.

The following example defines a ``ThingExists`` waiter that waits until the
``status`` member in the output of the ``GetThing`` operation returns
``"success"``. This example makes use of a "fail-fast"; in this example, if
a "Thing" has a ``failed`` status, then it can never enter the desired
``success`` state. To address this and prevent needlessly waiting on a
success state that can never happen, a ``failure`` state transition is
triggered if the ``status`` property equals ``failed``.

.. code-block:: smithy

    $version: "2"
    namespace smithy.example

    use smithy.waiters#waitable

    @waitable(
        ThingExists: {
            description: "Waits until a thing has been created"
            acceptors: [
                // Fail-fast if the thing transitions to a "failed" state.
                {
                    state: "failure"
                    matcher: {
                        output: {
                            path: "status"
                            comparator: "stringEquals"
                            expected: "failed"
                        }
                    }
                }
                // Succeed when the thing enters into a "success" state.
                {
                    state: "success"
                    matcher: {
                        output: {
                            path: "status"
                            comparator: "stringEquals"
                            expected: "success"
                        }
                    }
                }
            ]
        }
    )
    operation GetThing {
        input: GetThingInput
        output: GetThingOutput
    }

    @input
    structure GetThingInput {
        @required
        name: String
    }

    @output
    structure GetThingOutput {
        status: String
    }

Both input and output data can be queried using the ``inputOutput`` matcher.
The following example waiter completes successfully when the number of
provided groups on input matches the number of provided groups on output:

.. code-block:: smithy

    $version: "2"
    namespace smithy.example

    use smithy.waiters#waitable

    @waitable(
        GroupExists: {
            acceptors: [
                {
                    inputOutput: {
                        path: "length(input.groups) == length(output.groups)"
                        expected: "true"
                        comparator: "booleanEquals"
                    }
                }
            ]
        }
    )
    operation ListGroups {
        input: ListGroupsInput
        output: ListGroupsOutput
    }


.. _waiter-best-practices:

Waiter best-practices
=====================

The following non-normative section outlines best practices for defining
and implementing waiters.


Keep JMESPath expressions simple
--------------------------------

Overly complex JMESPath_ expressions can easily lead to bugs. While static
analysis of JMESPath expressions can give some level of confidence in
expressions, it does not guarantee that the logic encoded in the
expression is correct. If it's overly difficult to describe a waiter for
a particular use-case, consider if the API itself is overly complex and
needs to be simplified.


Name waiters after the resource and state
-----------------------------------------

Waiters SHOULD be named after the resource name and desired state, for example
``<Resource><StateName>``. "StateName" SHOULD match the expected state
name of the resource where possible. For example, if a "Snapshot" resource
can enter a "deleted" state, then the waiter name should be
``SnapshotDeleted`` and not ``SnapshotRemoved``.

Good
    * ObjectExists
    * ConversionTaskDeleted
Bad
    The following examples are bad because they are named after the completion
    of an operation rather than the state of the resource:

    * RunInstanceComplete
    * TerminateInstanceComplete

    More appropriate names would be:

    * InstanceRunning
    * InstanceTerminated

.. note::

    A common and acceptable exception to this rule are ``<Resource>Exists``
    and ``<Resource>NotExists`` waiters.


Do not model implicit acceptors
-------------------------------

Implicit acceptors are unnecessary and can quickly become incomplete as new
resource states and errors are added. Waiters have 2 implicit
:ref:`acceptors <waiter-acceptor>`:

* (Step 4) - If none of the acceptors are matched *and* an error was
  encountered while calling the operation, then transition to the
  ``failure`` state and stop waiting.
* (Step 5) - Transition the waiter to the ``retry`` state, follow the
  process described in :ref:`waiter-retries`, and continue to step 1.

This means it is unnecessary to model an acceptor with an "errorType"
:ref:`matcher <waiter-matcher>` that transitions to a state of "failure".
This is already the default behavior. For example, the following acceptor
is unnecessary:

.. code-block:: smithy

    {
        acceptors: [
            {
                state: "failure"
                matcher: {
                    errorType: "ValidationError"
                }
            }
            // other acceptors...
        ]
    }

Because a successful request that does not match any acceptor by default
transitions to the :ref:`retry state <waiter-acceptor-state>`, there is no
need to model matchers with a state of retry unless the matcher is for
specific errors. For example, the following matcher is unnecessary:

.. code-block:: smithy

    {
        acceptors: [
            {
                state: "retry"
                matcher: {
                    success: true
                }
            }
            // other acceptors...
        ]
    }


Only model terminal failure states
----------------------------------

Waiters SHOULD only model terminal failure states. A *terminal failure state*
is a resource state in which the resource cannot transition to the desired
success state without a user taking some explicit action. Only modeling
terminal failure states keeps waiter configurations as minimal as possible,
and it allows for more flexibility in the future. By avoiding the use of
intermediate resource states for waiter failure state transitions, a service
can add other intermediate states in the future without affecting existing
waiter logic.

For example, suppose a resource has the following state transitions, and
if a resource is in the "Stopped" state, it can only transition to "Running"
if the user invokes the "StartResource" API operation:

.. code-block::
    :caption: **Figure Waiters-1.1**: Example resource state transitions
    :name: waiters-figure-1.1
    :class: no-copybutton

              User calls
             StopResource
    ┌──────────┐        ┌──────────┐        ┌──────────┐
    │ Creating │───────▶│ Stopping │───────▶│ Stopped  │
    └──────────┘        └──────────┘        └──────────┘
          │                                       │
          │                                       │    User calls
          │                                       │   StartResource
          │                                       ▼
          │                                 ┌──────────┐
          └────────────────────────────────▶│ Starting │
                                            └──────────┘
                                                  │
                                                  │
                                                  │
                                                  ▼
                                            ┌──────────┐
                                            │  Running │
                                            └──────────┘

A "ResourceRunning" waiter for the above resource SHOULD NOT include
the intermediate state transition "Stopping" to fail-fast. Instead, a failure
transition should be defined that matches on the terminal "Stopped" state
because the only way to transition from "Stopped" to running is by invoking
the ``StartResource`` API operation.

.. code-block:: smithy

    @waitable(
        ResourceRunning: {
            description: "Waits for the resource to be running"
            acceptors: [
                {
                    state: "failure"
                    matcher: {
                        output: {
                            path: "State"
                            expected: "Stopped"
                            comparator: "stringEquals"
                        }
                    }
                }
                {
                    state: "success"
                    matcher: {
                        output: {
                            path: "State"
                            expected: "Running"
                            comparator: "stringEquals"
                        }
                    }
                }
                // other acceptors...
            ]
        }
    )
    operation GetResource {
        input: GetResourceInput
        output: GetResourceOutput
    }


.. _CommonMark: https://spec.commonmark.org/
.. _JMESPath: https://jmespath.org/
.. _JMESPath types: https://jmespath.org/specification.html#data-types
.. _Exponential backoff with full jitter: https://aws.amazon.com/builders-library/timeouts-retries-and-backoff-with-jitter/#Jitter
