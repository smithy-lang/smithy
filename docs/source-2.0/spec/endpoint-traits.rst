---------------
Endpoint traits
---------------

.. smithy-trait:: smithy.api#endpoint
.. _endpoint-trait:

``endpoint`` trait
==================

Summary
    Configures a custom operation endpoint.
Trait selector
    ``operation``
Value type
    ``structure``

The ``endpoint`` trait is a structure that contains the following members:

.. list-table::
    :header-rows: 1
    :widths: 10 10 80

    * - Property
      - Type
      - Description
    * - hostPrefix
      - ``string``
      - **Required** The ``hostPrefix`` property defines a template that expands
        to a valid *host* as defined in :rfc:`3986#section-3.2.2`.
        ``hostPrefix`` MAY contain :ref:`label placeholders <endpoint-Labels>`
        that reference top-level input members of the operation marked with the
        :ref:`hostLabel-trait`. The ``hostPrefix`` MUST NOT contain a scheme,
        userinfo, or port.

        .. warning::

            A host prefix that contains labels SHOULD end in a period (``.``) as
            otherwise there is a risk of clients inadvertently sending data to
            a domain that you do not control.

The following example defines an operation that uses a custom endpoint:

.. code-block:: smithy

    $version: "2"
    namespace smithy.example

    @readonly
    @endpoint(hostPrefix: "{foo}.data.")
    operation GetStatus {
        input: GetStatusInput
        output: GetStatusOutput
    }

    @input
    structure GetStatusInput {
        @required
        @hostLabel
        foo: String
    }


.. _endpoint-Labels:

Labels
------

``hostPrefix`` patterns MAY contain label placeholders. :dfn:`Labels` consist
of label name characters surrounded by open and closed braces (for example,
"{label_name}" is a label and ``label_name`` is the label name). Every label
MUST correspond to a top-level operation input member, the input member MUST
be marked as :ref:`required <required-trait>`, the input member MUST have the
:ref:`hostLabel-trait`, and the input member MUST reference a string.

Given the following operation,

.. code-block:: smithy

    @readonly
    @endpoint(hostPrefix: "{foo}.data.")
    operation GetStatus {
        input: GetStatusInput
        output: GetStatusOutput
    }

    @input
    structure GetStatusInput {
        @required
        @hostLabel
        foo: String
    }

and the following value provided for ``GetStatusInput``,

::

    "foo" = "abc"

the expanded ``hostPrefix`` evaluates to ``abc.data.``.

Any number of labels can be included within a pattern, provided that they are
not immediately adjacent and do not have identical label names.

Given the following operation,

.. code-block:: smithy

    @readonly
    @endpoint(hostPrefix: "{foo}-{bar}.data.")
    operation GetStatus {
        input: GetStatusInput
        output: GetStatusOutput
    }

    structure GetStatusInput {
        @required
        @hostLabel
        foo: String

        @required
        @hostLabel
        bar: String
    }

and the following values provided for ``GetStatusInput``,

::

    "foo" = "abc"
    "bar" = "def"

the expanded ``hostPrefix`` evaluates to ``abc-def.data.``.

Labels MUST NOT be adjacent in a ``hostPrefix``. The following operation is
invalid because the ``{foo}`` and ``{bar}`` labels are adjacent:

.. code-block:: smithy

    @readonly
    @endpoint(hostPrefix: "{foo}{bar}.data.")
    operation GetStatus {
        input: GetStatusInput
        output: GetStatusOutput
    }

.. _endpoint-ClientBehavior:

Client Behavior
---------------

If an API operation is decorated with an endpoint trait, a client MUST expand
the ``hostPrefix`` template and prepend the expanded value to the client's
endpoint host prior to its use. Clients MUST fail when expanding a
``hostPrefix`` template if the value of any labeled member is empty or null.

After the ``hostPrefix`` template is expanded, a client MUST prepend the
expanded value to the client's derived endpoint host. The client MUST NOT add
any additional characters between the ``hostPrefix`` and client derived
endpoint host. The resolved host value MUST result in a valid
:rfc:`3986#section-3.2.2` host.

Clients SHOULD provide a way for users to disable the ``hostPrefix`` injection
behavior. If a user sets this flag, the client MUST NOT perform any
``hostPrefix`` expansion and MUST NOT prepend the prefix to the client derived
host. The client MUST serialize members to any modeled target location
regardless of this flag.

The ``hostLabel`` trait MUST NOT affect the protocol-specific serialization
logic of a member.

Given the following operation,

.. code-block:: smithy

    @readonly
    @endpoint(hostPrefix: "{foo}.data.")
    @http(method: "GET", uri: "/status")
    operation GetStatus {
        input: GetStatusInput
        output: GetStatusOutput
    }

    structure GetStatusInput {
        @required
        @hostLabel
        @httpHeader("X-Foo")
        foo: String
    }

and the following value provided for ``GetStatusInput``,

::

    "foo" = "abc"

the expanded ``hostPrefix`` evaluates to ``abc.data.`` AND the ``X-Foo`` HTTP
header will contain the value ``abc``.


.. smithy-trait:: smithy.api#hostLabel
.. _hostLabel-trait:

``hostLabel`` trait
===================

Summary
    Binds a top-level operation input structure member to a label in the
    hostPrefix of an endpoint trait.
Trait selector
    ``structure > member[trait|required] :test(> string)``

    *Any required member of a structure that targets a string*
Value type
    Annotation trait

Operations marked with the :ref:`endpoint-trait` MAY contain labels in the
``hostPrefix`` property. These labels reference top-level operation input
structure members that MUST be annotated with the ``hostLabel`` trait. The
contents of the label match the member's name. For example, a host prefix
value of ``{spam}.eggs.`` MUST apply to an operation whose input contains a
member named ``spam`` that is annotated with the ``hostLabel`` trait. Any
``hostLabel`` trait applied to a member that is not a top-level input member
to an operation marked with the :ref:`endpoint-trait` will be ignored.

.. code-block:: smithy

    $version: "2"
    namespace smithy.example

    @readonly
    @endpoint(hostPrefix: "{foo}.data.")
    operation GetStatus {
        input: GetStatusInput
        output: GetStatusOutput
    }

    structure GetStatusInput {
        @required
        @hostLabel
        foo: String
    }

