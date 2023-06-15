.. _selectors:

=========
Selectors
=========

A :dfn:`Smithy selector` is a domain specific language (DSL) used to match
shapes within a model. Selectors are used to build custom
:ref:`validators <EmitEachSelector>` and to specify where it is valid to
apply a :ref:`trait <defining-traits>`.


Introduction
============

A *selector* is used to traverse a model as a graph and find matching shapes.
A model can be thought of as a traversable graph, more specifically, as a
labeled multidigraph: each :ref:`shape <shapes>` in a model is a labeled graph
vertex, and each :ref:`shape ID <shape-id>` and :ref:`member <member>` is a
labeled edge. Shapes that are one of the :ref:`aggregate types <aggregate-types>`
or :ref:`service types <service-types>` have named relationships to *neighbors*
that they reference or are connected to. For example, a :ref:`list <list>`
shape has a member that targets a shape; thus, the list shape is connected
to the member shape, and the member shape is connected to the targeted shape.


Matching shapes with selectors
==============================

Every shape in a model is sent through a selector, one at a time. The
individual shape that is sent through a selector is called the
*starting shape*. When given a shape, a selector *yields* zero or more
matching shapes. Any :ref:`variables <selector-variables>` defined
while evaluating a starting shape are cleared when evaluating the next
starting shape.

Selectors can be composed of multiple selectors. Selectors are evaluated
from left to right, yielding the results from one selector to the next.
The following selector matches all string shapes marked as
:ref:`sensitive <sensitive-trait>`:

.. code-block:: none

    string [trait|sensitive]

The result of evaluating a selector is either the set of shapes that are
yielded from each starting shape, or for more advanced use cases, the list
of all shapes yielded by a selector along with the variables currently
assigned when the shape was yielded.

.. seealso::

    Refer to :ref:`EmitEachSelector` for more information on how to use
    selector results.


Matching shapes by type
=======================

Shapes can be matched by type using the following tokens:

.. list-table::
    :header-rows: 1
    :widths: 10 90

    * - Token
      - Description
    * - ``*``
      - Matches all shapes
    * - ``number``
      - Matches all ``byte``, ``short``, ``integer``, ``long``, ``float``,
        ``double``, ``bigDecimal``, and ``bigInteger`` shapes
    * - ``simpleType``
      - Matches all :ref:`simple types <simple-types>`
    * - ``collection``
      - Deprecated: Matches both a ``list`` and ``set`` shape.
        This is considered an alias for ``list``.
    * - ``blob``
      - Matches blob shapes
    * - ``boolean``
      - Matches boolean shapes
    * - ``document``
      - Matches document shapes
    * - ``string``
      - Matches string shapes
    * - ``integer``
      - Matches integer shapes
    * - ``byte``
      - Matches byte shapes
    * - ``short``
      - Matches short shapes
    * - ``long``
      - Matches long shapes
    * - ``float``
      - Matches float shapes
    * - ``double``
      - Matches double shapes
    * - ``bigDecimal``
      - Matches bigDecimal shapes
    * - ``bigInteger``
      - Matches bigInteger shapes
    * - ``timestamp``
      -  Matches timestamp shapes
    * - ``list``
      - Matches list shapes. Note that set shapes also match ``list`` because
        they are considered sub-types of list.
    * - ``set``
      - Deprecated: Matches set shapes. This is considered an alias for ``list``.
    * - ``map``
      -  Matches map shapes
    * - ``structure``
      - Matches structure shapes
    * - ``union``
      - Matches union shapes
    * - ``service``
      - Matches service shapes
    * - ``operation``
      - Matches operation shapes
    * - ``resource``
      - Matches resource shapes
    * - ``member``
      -  Matches member shapes

The following selector matches shapes in a model:

.. code-block:: none

    *

The following selector matches all numbers defined in a model:

.. code-block:: none

    number


Attribute selectors
===================

*Attribute selectors* are used to match shapes based on
:ref:`shape IDs <shape-id>`, :ref:`traits <traits>`, and other
:ref:`attributes <selector-attributes>`.


.. _attribute-existence:

Attribute existence
-------------------

An attribute existence check tests for the existence of an attribute without
any kind of comparison.

The following selector matches shapes that are marked with the
:ref:`deprecated-trait`:

.. code-block:: none

    [trait|deprecated]

:ref:`Projection values <projection-attribute>` are only considered to
exist if they yield one or more results.

The following selector matches shapes that have an :ref:`enum-trait`,
the trait contains at least one ``enum`` entry, and one or more entries
contains a non-empty ``tags`` list.

.. code-block:: none

    [trait|enum|(values)|tags|(values)]


Attribute comparison
--------------------

An attribute selector with a :token:`comparator <selectors:SelectorComparator>`
checks for the existence of an attribute and compares the resolved
attribute value to a comma separated list of possible values. The
resolved attribute value on the left hand side of the comparator MUST
match one or more of the comma separated values on the right hand
side of the comparator.

There are three kinds of comparators:

* :ref:`String comparators <string-comparators>`
* :ref:`Numeric comparators <numeric-comparators>`
* :ref:`Projection comparators <projection-comparators>`


.. _string-comparators:

String comparators
------------------

:token:`String comparators <selectors:SelectorStringComparator>` are used to compare
the string representation of values. Attributes that do not have a string
representation are treated as an empty string when these comparisons are
performed.

.. list-table::
    :header-rows: 1
    :widths: 10 90

    * - Comparator
      - Description
    * - ``=``
      - Matches if the attribute value is equal to the comparison value.
        This comparator never matches if either value does not exist.


        The following selector matches shapes in the "smithy.example"
        namespace.

        .. code-block:: none

            [id|namespace = 'smithy.example']

        The following selector matches shapes that have the :ref:`since-trait`
        with a value of ``2019`` or ``2020``:

        .. code-block:: none

            [trait|since = 2019, 2020]
    * - ``!=``
      - Matches if the attribute value is not equal to the comparison value.
        This comparator never matches if either value does not exist.

        The following selector matches shapes that are not in the
        "smithy.example" namespace.

        .. code-block:: none

            [id|namespace != 'smithy.example']
    * - ``^=``
      - Matches if the attribute value starts with the comparison value.
        This comparator never matches if either value does not exist.

        The following selector matches shapes where the name starts with "_".

        .. code-block:: none

            [id|name ^= '_']
    * - ``$=``
      - Matches if the attribute value ends with the comparison value.
        This comparator never matches if either value does not exist.

        The following selector matches shapes where the name ends with "_".

        .. code-block:: none

            [id|name $= '_']
    * - ``*=``
      - Matches if the attribute value contains the comparison value.
        This comparator never matches if either value does not exist.

        The following selector matches shapes where the name contains "_".

        .. code-block:: none

            [id|name *= '_']
    * - ``?=``
      - Matches based on the existence of a value. This comparator uses the
        same rules defined in :ref:`attribute-existence`. The comparator
        matches if the value exists and the right hand side of the comparator
        is ``true``, or if the value does not exist and the right hand side
        of the comparator is set to ``false``. This selector is most useful
        in :ref:`scoped attribute selectors <scoped-attribute-selectors>`.

        The following selector matches shapes marked as ``required``.

        .. code-block:: none

            [trait|required ?= true]

String comparisons can be made case-insensitive by preceding the closing
bracket with ``i``.

The following selector matches shapes that have a :ref:`httpQuery-trait`
that case-insensitively contains the word "token":

.. code-block:: none

    [trait|httpQuery *= token i]


.. _numeric-comparators:

Numeric comparators
-------------------

Relative comparators only match if both values being compared contain valid
:token:`smithy:Number` productions when converted to a string.

.. list-table::
    :header-rows: 1
    :widths: 10 90

    * - Comparator
      - Description
    * - ``>``
      - Matches if the attribute value is greater than the comparison value.

        The following selector matches shapes with an :ref:`httpError-trait` value
        that is greater than `500`:

        .. code-block:: none

            [trait|httpError > 500]
    * - ``>=``
      - Matches if the attribute value is greater than or equal to the
        comparison value.
    * - ``<``
      - Matches if the attribute value is less than the comparison value.
    * - ``<=``
      - Matches if the attribute value is less than or equal to the
        comparison value.

If either value is not a valid number, then the selector does not match.

The following selector does not match any shapes because the comparison value
is not a valid number:

.. code-block:: none

    [trait|httpError >= "not a number!"]


.. _selector-attributes:

Attributes
==========

Attribute selectors return objects that MAY have nested properties. Objects
returned from selectors MAY be available to cast to a string. Shapes support
the following attributes:

.. list-table::
    :header-rows: 1
    :widths: 10 90

    * - Attribute
      - Description
    * - :ref:`id <id-attribute>`
      - Returns an object that contains the shape ID of a shape.
    * - :ref:`trait <trait-attribute>`
      - Returns an object that contains the traits applied to a shape.
    * - :ref:`service <service-attribute>`
      - Returns an object that contains information about service shapes.
    * - :ref:`var <var-attribute>`
      - Returns an object that contains the
        :ref:`variables <selector-variables>` currently in scope.

Nested properties of an attribute object can be selected using subsequent
pipe (``|``) delimited property names.

The following selector matches shapes that have a :ref:`range-trait`
with a ``min`` property set to ``1``:

.. code-block:: none

    [trait|range|min = 1]

Whitespace is insignificant. The following selector is equivalent to the
above selector:

.. code-block:: none

    [trait | range
        | min = 1 ]


.. _id-attribute:

``id`` attribute
----------------

The ``id`` attribute of a shape returns an object that contains information
about the shape ID of a shape. When used as a string, ``id`` contains the
absolute :ref:`shape ID <shape-id>` of a shape.

The following selector matches only the ``foo.baz#Structure`` shape:

.. code-block:: none

    [id = foo.baz#Structure]

Matching on a shape ID that contains a member requires that the shape ID
is enclosed in single or double quotes:

.. code-block:: none

    [id = 'foo.baz#Structure$foo']

**Properties**

The ``id`` attribute can be used as an object, and it supports the
following properties.

``namespace``
    Gets the :token:`smithy:Namespace` part of a shape ID.

    The following selector matches shapes in the ``foo.baz`` namespace:

    .. code-block:: none

        [id|namespace = 'foo.baz']
``name``
    Gets the name part of a shape ID.

    The following selector matches shapes named ``MyShape``.

    .. code-block:: none

        [id|name = MyShape]
``member``
    Gets the member part of a shape ID (if available). If the shape ID does
    not contain a member, an *empty value* is returned.

    The following selector matches all members in the model that have a member
    name of ``foo``.

    .. code-block:: none

        [id|member = foo]
``(length)``
    The ``(length)`` property returns the length of the absolute shape ID.

    The following selector matches shapes where the absolute shape ID is
    longer than 80 characters:

    .. code-block:: none

        [id|(length) > 80]

    Note that the ``(length)`` property can also be applied to the result of
    the ``namespace``, ``name``, and ``member`` properties.

    The following selector matches shapes where the member name is longer
    than 20 characters:

    .. code-block:: none

        [id|member|(length) > 20]


.. _service-attribute:

``service`` attribute
---------------------

The ``service`` attribute is an object that is available for service shapes.
The following selector matches all service shapes:

.. code-block:: none

    [service]

However, the intent of the above selector is more clearly stated using the
following selector:

.. code-block:: none

    service

When compared to a string value, the ``service`` attribute returns the
absolute shape ID of the service shape.

The following selector matches all service shapes with a shape ID of
``smithy.example#MyService``:

.. code-block:: none

    [service = smithy.example#MyService]

**Properties**

The ``service`` attribute supports the following properties:

``id``
    Returns the service shape ID as an :ref:`id-attribute`.
``version``
    Gets the version property of a service shape as a string.

    The following selector matches all service shapes that have a version
    property that starts with ``2018-``:

    .. code-block:: none

        [service|version ^= '2018-']


.. _trait-attribute:

``trait`` attribute
-------------------

The ``trait`` attribute returns an object that contains every trait applied
to a shape. The ``trait`` attribute supports the following properties:

``(keys)``
    The ``(keys)`` property returns a :ref:`projection <projection-attribute>`
    that contains the shape ID of every trait applied to a shape.

    The following selector matches shapes that apply a trait from the
    ``smithy.example`` namespace:

    .. code-block:: none

        [trait|(keys)|namespace = 'smithy.example']
``(values)``
    The ``(values)`` property returns a :ref:`projection <projection-attribute>`
    that contains every trait attached to a shape as a
    :ref:`node value <node-attribute>`.

    The following selector matches shapes that apply a trait that
    contains a top-level structure member named ``tags``:

    .. code-block:: none

        [trait|(values)|tags]
``(length)``
    The ``(length)`` property returns the number of traits applied to a
    shape.

    The following selector matches shapes with more than 10 traits
    applied to it:

    .. code-block:: none

        [trait|(length) > 10]
``*``
    Any other value is treated as a shape ID, where a relative shape ID is
    resolved to the ``smithy.api`` namespace. If a matching trait with the
    given shape ID is attached to the shape, it's :ref:`node value <node-attribute>`
    is returned. An :ref:`empty value <empty-attributes>` is returned if the
    trait does not exist.

    The following selector matches shapes that have the
    :ref:`deprecated-trait`:

    .. code-block:: none

        [trait|smithy.api#deprecated]

    Traits in the ``smithy.api`` namespace MAY be retrieved from the ``trait``
    attribute without a namespace.

    .. code-block:: none

        [trait|deprecated]

    Traits are converted to their serialized :token:`node <smithy:NodeValue>` form
    when matching against their values. Only string, boolean, and numeric
    values can be compared using a :ref:`string comparator <string-comparators>`.
    Boolean values are converted to "true" or "false". Numeric values are
    converted to their string representation.

    The following selector matches shapes with the :ref:`error-trait` set to
    ``client``:

    .. code-block:: none

        [trait|error = client]

    The following selector matches shapes that have the :ref:`error-trait`
    where its value is not ``client``:

    .. code-block:: none

        [trait|error != client]

    The following selector matches shapes with the :ref:`documentation-trait`
    with a value that contains "TODO" or "FIXME":

    .. code-block:: none

        [trait|documentation *= TODO, FIXME]

.. note::

    The ``trait`` attribute returns an empty string when compared with
    a string comparator.


.. _node-attribute:

Node attribute
--------------

A *node attribute* is created by retrieving nested values from a ``trait``
attribute. The node value created from a trait is defined in :ref:`trait-node-values`.
A node that contains a string, number, or boolean value is converted to a
string value when used by :ref:`string comparators <string-comparators>`
(where a boolean creates a string containing "true" or "false"). Other node
values return empty strings when used by string comparators.

**Properties**

``(keys)``
    When applied to an object node, the ``(keys)`` property returns a
    :ref:`projection <projection-attribute>` that contains all of the
    keys of the object. When applied to any other kind of node, an
    empty value is returned.

    The following selector matches shapes that have an
    :ref:`externalDocumentation-trait` with an entry named ``Homepage``:

    .. code-block:: none

        [trait|externalDocumentation|(keys) = Homepage]
``(values)``
    When applied to an array node, the ``(values)`` property returns a
    :ref:`projection <projection-attribute>` that contains every value in
    the array. When applied to an object node, ``(values)`` returns a
    projection that contains every value in the object. When applied to
    any other kind of node, an empty value is returned.

    The following selector matches shapes that have an :ref:`enum-trait`
    where one or more of the enum definitions has a ``tags`` property list
    in which one or more values in the list equals ``internal``:

    .. code-block:: none

        [trait|enum|(values)|tags|(values) = internal]
``(length)``
    When applied to an array node, the ``(length)`` property returns the
    number of items in the array. When applied to an object node, the
    ``(length)`` property returns the number of entries in the object. When
    applied to a string node, the ``(length)`` property returns the number of
    characters in the string. When applied to any other kind of node, an
    empty value is returned.

    The following selector matches shapes that have a
    :ref:`documentation-trait` value that is less than 3 characters:

    .. code-block:: none

        [trait|documentation|(length) < 3]
``*``
    Properties of an object node can be accessed by name.

    The following selector matches shapes that have an
    ``externalDocumentation`` trait that defines an entry named
    ``Reference Docs``:

    .. code-block:: none

        [trait|externalDocumentation|'Reference Docs']

    Attempting to access a nested property that does not exist or
    attempting to descend into nested values of a scalar type returns
    an :ref:`empty value <empty-attributes>`.


.. _empty-attributes:

Empty attribute
---------------

Attempting to access a trait that does not exist, a variable that does
not exist, or attempting to descend into node attribute values that do not
exist returns an *empty value*. An empty value does not satisfy existence
checks, returns an empty string when used with string comparators, and
returns an empty value when attempting to access any properties.

The following selector attempts to descend into non-existent properties of
the :ref:`documentation-trait`. This example MUST NOT cause an error and
MUST NOT match any shapes:

.. code-block:: none

    [trait|documentation|invalid|child = Hi]


.. _projection-attribute:

Projection attribute
--------------------

A *projection* is created using the ``(keys)`` or ``(values)`` property of
a :ref:`trait attribute <trait-attribute>` or
:ref:`node attribute <node-attribute>`.

**Properties**

Projections support the following properties:

``(first)``
    Recursively flattens the values of a projection and returns the
    first value. Projections are unordered. This property SHOULD only be
    used when the contents of a projection are known to have a single value.
``*``
    All other property access is forwarded to each value stored in the
    projection, and the results are returned in a new projection.


.. _projection-comparisons:

Comparisons to non-projections
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

When a projection is compared against a value that is not also a projection,
the comparison matches if any value in the projection satisfies the
comparator assertion against the other value.

The following selector matches shapes that have a :ref:`tags-trait` that
contains a value that is the string literal value ``foo``:

.. code-block:: none

    [trait|tags|(values) = foo]


Comparisons to projections
~~~~~~~~~~~~~~~~~~~~~~~~~~

When a projection is compared against another projection using a
:ref:`string comparator <string-comparators>` or :ref:`numeric comparator <numeric-comparators>`,
the comparison matches if any value in the left projection satisfies the
comparator when compared against any value in the right projection.

To illustrate an example, the following model defines a trait,
``allowedTags``, that is meant to constrain the set of tags that can appear
in the closure of a service.

.. code-block:: smithy

    namespace smithy.example

    @trait(selector: "service")
    list allowedTags {
        member: String,
    }

    @allowedTags(["internal", "external"])
    service MyService {
        version: "2020-04-28",
        operations: [OperationA, OperationB, OperationC, OperationD]
    }

    operation OperationA {
        input: OperationAInput,
    }

    @tags(["internal"])
    operation OperationB {}

    @tags(["internal", "external"])
    operation OperationC {}

    @tags(["invalid"])
    operation OperationD {}

    @input
    structure OperationAInput {
        badValue: BadEnum,
        goodValue: GoodEnum,
    }

    @enum([
        {value: "a", tags: ["internal"]},
        {value: "b", tags: ["invalid"]},
    ])
    string BadEnum

    @enum([
        {value: "a"},
        {value: "b", tags: ["internal", "external"]},
        {value: "c", tags: ["internal"]},
    ])
    string GoodEnum


The following selector finds all shapes within the closure of a service
that applies the ``allowedTags`` trait, where the shape applies a
``tags`` trait that is not part of the ``allowedTags`` trait.

.. code-block:: none

    service
    [trait|smithy.example#allowedTags]
    $service(*)
    ~>
    [trait|tags]
    :not([@: @{trait|tags|(values)} = @{var|service|trait|smithy.example#allowedTags|(values)}])

When the above selector is applied to the example model, it matches the
``smithy.example#OperationD`` shape because it uses a ``tags`` value of
``invalid``.

It might be useful to also ensure that ``tags`` added inside of ``enum``
traits adhere to the ``allowedTags`` trait. For example, the
``smithy.example#BadEnum`` shape has an ``enum`` definition that contains
an invalid tag, ``invalid``. The following selector tries, **and fails**,
to find all shapes that apply the ``enum`` trait where one of the ``enum``
definitions uses a tag that is not allowed.

.. code-block:: none

    service
    [trait|smithy.example#allowedTags]
    $service(*)
    ~>
    [trait|enum]
    :not([@: @{trait|enum|(values)|tags|(values)}
             = @{var|service|trait|smithy.example#allowedTags|(values)}])

The above selector fails to match any shapes in the model because of how
projections are compared. The ``@{trait|enum|(values)|tags|(values)}``
value creates a projection that contains every ``tags`` value found in
every ``enum`` trait value of a shape. The
``@{var|service|trait|smithy.example#allowedTags|(values)}`` attribute
creates a projection that gets the set of ``allowedTags`` from the previously
captured ``service`` :ref:`variable <selector-variables>`. Because ``BadEnum``
defines both a valid and invalid ``enum`` ``tags`` value, it satisfies the
``=`` comparator, which is then negated with the :ref:`:not function <not-function>`,
which means the shape does not match the selector. Projection comparators are
needed to solve this problem.

Building on the previous example, a :ref:`projection comparator <projection-comparators>`
can be used to correctly find shapes in which an ``enum`` trait uses ``tags``
that are not part of the set of ``allowedTags``.

.. code-block:: none

    service
    [trait|smithy.example#allowedTags]
    $service(*)
    ~>
    [trait|enum]
    :not([@: @{trait|enum|(values)|tags|(values)}
             {<} @{var|service|trait|smithy.example#allowedTags|(values)}])


.. _projection-comparators:

Projection comparators
~~~~~~~~~~~~~~~~~~~~~~

Projection comparators are used to compare projections to test if they are
equal, not equal, a subset, or a proper subset to another projection. With
the exception of the ``{!=}`` comparator, projection comparators match if and
only if both the left hand side of the comparator and the right hand side of
the comparator are projections.

.. list-table::
    :header-rows: 1
    :widths: 10 90

    * - Comparator
      - Description
    * - ``{=}``
      - Matches if every value in the left hand side can be found in
        the right hand side using the ``=`` comparator for equality.
        Projection comparisons are unordered, and the projections are not
        required to have the same number of items.
    * - ``{!=}``
      - This comparator is the negation of the result of ``{=}``. Comparing
        a projection to a non-projection value will always return ``true``.
    * - ``{<}``
      - Matches if the left projection is a *subset* of the right
        projection. Every value in the left projection MUST be found
        in the right projection using the ``=`` comparator for equality.
    * - ``{<<}``
      - Matches if the left projection is a *proper subset* of the right
        projection. Every value in the left projection MUST be found in
        the right projection using the ``=`` comparator for equality,
        but the projections themselves are not equal, meaning that the left
        projection is missing one or more values found in the right
        projection.


.. _scoped-attribute-selectors:

Scoped attribute selectors
==========================

A :token:`scoped attribute selector <selectors:SelectorScopedAttr>` is similar to an
attribute selector, but it allows multiple complex comparisons to be made
against a scoped attribute.


Context values
--------------

The first part of a scoped attribute selector is the attribute that is scoped
for the expression, followed by ``:``. The scoped attribute is accessed using
a :token:`context value <selectors:SelectorContextValue>` in the form of
``@{`` :token:`smithy:Identifier` ``}``.

In the following selector, the ``trait|range`` attribute is used as the scoped
attribute of the expression, and the selector matches shapes marked with
the :ref:`range-trait` where the ``min`` value is greater than the ``max``
value:

.. code-block:: none

    [@trait|range: @{min} > @{max}]

The scope can also be set to the current shape being evaluated by omitting
an expression before the ``:`` character.

The following selector matches shapes that are traits that are applied
to themselves as traits (for example, this matches ``smithy.api#trait``,
``smithy.api#documentation``, etc.):

.. code-block:: none

    [trait|trait][@: @{trait|(keys)} = @{id}]

A :ref:`projection <projection-attribute>` MAY be used as the scoped
attribute context value. When the scoped attribute context value is a
projection, each recursively flattened value of the projection is
individually tested against each assertion. If any value from the
projection matches the assertions, then the selector matches the shape.

The following selector matches shapes that have an :ref:`enum-trait` where one
or more of the enum definitions is both marked as ``deprecated`` and contains
an entry in its ``tags`` property named ``deprecated``.

.. code-block:: none

    [@trait|enum|(values):
        @{deprecated} = true &&
        @{tags|(values)} = "deprecated"]


And-logic
---------

Scoped attribute selector assertions can be combined together using
*and* statements with ``&&``. All assertions MUST match in order for
the selector to match.

The following selector matches shapes with the :ref:`idRef-trait` that
set ``failWhenMissing`` to ``true`` and omit an ``errorMessage``:

.. code-block:: none

    [@trait|idRef:
        @{failWhenMissing} = true &&
        @{errorMessage} ?= false]


Matching multiple values
------------------------

Like non-scoped selectors, multiple values can be provided using a comma
separated list. One or more resolved attribute values MUST match one or more
provided values.

The following selector matches shapes with the :ref:`httpApiKeyAuth-trait`
where the ``in`` property is ``header`` and the ``name`` property is neither
``x-api-token`` or ``authorization``:

.. code-block:: none

    [@trait|httpApiKeyAuth:
        @{name} = header &&
        @{in} != 'x-api-token', 'authorization']


Case insensitive comparisons
----------------------------

The ``i`` token used before ``&&`` or the closing ``]`` makes a comparison
case-insensitive.

The following selector matches on the ``httpApiKeyAuth`` trait using
case-insensitive string comparisons:

.. code-block:: none

    [@trait|httpApiKeyAuth:
        @{name} = header i &&
        @{in} != 'x-api-token', 'authorization' i]

The following selector matches on the ``httpApiKeyAuth`` trait but only
uses a case-insensitive comparison on ``in``:

.. code-block:: none

    [@trait|httpApiKeyAuth:
        @{name} = header &&
        @{in} != 'x-api-token', 'authorization' i]


Neighbors
=========

Neighbor selectors yield shapes that are connected to the current shape.
Most selectors are used to determine if a shape matches some criteria,
meaning the selector yields zero or exactly one shape. However, neighbor
selectors yield zero or more shapes by traversing the relationships of
a shape.


Forward undirected neighbor
----------------------------

A :token:`forward undirected neighbor <selectors:SelectorForwardUndirectedNeighbor>`
(``>``) yields every shape that is connected to the current shape. For
example, the following selector matches the key and value members of
every map:

.. code-block:: none

    map > member

Neighbors can be chained to traverse further into a shape. The following
selector yields strings that are targeted by list members:

.. code-block:: none

    list > member > string


Forward directed neighbors
--------------------------

The forward undirected neighbor selector (``>``) is an *undirected* edge
traversal. Sometimes, a directed edge traversal is necessary. For example,
the following selector matches the "bound", "input", "output", and "error"
relationships of each operation:

.. code-block:: none

    operation > *

A forward directed edge traversal is applied using :token:`selectors:SelectorForwardDirectedNeighbor`
(``-[X, Y, Z]->``). The following selector matches all structure shapes
referenced as operation ``input`` or ``output``.

.. code-block:: none

    operation -[input, output]-> structure

The :ref:`:test <test-function>` function can be used to check if a shape
has a named relationship. The following selector matches all resource
shapes that define an identifier:

.. code-block:: none

    resource :test(-[identifier]->)

Relationships from a shape to the traits applied to the shape can be traversed
using a forward directed relationship named ``trait``. It is atypical to
traverse ``trait`` relationships, therefore they are only yielded by
selectors when explicitly requested using a ``trait`` directed relationship.
The following selector finds all service shapes that have a protocol trait
applied to it (that is, a trait that is marked with the
:ref:`protocolDefinition-trait`):

.. code-block:: none

    service :test(-[trait]-> [trait|protocolDefinition])


Forward recursive neighbors
---------------------------

The forward recursive neighbor selector (``~>``) yields all shapes that are
recursively connected in the closure of another shape. The shapes yielded
by this selector are equivalent to yielding every shape connected to the
current shape using a forward undirected neighbor, yielding every shape
connected to those shapes, and so on.

The following selector matches operations that are connected to a service:

.. code-block:: none

    service ~> operation

The following selector finds operations that do not have the :ref:`http-trait`
that are in the closure of a service marked with the ``aws.protocols#restJson``
trait:

.. code-block:: none

    service[trait|aws.protocols#restJson1]
        ~> operation:not([trait|http])


Reverse undirected neighbor
---------------------------

A *reverse undirected neighbor* yields all of the shapes that have a
relationship that points to the current shape.

The following selector matches strings that are targeted by members of lists:

.. code-block:: none

    string :test(< member < list)

The following selector yields all shapes that are not traits and are not
referenced by other shapes:

.. code-block:: none

    :not([trait|trait]) :not(< *)

The following selectors are equivalent; however, a forward neighbor traversal
is preferred over a reverse neighbor traversal when possible.

.. code-block:: none

    * Reverse: string < member < list
    * Forward: list :test(> member > string)


Reverse directed neighbor
-------------------------

A *reverse directed neighbor* yields all of the shapes that have a
relationship of a specific type that points to the current shape.

For example, shapes marked with the :ref:`streaming-trait` can only
be targeted by top-level members of operation input or output structures.
The following selector finds all shapes that target a streaming shape
and violate this constraint:

.. code-block:: none

    [trait|streaming]
    :test(<)
    :not(< member < structure <-[input, output]- operation)

Like forward directed neighbors, ``trait`` relationships are only included
when explicitly provided in the list of relationships to traverse. The
following selector yields all shapes that are traits that are not applied
to any shapes:

.. code-block:: none

    [trait|trait] :not(<-[trait]-)


.. _selector-relationships:

Relationships
-------------

The table below lists the labeled directed relationships from each shape.

.. list-table::
    :header-rows: 1
    :widths: 15 15 70

    * - Shape
      - Relationship
      - Description
    * - service
      - operation
      - Each operation that is bound to a service.
    * - service
      - resource
      - Each resource that is bound to a service.
    * - service
      - error
      - Each error structure referenced by the service (if present).
    * - resource
      - identifier
      - The identifier referenced by the resource (if specified).
    * - resource
      - operation
      - Each operation that is bound to a resource through the
        "operations", "create", "put", "read", "update", "delete", and "list"
        properties.
    * - resource
      - instanceOperation
      - Each operation that is bound to a resource through the
        "operations", "put", "read", "update", and "delete" properties.
    * - resource
      - collectionOperation
      - Each operation that is bound to a resource through the
        "collectionOperations", "create", and "list" properties.
    * - resource
      - resource
      - Each resource that is bound to a resource.
    * - resource
      - create
      - The operation referenced by the :ref:`create-lifecycle` property of
        a resource (if present).
    * - resource
      - read
      - The operation referenced by the :ref:`read-lifecycle` property of
        a resource (if present).
    * - resource
      - update
      - The operation referenced by the :ref:`update-lifecycle` property of
        a resource (if present).
    * - resource
      - delete
      - The operation referenced by the :ref:`delete-lifecycle` property of
        a resource (if present).
    * - resource
      - list
      - The operation referenced by the :ref:`list-lifecycle` property of
        a resource (if present).
    * - resource
      - bound
      - The service or resource to which the resource is bound.
    * - operation
      - bound
      - The service or resource to which the operation is bound.
    * - operation
      - input
      - The input structure of the operation (if present).

        .. note::

            :ref:`smithy.api#Unit <unit-type>` is considered "not present"
            for this relationship, and will not be yielded.

    * - operation
      - output
      - The output structure of the operation (if present).

        .. note::

            :ref:`smithy.api#Unit <unit-type>` is considered "not present"
            for this relationship, and will not be yielded.

    * - operation
      - error
      - Each error structure referenced by the operation (if present).
    * - list
      - member
      - The :ref:`member` of the list. Note that this is not the shape targeted
        by the member.
    * - map
      - member
      - The key and value members of the map. Note that these are not the
        shapes targeted by the member.
    * - structure
      - member
      - Each structure member. Note that these are not the shapes targeted by
        the members.
    * - union
      - member
      - Each union member. Note that these are not the shapes targeted by
        the members.
    * - member
      -
      - The shape targeted by the member. Note that member targets have no
        relationship name.
    * - ``*``
      - trait
      - Each trait applied to a shape. The neighbor shape is the shape that
        defines the trait. This kind of relationship is only traversed if the
        ``trait`` relationship is explicitly stated as a desired directed
        neighbor relationship type.

.. important::

    Implementations MUST tolerate parsing unknown relationship types. When
    evaluated, the directed traversal of unknown relationship types yields
    no shapes.


Functions
=========

Functions are used to filter and yield shapes using a variadic argument
list of selectors separated by a comma (``,``). Functions always start with
a colon (``:``).

.. important::

    Implementations MUST tolerate parsing unknown function names. When
    evaluated, an unknown function yields no shapes.


.. _test-function:

``:test``
---------

The ``:test`` function is used to test if a shape is matched by any of the
provided predicate selectors. The ``:test`` function stops testing predicates
and yields the current shape as soon as the first predicate yields a shape.

The following selector is used to match all list shapes that target a string:

.. code-block:: none

    list:test(> member > string)

The above selector is very different from the following selector because the
following selector returns only string shapes that are targeted by the members
of list shapes:

.. code-block:: none

    list > member > string

The following selector matches shapes that are bound to a resource
(for example, identifiers, operations, child resources) and have
no documentation:

.. code-block:: none

    :test(-[bound, resource]->)
    :not([trait|documentation])


``:is``
-------

The ``:is`` function passes the current shape to each selector and
yields the shapes yielded by each selector.

The following selector yields string and number shapes:

.. code-block:: none

    :is(string, number)

The following selector yields string and number shapes that are targeted
by a member:

.. code-block:: none

    member > :is(string, number)

The following selector yields shapes that are either targeted by a list
member or targeted by a map member:

.. code-block:: none

    :is(list > member > *, map > member > *)

.. note::

    This function was previously named ``:each``. Implementations that wish
    to maintain backward compatibility with the old function name MAY
    treat ``:each`` as an alias for ``:is``, and models that use ``:each``
    SHOULD update to use ``:is``.


.. _not-function:

``:not``
--------

The ``:not`` function is used to filter out shapes. This function MUST be
provided a **single** predicate selector argument. If the predicate
selector yields any shapes when given the current shape as input, then
the current shape is not yielded by the function.

The following selector does not yield string shapes:

.. code-block:: none

    :not(string)

The following selector does not yield string or float shapes:

.. code-block:: none

    :not(string) :not(float)

The following selector yields list shapes that do not target strings:

.. code-block:: none

    list :not(> member > string)

The following selector yields structure members that do not have the
``length`` trait, and the member targets a string that does not have
the ``length`` trait:

.. code-block:: none

    structure > member
        :not([trait|length])
        :test(> string :not([trait|length]))

The following selector yields service shapes that do not have a protocol
trait applied to it:

.. code-block:: none

    service :not(-[trait]-> [trait|protocolDefinition])


``:topdown``
------------

The ``:topdown`` function matches service, resource, and operation shapes
and resource and operation shapes within their containment hierarchy. The
``:topdown`` function starts at each given shape and forward-traverses
the containment hierarchy of the shape by following ``operation`` and
``resource`` :ref:`relationships <selector-relationships>` from the shape
to its neighbors; this function *does not* traverse *up* the containment
hierarchy of a given shape to check if the shape is within the containment
hierarchy of a qualified service or resource shape. This function essentially
allows shapes to be matched by inheriting from the resource or service they
are bound to.

.. rubric:: Selector arguments

Exactly one or two selectors MUST be provided to the ``:topdown`` selector:

1. The first selector is the "qualifier". It is used to mark a shape as a
   match. If the selector yields any results, then it is considered a match.
2. If provided, the second selector is called the "disqualifier". It is used
   to remove the match flag for the current shape before traversing any
   resource and operation bindings of the current shape. If this selector
   yields any results, then the shape is not considered a match, and bound
   resources and operations are not considered a match until the qualifier
   selector matches again. Resource and operation binding traversal continues
   regardless of if the second selector removes the match flag for the current
   shape because resource and operation shapes bound to the current shape
   could yield matching results.

.. rubric:: Examples

The following selector finds all service, resource, and operation shapes that
are marked with the ``aws.api#dataPlane`` trait or that are bound within the
containment hierarchy of resource and service shapes that are marked as such:

.. code-block:: none

    :topdown([trait|aws.api#dataPlane])

The following selector finds all service, resource, and operation shapes that
are marked with the ``aws.api#dataPlane`` trait, but does not match shapes
where the ``aws.api#controlPlane`` trait is used to override the
``aws.api#dataPlane`` trait. For example, if a service is marked with the
``aws.api#dataPlane`` trait to provide a default setting for all resources and
operations within the service, the ``aws.api#controlPlane`` trait can be used
to override the default.

.. code-block:: none

    :topdown([trait|aws.api#dataPlane], [trait|aws.api#controlPlane])

The above selector applied to the following model matches ``Example``,
``OperationA``, and ``OperationB``. It does not match ``Foo`` because ``Foo``
matches the disqualifier selector.

.. code-block:: smithy

    namespace smithy.example

    @aws.api#dataPlane
    service Example {
        version: "2020-09-08",
        resources: [Foo],
        operations: [OperationA],
    }

    operation OperationA {}

    @aws.api#controlPlane
    resource Foo {
        operations: [OperationB]
    }

    @aws.api#dataPlane
    operation OperationB {}

In the following example, the ``:topdown`` function does not inherit any
matches from service shapes because the selector only sends resource shapes
to the function. When applied to the previous example model, the following
selector matches only ``OperationB``.

.. code-block:: none

    resource :topdown([trait|aws.api#dataPlane], [trait|aws.api#controlPlane])


.. _selector-variables:

Variables
=========

Variables are used to store eagerly computed, named intermediate results that
can be accessed later in a selector. Variables are useful for caching
results that are computed multiples times in a selector or for capturing
information about the current shape that is referenced later in a selector
after traversing neighbors.

A variable is set using a :token:`selectors:SelectorVariableSet` expression.
Variables can be reassigned without error.

The following selector defines a variable named ``foo`` that sets the
variable to the result of applying the ``*`` selector to the current shape.

.. code-block:: none

    $foo(*)

A variable is retrieved by name using a :token:`selectors:SelectorVariableGet`
expression. Retrieving a variable yields the set of shapes stored in the
variable. Attempting to get a variable that does not exist yields no shapes.

.. code-block:: none

    ${foo}

Variables can also be accessed inside of :ref:`scoped attribute selectors <scoped-attribute-selectors>`
from shapes using the ``var`` attribute.


.. _var-attribute:

``var`` attribute
-----------------

A *var attribute* is an object accessible from a shape that provides
access to the named :ref:`variables <selector-variables>` currently in scope.
Variables are accessed by providing the variable name after ``var``. The
values returned from ``var`` are :ref:`projections <projection-attribute>`
that contain the set of shapes that were bound to the variable, or an
:ref:`empty value <empty-attributes>` if the variable does not exist.

The following selector finds all operations in the closure of a service
where the operation has an :ref:`auth-trait` that is not a subset of the
:ref:`authDefinition traits <authDefinition-trait>` applied to the service.

.. code-block:: none

    service
    $authTraits(-[trait]-> [trait|authDefinition])
    ~>
    operation
    [trait|auth]
    :not([@: @{trait|auth|(values)} {<} @{var|authTraits|id}]))

Given the following model, the selector matches the ``HasDigestAuth``
operation:

.. code-block:: smithy

    namespace smithy.example

    @httpBasicAuth
    @httpBearerAuth
    service MyService {
        version: "2020-04-21",
        operations: [HasDigestAuth, HasBasicAuth, NoAuth]
    }

    @auth([httpDigestAuth])
    operation HasDigestAuth {}

    @auth([httpBasicAuth])
    operation HasBasicAuth {}

    operation NoAuth {}

The ``HasDigestAuth`` operation is matched because it is bound within the
closure of ``MyService``, it has an ``auth`` trait set to ``httpDigestAuth``,
and ``MyService`` does not apply the ``httpDigestAuth`` trait.

The above selector is equivalent to the following pseudo-code:

.. code-block:: python

    matched_shapes = set()
    for model.shapes as current_shape:
        # service
        if current_shape.type != "service":
            continue
        # $authTraits(-[trait]-> [trait|authDefinition])
        auth_traits = []
        for current_shape.traits as trait:
            if "smithy.api#authDefinition" in trait.traits:
                auth_traits.append(trait)
        # ~>
        for current_shape.get_recursive_neighbors() as current_shape:
            # operation
            if current_shape.type != "operation":
                continue
            # [trait|auth]
            if "smithy.api#auth" not in current_shape.traits:
                continue
            # :not([@: @{trait|auth|(values)} {<} @{var|authTraits|id}]))
            __trait_auth_values_projection = current_shape.traits.get("smithy.api#auth").values
            __auth_traits_id_projection = auth_traits.get("id")
            if not __trait_auth_values_projection.issubset(__auth_traits_id_projection):
                matched_shapes.add(current_shape)


Grammar
=======

Selectors are defined by the following ABNF_ grammar.

.. admonition:: Lexical note
   :class: note

   Whitespace is insignificant and can occur between any token without
   changing the semantics of a selector.

.. productionlist:: selectors
    Selector                           :`SelectorExpression` *(`SelectorExpression`)
    SelectorExpression                 :`SelectorShapeTypes`
                                       :/ `SelectorAttr`
                                       :/ `SelectorScopedAttr`
                                       :/ `SelectorFunction`
                                       :/ `SelectorForwardUndirectedNeighbor`
                                       :/ `SelectorReverseUndirectedNeighbor`
                                       :/ `SelectorForwardDirectedNeighbor`
                                       :/ `SelectorForwardRecursiveNeighbor`
                                       :/ `SelectorReverseDirectedNeighbor`
                                       :/ `SelectorVariableSet`
                                       :/ `SelectorVariableGet`
    SelectorShapeTypes                 :"*" / `smithy:Identifier`
    SelectorForwardUndirectedNeighbor  :">"
    SelectorReverseUndirectedNeighbor  :"<"
    SelectorForwardDirectedNeighbor    :"-[" `SelectorDirectedRelationships` "]->"
    SelectorReverseDirectedNeighbor    :"<-[" `SelectorDirectedRelationships` "]-"
    SelectorDirectedRelationships      :`smithy:Identifier` *("," `smithy:Identifier`)
    SelectorForwardRecursiveNeighbor   :"~>"
    SelectorAttr                       :"[" `SelectorKey` [`SelectorAttrComparison`] "]"
    SelectorAttrComparison             :`SelectorComparator` `SelectorAttrValues` ["i"]
    SelectorKey                        :`smithy:Identifier` ["|" `SelectorPath`]
    SelectorPath                       :`SelectorPathSegment` *("|" `SelectorPathSegment`)
    SelectorPathSegment                :`SelectorValue` / `SelectorFunctionProperty`
    SelectorValue                      :`SelectorText` / `smithy:Number` / `smithy:RootShapeId`
    SelectorFunctionProperty           :"(" `smithy:Identifier` ")"
    SelectorAttrValues                 :`SelectorValue` *("," `SelectorValue`)
    SelectorComparator                 :`SelectorStringComparator`
                                       :/ `SelectorNumericComparator`
                                       :/ `SelectorProjectionComparator`
    SelectorStringComparator           :"^=" / "$=" / "*=" / "!=" / "=" / "?="
    SelectorNumericComparator          :">=" / ">" / "<=" / "<"
    SelectorProjectionComparator       :"{=}" / "{!=}" / "{<}" / "{<<}"
    SelectorAbsoluteRootShapeId        :`smithy:Namespace` "#" `smithy:Identifier`
    SelectorScopedAttr                 :"[@" [`SelectorKey`] ":" `SelectorScopedAssertions` "]"
    SelectorScopedAssertions           :`SelectorScopedAssertion` *("&&" `SelectorScopedAssertion`)
    SelectorScopedAssertion            :`SelectorScopedValue` `SelectorComparator` `SelectorScopedValues` ["i"]
    SelectorScopedValue                :`SelectorValue` / `SelectorContextValue`
    SelectorContextValue               :"@{" `SelectorPath` "}"
    SelectorScopedValues               :`SelectorScopedValue` *("," `SelectorScopedValue`)
    SelectorFunction                   :":" `smithy:Identifier` "(" `SelectorFunctionArgs` ")"
    SelectorFunctionArgs               :`Selector` *("," `Selector`)
    SelectorText                       :`SelectorSingleQuotedText` / `SelectorDoubleQuotedText`
    SelectorSingleQuotedText           :"'" 1*`SelectorSingleQuotedChar` "'"
    SelectorDoubleQuotedText           :DQUOTE 1*`SelectorDoubleQuotedChar` DQUOTE
    SelectorSingleQuotedChar           :%x20-26 / %x28-5B / %x5D-10FFFF ; Excludes (')
    SelectorDoubleQuotedChar           :%x20-21 / %x23-5B / %x5D-10FFFF ; Excludes (")
    SelectorVariableSet                :"$" `smithy:Identifier` "(" `Selector` ")"
    SelectorVariableGet                :"${" `smithy:Identifier` "}"


Compliance Tests
================

Selector compliance tests are used to verify the behavior of selectors. Each compliance test is written as a Smithy file
and includes a :ref:`metadata <metadata>` called ``selectorTests``. This metadata contains a list of test cases, each including a selector,
the expected matched shapes, and additional configuration options. The test case contains the following properties:

.. list-table::
    :header-rows: 1
    :widths: 10 20 70

    * - Property
      - Type
      - Description
    * - selector
      - ``string``
      - **REQUIRED** The selector to match shapes within the smithy model
    * - matches
      - ``list<shape ID>``
      - **REQUIRED** The expected shapes ID of the matched shapes
    * - skipPreludeShapes
      - ``boolean``
      - Skip :ref:`prelude shapes <prelude>` when comparing the expected shapes and the actual shapes returned from the selector. Default value is ``false``

Below is an example selector compliance test:

.. code-block:: smithy

    $version: "1.0"

    metadata selectorTests = [
        {
            selector: "[trait|length|min > 1]"
            matches: [
                smithy.example#AtLeastTen
            ]
        }
        {
            selector: "[trait|length|min >= 1]"
            skipPreludeShapes: true
            matches: [
                smithy.example#AtLeastOne
                smithy.example#AtLeastTen
            ]
        }
        {
            selector: "[trait|length|min < 2]"
            skipPreludeShapes: true
            matches: [
                smithy.example#AtLeastOne
            ]
        }
    ]

    namespace smithy.example

    @length(min: 1)
    string AtLeastOne

    @length(max: 5)
    string AtMostFive

    @length(min: 10)
    string AtLeastTen

The compliance tests can also be accessed in this
`directory <https://github.com/awslabs/smithy/tree/main/smithy-model/src/test/resources/software/amazon/smithy/model/selector/cases>`__
of the Smithy Github repository.

.. _ABNF: https://tools.ietf.org/html/rfc5234
.. _set: https://en.wikipedia.org/wiki/Set_(abstract_data_type)
