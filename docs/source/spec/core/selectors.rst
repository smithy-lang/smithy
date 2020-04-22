.. _selectors:

=========
Selectors
=========

A :dfn:`Smithy selector` is a domain specific language (DSL) used to match
specific shapes within a model. Selectors are used to build custom
:ref:`validators <EmitEachSelector>` and to specify where it is valid to
apply a trait.

.. contents:: Table of contents
    :depth: 2
    :local:
    :backlinks: none


Introduction
============

A loaded Smithy *model* can be thought of as a traversable graph, more
specifically, as a labeled multidigraph: each :ref:`shape <shapes>` in a model
is a labeled graph vertex, and each :ref:`shape ID <shape-id>` and
:ref:`member <member>` is a labeled edge. Shapes that are one of the
:ref:`aggregate types <aggregate-types>` or :ref:`service types <service-types>`
have named relationships to *neighbors* that they reference or are connected
to. For example, a :ref:`list` shape has a member that targets a shape; thus,
the list shape is connected to the member shape, and the member shape is
connected to the targeted shape.


Matching shapes with selectors
==============================

Every shape in a model, including members defined in shapes of aggregate types,
is sent through a selector, and all of the shapes returned from the selector
are the matching shapes.


Matching shapes by type
=======================

Shapes can be matched by type using the following tokens:
``blob``, ``boolean``, ``document``, ``string``, ``integer``, ``byte``,
``short``, ``long``, ``float``, ``double``, ``bigDecimal``, ``bigInteger``,
``timestamp``, ``list``, ``map``, ``set``, ``structure``, ``union``,
``service``, ``operation``, ``resource``, ``member``, ``number``,
``simpleType``, ``collection``, ``*``.

* ``number`` matches all ``byte``, ``short``, ``integer``, ``long``, ``float``,
  ``double``, ``bigDecimal``, and ``bigInteger`` shapes.
* ``simpleType`` matches all :ref:`simple types <simple-types>`.
* ``collection`` matches both a ``list`` and ``set`` shape.
* ``*`` matches all shapes.

The following selector matches all string shapes in a model:

.. code-block:: none

    string

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

Projected values from the :ref:`values-projection` and :ref:`keys-projection`
are only considered present if they yield one or more results.

The following example matches all shapes that have an ``enum`` trait,
the trait contains at least one ``enum`` entry, and one or more entries
contains a non-empty ``tags`` list.

.. code-block:: none

    [trait|enum|(values)|tags|(values)]


Attribute comparison
--------------------

An attribute selector with a :token:`comparator <selector_comparator>`
checks for the existence of an attribute and compares the resolved
attribute values to a comma separated list of values.

The following selector matches shapes that have the :ref:`documentation-trait`
with a value set to an empty string:

.. code-block:: none

    [trait|documentation=""]

Multiple values can be provided using a comma separated list. One or more
resolved attribute values MUST match one or more provided values.

The following selector matches shapes that have the :ref:`tags-trait` in
which one or more tags matches either "foo" or "baz".

.. code-block:: none

    [trait|tags|(values)=foo, baz]

Attribute comparisons can be made case-insensitive by preceding the closing
bracket with ``i``.

The following selector matches shapes that have a :ref:`httpQuery-trait`
that case-insensitively contains the word "token":

.. code-block:: none

    [trait|httpQuery*=token i]


Attribute comparators
---------------------

Attribute selectors support the following
:token:`comparators <selector_comparator>`:

.. list-table::
    :header-rows: 1
    :widths: 10 90

    * - Comparator
      - Description
    * - ``=``
      - Matches if the attribute value is equal to the comparison value.
    * - ``!=``
      - Matches if the attribute value is not equal to the comparison value.
        Note that this comparator is never matched if the resolved attribute
        does not exist.
    * - ``^=``
      - Matches if the attribute value starts with the comparison value.
    * - ``$=``
      - Matches if the attribute value ends with the comparison value.
    * - ``*=``
      - Matches if the attribute value contains with the comparison value.
    * - ``>``
      - Matches if the attribute value is greater than the comparison value.
    * - ``>=``
      - Matches if the attribute value is greater than or equal to the
        comparison value.
    * - ``<``
      - Matches if the attribute value is less than the comparison value.
    * - ``<=``
      - Matches if the attribute value is less than or equal to the
        comparison value.
    * - ``?=``
      - Matches if the attribute value on the left hand side of the comparator
        *exists* and matches the existence assertion on the right hand side.
        This comparator uses the same rules defined in :ref:`attribute-existence`.
        The comparator matches if the value exists and the right hand side of
        the comparator is ``true``, or if the value does not exist and the
        right hand side of the comparator is set to ``false``.


Relative comparators
~~~~~~~~~~~~~~~~~~~~

The ``<``, ``<=``, ``>``, ``>=`` comparators only match if both the attribute
value and comparison value contain valid :token:`number` productions. If
either is not a number, then the selector does not match.

The following selector matches shapes that have an :ref:`httpError-trait`
value that is greater than or equal to `500`:

.. code-block:: none

    [trait|httpError >= 500]

The following selector is equivalent:

.. code-block:: none

    [trait|httpError >= '500']

The following selector does not match any shapes because the comparison value
is not a valid number:

.. code-block:: none

    [trait|httpError >= "not a number!"]


.. _selector-attributes:

Attributes
==========

Selector attributes return objects that MAY have nested properties. Objects
returned from selectors MAY be available to cast to a string.

.. important::

    Implementations MUST NOT fail when unknown attribute keys are
    encountered; implementations SHOULD emit a warning and match no results
    when an unknown attribute is encountered.


``id`` attribute
----------------

The ``id`` attribute returns an object that can be evaluated as a string.
When used as a string, ``id`` contains the full :ref:`shape ID <shape-id>`
of a shape.

The following example matches only the ``foo.baz#Structure`` shape:

.. code-block:: none

    [id=foo.baz#Structure]

Matching on a shape ID that contains a member requires that the shape ID
is enclosed in single or double quotes:

.. code-block:: none

    [id='foo.baz#Structure$foo']


``id`` properties
~~~~~~~~~~~~~~~~~

The ``id`` attribute can be used as an object and has the
following properties.

``namespace`` (``string``)
    Gets the :token:`namespace` part of a shape ID.

    The following example matches all shapes in the ``foo.baz`` namespace:

    .. code-block:: none

        [id|namespace='foo.baz']
``name`` (``string``)
    Gets the name part of a shape ID.

    The following example matches all shapes in the model that have a shape
    name of ``MyShape``.

    .. code-block:: none

        [id|name=MyShape]
``member`` (``string``)
    Gets the member part of a shape ID (if available).

    The following example matches all members in the model that have a member
    name of ``foo``.

    .. code-block:: none

        [id|member=foo]
``(length)``
    The :ref:`(length) attribute function <attribute-function-properties>`
    returns the length of the :token:`absolute shape ID <absolute_shape_id>`
    as a string.

    The following example matches all shapes with an absolute shape ID that
    is longer than 100 characters:

    .. code-block:: none

        [id|(length) > 100]


``service`` attribute
---------------------

The ``service`` attribute is an object that is available for service shapes.
The following selector matches all service shapes:

.. code-block:: none

    [service]

The intent of the above selector is more clearly stated using the following
selector:

.. code-block:: none

    service

When compared to a string value, the ``service`` attribute returns an
empty string.


``service`` properties
~~~~~~~~~~~~~~~~~~~~~~

The ``service`` attribute contains the following properties:

``version`` (``string``)
    Gets the version property of a service shape if the shape is
    a service.

    The following example matches all service shapes that have a version
    property that starts with ``2018-``:

    .. code-block:: none

        [service|version^='2018-']
``(length)``
    Returns ``1``, the number of attribute supported by the service
    property.


``trait`` attribute
-------------------

The ``trait`` attribute returns an object that contains every trait applied
to a shape. Each key of the ``trait`` object is the absolute shape ID of a
trait applied to the shape, and each value is the value of the applied trait.

The following example matches all shapes that have the
:ref:`deprecated-trait`:

.. code-block:: none

    [trait|smithy.api#deprecated]

Traits in the ``smithy.api`` namespace MAY be retrieved from the ``trait``
object without a namespace.

.. code-block:: none

    [trait|deprecated]

Traits are converted to their serialized :token:`node <node_value>` form
when matching against their values. Only string, Boolean, and numeric
values can be compared with an expected value. Boolean values are converted
to "true" or "false". Numeric values are converted to their string
representation.

The following selector matches all shapes with the :ref:`error-trait` set to
``client``:

.. code-block:: none

    [trait|error=client]

The following selector matches all shapes with the :ref:`error-trait`, but
the trait is not set to ``client``:

.. code-block:: none

    [trait|error!=client]

The following selector matches all shapes with the :ref:`documentation-trait`
that have a value that contains "TODO" or "FIXME":

.. code-block:: none

    [trait|documentation *= TODO, FIXME]

.. note::

    When converted to a string, the ``trait`` attribute returns an
    empty string.

The ``(length)`` attribute function returns the number of traits applied to a
shape.

The following example matches all shapes with more than 10 traits applied to it:

.. code-block:: none

    [trait|(length) > 10]


Nested attribute properties
---------------------------

Nested properties of an object attribute can be selected using subsequent
pipe (``|``) delimited property names.

The following example matches all shapes that have a :ref:`range-trait`
with a ``min`` property set to ``1``:

.. code-block:: none

    [trait|range|min=1]


.. _attribute-function-properties:

Attribute function properties
-----------------------------

:token:`Attribute function properties <selector_function_property>` are used
to create :ref:`projections <attribute-projections>` and apply other
functions on attributes. Attributes support the following functions:

``(keys)``
    Creates a :ref:`keys-projection` on objects.
``(values)``
    Creates a :ref:`values-projection` on arrays and objects.
``(length)``
    Returns the number of elements in an array, the number of entries in an
    object, or the number of characters in a string.

    The following example matches shapes where the name of the shape is
    longer than 20 characters:

    .. code-block:: none

        [id|name|(length) > 20]

    The following example matches shapes where the :ref:`externalDocumentation-trait`
    has more than 10 entries:

    .. code-block:: none

        [trait|externalDocumentation|(length) > 10]

    The following example checks if any ``enum`` trait definition contains
    more than 100 tags:

    .. code-block:: none

        [trait|enum|(values)|tags|(length) > 100]

    The following example checks if any ``enum`` trait definition contains
    a tag that is longer than 20 characters:

    .. code-block:: none

        [trait|enum|(values)|tags|(values)|(length) > 20]

.. note::

    Attribute functions are not actual properties of an attribute. They are
    never yielded as part of the result of a ``(values)`` or ``(keys)``
    projection.


.. _attribute-projections:

Attribute projections
---------------------

*Attribute projections* are values that perform set intersections with other
values. A projection is formed using either the ``(values)`` or ``(keys)``
:token:`function-property <selector_function_property>`.


.. _values-projection:

``(values)`` projection
~~~~~~~~~~~~~~~~~~~~~~~

The ``(values)`` property creates a *projection* of all values contained
in a :token:`list <node_array>` or :token:`object <node_object>`. Each
element from the value currently being evaluated is used as a new value
to check subsequent properties against. A ``(values)`` projection on any
value other than an array or object yields no result.

The following example matches all shapes that have an :ref:`enum-trait`
that contains an enum definition with a ``tags`` property that is set to
``internal``:

.. code-block:: none

    [trait|enum|(values)|tags|(values)=internal]

The following example matches all shapes that have an :ref:`externalDocumentation-trait`
that has a value set to ``https://example.com``:

.. code-block:: none

    [trait|externalDocumentation|(values)='https://example.com']

The following selector matches every trait applied to a shape that is a string
that contains a '$' character:

.. code-block:: none

    [trait|(values)*='$']


.. _keys-projection:

``(keys)`` projection
~~~~~~~~~~~~~~~~~~~~~

The ``(keys)`` property creates a *projection* of all keys of an
:token:`object <node_object>`. Each key of the object currently being
evaluated is used as a new value to check subsequent properties against.
A ``(keys)`` projection on any value other than an object yields no
result.

The following example matches all shapes that have an ``externalDocumentation``
trait that has an entry named ``Homepage``:

.. code-block:: none

    [trait|externalDocumentation|(keys)=Homepage]

The following selector matches shapes that apply any traits in the
``smithy.example`` namespace:

.. code-block:: none

    [trait|(keys)^='smithy.example#']


Projection comparisons
~~~~~~~~~~~~~~~~~~~~~~

When a projection is compared against a scalar value, the comparison matches
if any value in the projection satisfies the comparator assertion against the
scalar value. When a projection is compared against another projection, the
comparison matches if any value in the left projection satisfies the
comparator when compared against any value in the right projection.


Path traversal error handling
-----------------------------

Implementations MUST tolerate expressions that do not perform a valid
traversal of an attribute. The following example attempts to descend into
non-existent properties of the :ref:`documentation-trait`. This example
MUST not cause an error and MUST match no shapes:

.. code-block:: none

    [trait|documentation|invalid|child=Hi]


Scoped attribute selectors
==========================

A :token:`scoped attribute selector <selector_scoped_attr>` is similar to an
attribute selector, but it allows multiple complex comparisons to be made
against a scoped attribute.


Context values
--------------

The first part of a scoped attribute selector is the attribute that is scoped
for the expression, followed by ``:``. The scoped attribute is accessed using
a :token:`context value <selector_context_value>` in the form of
``@{`` :token:`identifier` ``}``.

In the following example, the ``trait|range`` attribute is used as the scoped
attribute of the expression, and the selector matches all shapes marked with
the :ref:`range-trait` where the ``min`` value is greater than the ``max``
value:

.. code-block:: none

    [@trait|range: @{min} > @{max}]

The ``(values)`` and ``(keys)`` projections MAY be used as the scoped
attribute context value. When the scoped attribute context value is a
projection, each flattened value of the projection is individually tested
against each assertion. If any value from the projection matches the
assertions, then the selector matches the shape.

The following selector matches shapes that have an :ref:`enum-trait` where one
or more of the enum definitions is both marked as ``deprecated`` and contains
an entry in its ``tags`` property named ``deprecated``.

.. code-block:: none

    [@trait|enum|(values):
        @{deprecated}=true &&
        @{tags|(values)}="deprecated"]


And-logic
---------

Selector assertions can be combined together using *and* statements with ``&&``.

The following selector matches all shapes with the :ref:`idRef-trait` that
set ``failWhenMissing`` to true and omit an ``errorMessage``:

.. code-block:: none

    [@trait|idRef: @{failWhenMissing}=true && @{errorMessage}?=false]


Matching multiple values
------------------------

Like non-scoped selectors, multiple values can be provided using a comma
separated list. One or more resolved attribute values MUST match one or more
provided values.

The following selector matches all shapes with the :ref:`httpApiKeyAuth-trait`
where the ``in`` property is ``header`` and the ``name`` property is neither
``x-api-token`` or ``authorization``:

.. code-block:: none

    [@trait|httpApiKeyAuth:
        @{name}=header &&
        @{in}!='x-api-token', 'authorization']


Case insensitive comparisons
----------------------------

The ``i`` token used before ``&&`` or the closing ``]`` makes a comparison
case-insensitive.

The following selector matches on the ``httpApiKeyAuth`` trait using
case-insensitive comparisons:

.. code-block:: none

    [@trait|httpApiKeyAuth:
        @{name}=header i &&
        @{in}!='x-api-token', 'authorization' i]

The following selector matches on the ``httpApiKeyAuth`` trait but only
uses a case-insensitive comparison on ``in``:

.. code-block:: none

    [@trait|httpApiKeyAuth:
        @{name}=header &&
        @{in}!='x-api-token', 'authorization' i]


Neighbors
=========

The *current shapes* evaluated by a selector are changed using a
:token:`selector_neighbor` token.


Undirected neighbor
-------------------

An :token:`undirected neighbor <selector_undirected_neighbor>` (``>``) changes
the current set of shapes to every shape that is connected to the current
shapes. For example, the following selector returns the key and value
members of every map:

.. code-block:: none

    map > member

Selectors can return just the key members or just the value members by adding
an attribute selector on the ``id|member``:

.. code-block:: none

    map > member[id|member=key]

Neighbors can be chained to traverse further into a shape. The following
selector returns strings that are targeted by list members:

.. code-block:: none

    list > member > string


Directed neighbors
------------------

The ``>`` neighbor selector is an *undirected* edge traversal. Sometimes a
directed edge traversal is necessary to match the appropriate shapes. For
example, the following selector returns the "bound", "input", "output",
and "errors" relationships of each operation:

.. code-block:: none

    operation > *

A directed edge traversal can be performed using the ``-[`` token followed
by a comma separated list of :ref:`relationships <selector-relationships>`,
followed by ``]->``. The following selector matches all structure
shapes referenced as operation input or output.

.. code-block:: none

    operation -[input, output]->

The ``:test`` function can be used to check if a shape has a named
relationship. The following selector matches all resource shapes that define
an identifier:

.. code-block:: none

    resource:test(-[identifier]->)

Relationships from a shape to the traits applied to the shape can be traversed
using a directed relationship named ``trait``. It is atypical to traverse
``trait`` relationships, therefore they are only yielded by selectors when
explicitly requested using a ``trait`` directed relationship. The following
selector finds all service shapes that have a protocol trait applied to it
(that is, a trait that is marked with the :ref:`protocolDefinition-trait`):

.. code-block:: none

    service:test(-[trait]-> [trait|protocolDefinition])


Recursive neighbors
-------------------

The ``~>`` neighbor selector finds all shapes that are recursively connected in
the closure of another shape.

The following selector finds all operations that are connected to a service
shape:

.. code-block:: none

    service ~> operation

The following selector finds all operations that do not have the :ref:`http-trait`
that are in the closure of a service marked with the ``aws.protocols#restJson``
trait:

.. code-block:: none

    service[trait|aws.protocols#restJson1] ~> operation:not([trait|http])


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
    * - operation
      - output
      - The output structure of the operation (if present).
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
    evaluated, the directed traversal of unknown relationship types matches
    no shapes.


Functions
=========

Functions are used to filter shapes. Functions always start with ``:``.

.. important::

    Implementations MUST tolerate parsing unknown function names. When
    evaluated, the unknown function matches no shapes.


``:test``
---------

The ``:test`` function is used to test if a shape is contained within any of
the provided predicate selector return values without changing the current
shape.

The following selector is used to match all list shapes that target a string:

.. code-block:: none

    list:test(> member > string)

The following example matches all shapes that are bound to a resource and have
no documentation:

.. code-block:: none

    :test(-[bound, resource]->) :not([trait|documentation])


``:is``
-------

The ``:is`` function is used to map over the current shapes with multiple
selectors and returns all of the shapes returned from each selector. The
``:is`` function accepts a variadic list of selectors each separated by a
comma (",").

The following selector matches all string and number shapes:

.. code-block:: none

    :is(string, number)

Each can be used inside of neighbors too. The following selector
matches all members that target a string or number:

.. code-block:: none

    member > :is(string, number)

The following ``:is`` selector matches all shapes that are either
targeted by a list member or targeted by a map member:

.. code-block:: none

    :is(list > member > *, map > member > *)

The following selector matches all list and map shapes that target strings:

.. code-block:: none

    :is(:test(list > member > string), :test(map > member > string))

Because none of the selectors in the ``:is`` function are intended to
change the current node, this can be reduced to the following selector:

.. code-block:: none

    :test(:is(list > member > string, map > member > string))

.. note::

    This function was previously named ``:each``. Implementations that wish
    to maintain backward compatibility with the old function name MAY
    treat ``:each`` as an alias for ``:is``, and models that use ``:each``
    SHOULD update to use ``:is``.


``:not``
--------

The *:not* function is used to filter out shapes. This function accepts a
list of selector arguments, and the shapes returned from each predicate are
filtered out from the result set.

The following selector matches every shape except strings:

.. code-block:: none

    :not(string)

The following selector matches every shape except strings and floats:

.. code-block:: none

    :not(string, float)

The following example matches all shapes except for strings that are targeted
by a list member:

.. code-block:: none

    :not(list > member > string)

.. important::

    The shapes *returned* from the predicate selectors are filtered out.

The ``:test`` function can be used to test a shape, potentially traversing its
neighbors, without changing the return value of the test. The following
example does not match any list shape that has a string member:

.. code-block:: none

    :not(:test(list > member > string))

Successive ``:not`` functions can be used to filter shapes using several
predicates. The following example does not match strings or shapes with the
:ref:`sensitive-trait` trait:

.. code-block:: none

    :not(string):not([trait|sensitive])

Multiple selectors can be provided to ``:not`` to find shapes that do not
match all of the provided predicates. The following selector finds all
string shapes that do not have both the ``length`` and ``pattern``
traits:

.. code-block:: none

    string:not([trait|length], [trait|pattern])

The following example matches all structure members that target strings in
which the member does not have the ``length`` trait and the shape targeted by
the member does not have the ``length`` trait:

.. code-block:: none

    structure > member
        :test(> string:not([trait|length]))
        :test(:not([trait|length]))

The following selector finds all service shapes that do not have a
protocol trait applied to it:

.. code-block:: none

    service:not(:test(-[trait]-> [trait|protocolDefinition]))

The following selector finds all traits that are not attached to any shape
in the model:

.. code-block:: none

    :not(* -[trait]-> *)[trait|trait]


``:of``
-------

The ``:of`` function is used to match members based on their containers
(i.e., the shape that defines the member). The ``:of`` function accepts one
or more selector arguments. Each selector receives the containing shape
of the member, and if any of the selectors return returns 1 or more shapes,
the member is matched.

The following example matches all structure members:

.. code-block:: none

    member:of(structure)

The following example matches all structure and list members:

.. code-block:: none

    member:of(structure, list)


Grammar
=======

Selectors are defined by the following ABNF_ grammar.

.. admonition:: Lexical note
   :class: note

   Whitespace is insignificant and can occur between any token without
   changing the semantics of a selector.

.. productionlist:: selectors
    selector                        :`selector_expression` *(`selector_expression`)
    selector_expression             :`selector_shape_types`
                                    :/ `selector_attr`
                                    :/ `selector_scoped_attr`
                                    :/ `selector_function_expression`
                                    :/ `selector_neighbor`
    selector_shape_types            :"*" / `identifier`
    selector_neighbor               :`selector_undirected_neighbor`
                                    :/ `selector_directed_neighbor`
                                    :/ `selector_recursive_neighbor`
    selector_undirected_neighbor    :">"
    selector_directed_neighbor      :"-[" `selector_rel_type` *("," `selector_rel_type`) "]->"
    selector_recursive_neighbor     :"~>"
    selector_rel_type               :`identifier`
    selector_attr                   :"[" `selector_key` *(`selector_comparator` `selector_values` ["i"]) "]"
    selector_key                    :`identifier` ["|" `selector_path`]
    selector_path                   :`selector_path_segment` *("|" `selector_path_segment`)
    selector_path_segment           :`selector_value` / `selector_function_property`
    selector_value                  :`selector_text` / `number` / `root_shape_id`
    selector_function_property      :"(" `identifier` ")"
    selector_values                 :`selector_value` *("," `selector_value`)
    selector_comparator             :"^=" / "$=" / "*=" / "!=" / ">=" / ">" / "<=" / "<" / "?=" / "="
    selector_absolute_root_shape_id :`namespace` "#" `identifier`
    selector_scoped_attr            :"[@" `selector_key` ":" `selector_scoped_assertions` "]"
    selector_scoped_assertions      :`selector_scoped_assertion` *("&&" `selector_scoped_assertion`)
    selector_scoped_assertion       :`selector_scoped_value` `selector_comparator` `selector_scoped_values` ["i"]
    selector_scoped_value           :`selector_value` / `selector_context_value`
    selector_context_value          :"@{" `selector_path` "}"
    selector_scoped_values          :`selector_scoped_value` *("," `selector_scoped_value`)
    selector_function_expression    :":" `selector_function` "(" `selector` *("," `selector`) ")"
    selector_function               :`identifier`
    selector_text                   :`selector_single_quoted_text` / `selector_double_quoted_text`
    selector_single_quoted_text     :"'" 1*`selector_single_quoted_char` "'"
    selector_double_quoted_text     :DQUOTE 1*`selector_double_quoted_char` DQUOTE
    selector_single_quoted_char     :%x20-26 / %x28-5B / %x5D-10FFFF ; Excludes (')
    selector_double_quoted_char     :%x20-21 / %x23-5B / %x5D-10FFFF ; Excludes (")

.. _ABNF: https://tools.ietf.org/html/rfc5234
