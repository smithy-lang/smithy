.. _shape-id:

========
Shape ID
========

A :dfn:`shape ID` is used to refer to shapes and traits in the model.

.. contents:: Table of contents
    :depth: 1
    :local:
    :backlinks: none


---------------
Shape ID Syntax
---------------

Shape IDs adhere to the following syntax:

.. code-block:: none

    com.foo.baz#ShapeName$memberName
    \_________/ \_______/ \________/
         |          |          |
     Namespace  Shape name  Member name

Absolute shape ID
    An :dfn:`absolute shape ID` starts with a :token:`namespace` name,
    followed by "``#``", followed by a *relative shape ID*.
Relative shape ID
    A :dfn:`relative shape ID` contains a :token:`shape name <identifier>`
    and an optional :token:`member name <identifier>`. The shape name and
    member name are separated by the "``$``" symbol if a member name is
    present.

    A relative shape ID is resolved to an absolute shape ID using the
    process defined in :ref:`relative-shape-id`.


.. _shape-id-abnf:

-------------
Shape ID ABNF
-------------

Shape IDs are formally defined by the following ABNF:

.. productionlist:: smithy
    identifier             :(ALPHA / "_") *(ALPHA / DIGIT / "_")
    namespace              :`identifier` *("." `identifier`)
    shape_id               :`absolute_shape_id` / `relative_shape_id`
    absolute_shape_id      :`namespace` "#" `relative_shape_id`
    relative_shape_id      :`identifier` ["$" `identifier`]
    LOALPHA                :%x61-7A ; a-z

.. admonition:: Lexical note
   :class: important

   Whitespace is **significant** in shape IDs.


.. _relative-shape-id:

----------------------------
Relative shape ID resolution
----------------------------

In the Smithy IDL, relative shape IDs are resolved using the following process:

#. If a :token:`use_statement` has imported a shape with the same name,
   the shape ID resolves to the imported shape ID.
#. If a shape is defined in the same namespace as the shape with the same name,
   the namespace of the shape resolves to the *current namespace*.
#. If a shape is defined in the :ref:`prelude <prelude>` with the same name,
   the namespace resolves to ``smithy.api``.
#. If a relative shape ID does not satisfy one of the above cases, the shape
   ID is invalid, and the namespace is inherited from the *current namespace*.

The following example Smithy model contains comments above each member of
the shape named ``MyStructure`` that describes the shape the member resolves
to.

.. code-block:: smithy

    namespace smithy.example

    use foo.baz#Bar

    string MyString

    structure MyStructure {
        // Resolves to smithy.example#MyString
        // There is a shape named MyString defined in the same namespace.
        a: MyString,

        // Resolves to smithy.example#MyString
        // Absolute shape IDs do not perform namespace resolution.
        b: smithy.example#MyString,

        // Resolves to foo.baz#Bar
        // The "use foo.baz#Bar" statement imported the Bar symbol,
        // allowing the shape to be referenced using a relative shape ID.
        c: Bar,

        // Resolves to foo.baz#Bar
        // Absolute shape IDs do not perform namespace resolution.
        d: foo.baz#Bar,

        // Resolves to foo.baz#MyString
        // Absolute shape IDs do not perform namespace resolution.
        e: foo.baz#MyString,

        // Resolves to smithy.api#String
        // No shape named String was imported through a use statement
        // the smithy.example namespace does not contain a shape named
        // String, and the prelude model contains a shape named String.
        f: String,

        // Resolves to smithy.example#MyBoolean.
        // There is a shape named MyBoolean defined in the same namespace.
        // Forward references are supported both within the same file and
        // across multiple files.
        g: MyBoolean,

        // Invalid. A shape by this name has not been imported through a
        // use statement, a shape by this name does not exist in the
        // current namespace, and a shape by this name does not exist in
        // the prelude model.
        h: InvalidShape,
    }

    boolean MyBoolean

.. _relative-shape-id-json:

Relative shape IDs in the :ref:`JSON AST <json-ast>` are resolved using
the same process as the IDL with the only difference being the JSON AST
does not support any kind of ``use`` statements.

For example, given the following Smithy model:

.. code-block:: json

    {
        "smithy": "1.0.0",
        "shapes": {
            "smithy.example#MyStructure": {
                "type": "structure",
                "members": {
                    "a": {
                        "target": "smithy.example#MyString"
                    },
                    "b": {
                        "target": "smithy.api#String"
                    },
                    "c": {
                        "target": "smithy.example#Foo"
                    },
                    "d": {
                        "target": "smithy.example#InvalidShape"
                    }
                }
            },
            "smithy.example#MyString": {
                "type": "string"
            }
        }
    }

The members of ``MyStructure`` resolve to the following shape IDs:

- ``a`` targeting ``MyString`` resolves to ``smithy.example#MyString``.
- ``b`` targeting ``String`` resolves to ``smithy.api#String`` in the prelude.
- ``c`` targeting ``smithy.example#Foo`` resolves to ``smithy.example#Foo``
  because absolute shape IDs do not perform namespace resolution.
- ``d`` targeting ``InvalidShape`` resolves to an invalid shape ID that
  targets ``smithy.example#InvalidShape`` because a shape named
  ``InvalidShape`` does not exist in the ``smithy.example`` namespace nor
  does one exist in the prelude.


.. _shape-id-member-names:

---------------------
Shape ID member names
---------------------

A :ref:`member` of an :ref:`aggregate shape <aggregate-types>` can be
referenced in a shape ID by appending "``$``" followed by the
appropriate member name. Member names for each shape are defined as follows:

.. list-table::
    :header-rows: 1
    :widths: 25 40 35

    * - Shape ID
      - Syntax
      - Examples
    * - :ref:`structure` member
      - ``<name>$<member-name>``
      - ``Shape$foo``, ``ns.example#Shape$baz``
    * - :ref:`union` member
      - ``<name>$<member-name>``
      - ``Shape$foo``, ``ns.example#Shape$baz``
    * - :ref:`list` member
      - ``<name>$member``
      - ``Shape$member``, ``ns.example#Shape$member``
    * - :ref:`set` member
      - ``<name>$member``
      - ``Shape$member``, ``ns.example#Shape$member``
    * - :ref:`map` key
      - ``<name>$key``
      - ``Shape$key``, ``ns.example#Shape$key``
    * - :ref:`map` value
      - ``<name>$value``
      - ``Shape$value``, ``ns.example#Shape$value``


.. _shape-names:

-----------
Shape names
-----------

Consumers of a Smithy model MAY choose to inflect shape names, structure
member names, and other facets of a Smithy model in order to expose a more
idiomatic experience to particular programming languages. In order to make this
easier for consumers of a model, model authors SHOULD utilize a strict form of
PascalCase in which only the first letter of acronyms, abbreviations, and
initialisms are capitalized.

===========   ===============
Recommended   Not recommended
===========   ===============
UserId        UserID
ResourceArn   ResourceARN
IoChannel     IOChannel
HtmlEntity    HTMLEntity
HtmlEntity    HTML_Entity
===========   ===============


------------------
Shape ID conflicts
------------------

While shape IDs used within a model are case-sensitive, no two shapes in
the model can have the same case-insensitive shape ID. For example,
``com.Foo#baz`` and ``com.foo#baz`` are not allowed in the same model. This
property also extends to member names: ``com.foo#Baz$bar`` and
``com.foo#Baz$Bar`` are not allowed on the same structure.
