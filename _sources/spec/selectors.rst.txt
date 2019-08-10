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

::

    string


Attribute selectors
===================

*Attribute selectors* are used to match shapes based on the
:ref:`shape ID <shape-id>`, :ref:`traits <traits>`, and member target.
Attribute selectors take one of two forms: existence of an attribute and
comparison of an attribute value to an expected value.

Attribute selectors support the following comparators:

.. list-table::
    :header-rows: 1
    :widths: 10 90

    * - Comparator
      - Description
    * - ``=``
      - Matches if the attribute value is equal to the expected value.
    * - ``^=``
      - Matches if the attribute value starts with the expected value.
    * - ``$=``
      - Matches if the attribute value ends with the expected value.
    * - ``*=``
      - Matches if the attribute value contains with the expected value.

Attribute comparisions can be made case-insensitive by preceding the closing
bracket with " i" (e.g., ``string[trait|time=DATE i]``).


Matching traits
~~~~~~~~~~~~~~~

We can match shapes based on traits using an *attribute selector*. The
following selector finds all structure shapes with the :ref:`error-trait`
trait:

::

    structure[trait|error]

The ``trait|`` is called a *namespace prefix*. This particular prefix tells
the selector that we are interested in a trait applied to the current shape,
and that that specific trait is ``time``.

We can match string shapes that have a specific trait value:

::

    structure[trait|error=client]

Matching on trait values only works for traits that have a scalar value
(e.g., strings, numbers, and booleans). We can also match case-insensitvely
on the value by appending " i" before the closing bracket:

::

    structure[trait|error=CLIENT i]

Fully-qualified trait names are also supported:

::

    string[trait|smithy.example#customTrait=foo]


Matching on shape ID
~~~~~~~~~~~~~~~~~~~~

Attribute selectors can be used to match the :ref:`shape ID <shape-id>`. The
following example matches a single resource shape with an ID of
``smithy.example#Foo``:

::

    resource[id='smithy.example#Foo']

Notice that the value of an attribute selector can be quoted. The example
above uses single quotes, but double quotes work too.

Smithy provides several attributes in the ``id`` namespace to make matching
on a shape ID easier. The following example finds all shapes that are in the
"smithy.example" namespace:

::

    resource[id|namespace=smithy.example]

Though not as clear, matching shapes in a specific namespace can also be
acheived using the ``^=`` comparator against ``id``:

::

    resource[id^=smithy.example#]

The following example matches all member shapes that have a member name of
"key":

::

    resource[id|member=key]

Though not as clear, matching members with a member name of "key" can also be
acheived using the ``$=`` comparator against ``id``:

::

    resource[id$="$key"]


Available attributes
~~~~~~~~~~~~~~~~~~~~

.. list-table::
    :header-rows: 1
    :widths: 10 50 40

    * - Attribute
      - Description
      - Example result
    * - ``id``
      - The full shape ID of a shape
      - ``foo.baz#Structure$memberName``
    * - ``id|namespace``
      - The namespace part of a shape ID
      - ``foo.baz``
    * - ``id|name``
      - The name part of a shape ID
      - ``Structure``
    * - ``id|member``
      - The member part of a shape ID (if available)
      - ``memberName``
    * - ``service|version``
      - Gets the version property of a service shape if the shape is
        a service.
      - ``service[service|version^='2018-']``
    * - ``trait|*``
      - Gets the value of a trait applied to a shape, where "*" is the name
        of a trait (e.g., ``trait|error``). Boolean trait values are
        converted to "true" or "false".
      - ``client``


Neighbors
=========

The *current* shape evaluated by a selector is changed using a neighbor token,
``>``. A neighbor token returns every shape that is connected to the current
shape. For example, the following selector returns the key and value members of
every map:

::

    map > member

We can return just the key members or just the value members by adding an
attribute selector on the ``id|member``:

::

    map > member[id|member=key]

Neighbors can be chained to traverse further into a shape. The following
selector returns strings that are targeted by list members:

::

    list > member > string


Directed neighbors
~~~~~~~~~~~~~~~~~~

The ``>`` neighbor selector is an *undirected* edge traversal. Sometimes a
directed edge traversal is necessary to match the appropriate shapes. For
example, the following selector returns the "bound", "input", "output",
and "errors" relationships of each operation:

::

    operation > *

A directed edge traversal can be performed using the ``-[`` token followed
by a comma separated list of :ref:`relationships <selector-relationships>`,
followed by ``]->``. The following selector matches all structure
shapes referenced as operation input or output.

::

    operation -[input, output]->

The ``:test`` function can be used to check if a shape has a named
relationship. The following selector matches all resource shapes that define
an identifier:

::

    resource:test(-[identifier]->)


.. _selector-relationships:

Relationships
~~~~~~~~~~~~~

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
      - "operations", "put", "read", "update", and "delete" properties.
    * - resource
      - collectionOperation
      - Each operation that is bound to a resource through the
      - "collectionOperations", "create", and "list" properties.
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


Functions
=========

Functions are used to filter shapes. Functions always start with ``:``.


:each
~~~~~

The ``:each`` function is used to map over the current shape with multiple
selectors and returns all of the shapes returned from each selector. The
``:each`` function accepts a variadic list of selectors each separated by a
comma (",").

The following selector matches all string and number shapes:

::

    :each(string, number)

Each can be used inside of neighbors too. The following selector
matches all members that target a string or number:

::

    member > :each(string, number)

The following ``:each`` selector matches all shapes that are either
targeted by a list member or targeted by a map member:

::

    :each(list > member > *, map > member > *)

The following selector matches all list and map shapes that target strings:

::

    :each(:test(list > member > string), :test(map > member > string))

Because none of the selectors in the ``:each`` function are intended to
change the current node, this can be reduced to the following selector:

::

    :test(:each(list > member > string, map > member > string))


:test
~~~~~

The ``:test`` function is used to test if a shape is contained within any of
the provided predicate selector return values without changing the current
shape.

The following selector is used to match all string and number shapes:

::

    :test(string, number)

The ``:test`` function is much more interesting when used to test if a shape
contains a neighbor in addition to other filtering. The following example
matches all shapes that are bound to a resource and have no documentation:

::

    :test(-[bound, resource]->) :not([trait|documentation])


:not
~~~~

The *:not* function is used to filter out shapes. This function accepts a
list of selector arguments, and the shapes returned from each predicate are
filtered out from the result set.

The following selector matches every shape except strings:

::

    :not(string)

The following selector matches every shape except strings and floats:

::

    :not(string, float)

The following example matches all shapes except for strings that are targeted
by a list member:

::

    :not(list > member > string)

.. important::

    The shapes *returned* from the predicate selectors are filtered out.

The ``:test`` function can be used to test a shape, potentially traversing its
neighbors, without changing the return value of the test. The following
example does not match any list shape that has a string member:

::

    :not(:test(list > member > string))

Successive ``:not`` functions can be used to filter shapes using several
predicates. The following example does not match strings or shapes with the
:ref:`sensitive-trait` trait:

::

    :not(string):not([trait|sensitive])

Multiple selectors can be provided to ``:not`` to find shapes that do not
match all of the provided predicates. The following selector finds all
string shapes that do not have both the ``length`` and ``pattern``
traits:

::

    string:not([trait|length], [trait|pattern])

The following example matches all structure members that target strings in
which the member does not have the ``length`` trait and the shape targeted by
the member does not have the ``length`` trait:

::

    structure > member
        :test(> string:not([trait|length]))
        :test(:not([trait|length]))


:of
~~~

The ``:of`` function is used to match members based on their containers
(i.e., the shape that defines the member). The ``:of`` function accepts one
or more selector arguments. Each selector receives the containing shape
of the member, and if any of the selectors return returns 1 or more shapes,
the member is matched.

The following example matches all structure members:

::

    member:of(structure)

The following example matches all structure and list members:

::

    member:of(structure, list)


Grammar
=======

Selectors are defined by the following ABNF_ grammar.

.. admonition:: Lexical note
   :class: note

   Whitespace is insignificant and can occur between any token without
   changing the semantics of a selector.

.. productionlist:: selectors
    selector             :`selector_expression` *(`selector_expression`)
    selector_expression  :`shape_types` / `attr` / `function_expression` / `neighbors`
    shape_types          :"*"
                         :/ "blob"
                         :/ "boolean"
                         :/ "document"
                         :/ "string"
                         :/ "byte"
                         :/ "short"
                         :/ "integer"
                         :/ "long"
                         :/ "float"
                         :/ "double"
                         :/ "bigDecimal"
                         :/ "bigInteger"
                         :/ "timestamp"
                         :/ "list"
                         :/ "map"
                         :/ "set"
                         :/ "structure"
                         :/ "union"
                         :/ "service"
                         :/ "operation"
                         :/ "resource"
                         :/ "member"
                         :/ "number"
                         :/ "simpleType"
                         :/ "collection"
    neighbors            :">" / `directed_neighbor`
    directed_neighbor    :"-[" `relationship_type` *("," `relationship_type`) "]->"
    relationship_type    :"identifier"
                         :/ "create"
                         :/ "read"
                         :/ "update"
                         :/ "delete"
                         :/ "list"
                         :/ "member"
                         :/ "input"
                         :/ "output"
                         :/ "error"
                         :/ "operation"
                         :/ "collectionOperation"
                         :/ "instanceOperation"
                         :/ "resource"
                         :/ "bound"
    attr                   :"[" `attr_key` *(`comparator` `attr_value` ["i"]) "]"
    attr_key               :`id_attribute` / `trait_attribute` / `service_attribute`
    id_attribute           :"id" ["|" ("namespace" / "name" / "member")]
    trait_attribute        :"trait" "|" `attr_value` *("|" `attr_value`)
    attr_value             :`attr_identifier` / `selector_text`
    attr_identifier        :1*(ALPHA / DIGIT / "_") *(ALPHA / DIGIT / "_" / "-" / "." / "#")
    service_attribute      :"service|version"
    comparator            :"^=" / "$=" / "*=" / "="
    function_expression   :":" `function` "(" `selector` *("," `selector`) ")"
    function              :"each" / "test" / "of" / "not"
    selector_text         :`selector_single_quoted_text` / `selector_double_quoted_text`
    selector_single_quoted_text    :"'" 1*`selector_single_quoted_char` "'"
    selector_double_quoted_text    :DQUOTE 1*`selector_double_quoted_char` DQUOTE
    selector_single_quoted_char    :%x20-26 / %x28-5B / %x5D-10FFFF ; Excludes (')
    selector_double_quoted_char    :%x20-21 / %x23-5B / %x5D-10FFFF ; Excludes (")

.. _ABNF: https://tools.ietf.org/html/rfc5234
