.. _smithy-language-specification:

=============================
Smithy Language Specification
=============================

This document defines the ABNF_ grammar and syntax for defining models using
the *Smithy interface definition language* (IDL) and the JSON abstract syntax
tree (AST) that can be used to represent a model.

Smithy models MUST be encoded using UTF-8 and SHOULD use Unix style
line endings.

.. contents:: Table of contents
    :depth: 2
    :local:
    :backlinks: none


.. _smithy-idl-abnf:

Smithy IDL ABNF
===============

The Smithy IDL is a series of statements separated by newlines.

.. productionlist:: smithy
    idl                     :[`statement` *(1*`br` `statement`)]
    statement               :`version_statement`
                            :/ `metadata_statement`
                            :/ `namespace_statement`
                            :/ `apply_statement`
                            :/ `trait_statement`
                            :/ `shape_statement`


Lexical notes
-------------

Whitespace is insignificant except for the following cases:

* :token:`br` production which indicates that a new line MUST occur
* :ref:`shape ID ABNF productions <shape-id-abnf>`

.. productionlist:: smithy
    br                  :%x0A / %x0D.0A

.. _comments:

Comments
~~~~~~~~

Comments can occur at any place in the IDL between tokens where whitespace
is insignificant. Comments in Smithy are defined using two forward slashes
followed by any character. A newline terminates a comment.

.. productionlist:: smithy
    line_comment        :"//" *(`not_newline`)
    not_newline         :%x09 / %x20-10FFFF ; Any character but new line

Example:

::

    // This is a comment
    namespace com.foo // This is also a comment

    // Another comment
    string MyString


.. _version-statement:

Version statement
-----------------

The version statement is used to set the version of the Smithy model. Multiple
version statements MAY appear in a Smithy model if and only if all of the
version statements define the same version. A version of `1.0` is assumed if
no version is specified.

.. productionlist:: smithy
    version_statement       :"$version:" (`text` / `number`)

Example:

::

    $version:1.0


.. _metadata-statement:

Metadata statement
------------------

The metadata statement is used to attach arbitrary :ref:`metadata <metadata>`
to a model.

.. productionlist:: smithy
    metadata_statement:"metadata" `metadata_key` "=" `metadata_value`
    metadata_key:`text`
    metadata_value:`node_value`

Example:

::

    metadata example.string1 = "hello there"
    metadata example.string2 = 'hello there'
    metadata example.string3 = hello
    metadata example.string4 = hello.there
    metadata example.string5 = hello.there
    metadata example.bool1 = true
    metadata example.bool2 = false
    metadata example.number = 10
    metadata example.array = [10, true, "hello"]
    metadata example.object = {foo: baz}
    metadata example.null = null

Top-level metadata key-value pair conflicts are resolved by
:ref:`merging metadata <merging-metadata>`


.. _namespace-statement:

Namespace statement
-------------------

The namespace statement is used to set the *current namespace*. Shapes
can only be defined if a current namespace is defined. Any number of namespace
statements can appear in a model.

.. productionlist:: smithy
    namespace_statement     :"namespace" `namespace`

Example:

::

    namespace com.foo.baz


Shape statements
----------------

Shape statements are used to define :ref:`shapes <shapes>`. Shapes can only
be defined after a *current namespace* has been defined using a
:ref:`namespace statement <namespace-statement>`.

.. productionlist:: smithy
    shape_statement         :[`inline_traits` `br`] `shape_body`
    shape_body              :`service_statement`
                            :/ `resource_statement`
                            :/ `operation_statement`
                            :/ `structure_statement`
                            :/ `union_statement`
                            :/ `list_statement`
                            :/ `set_statement`
                            :/ `map_statement`
                            :/ `simple_shape`
    service_statement       :"service" `identifier` `node_object`
    resource_statement      :"resource" `identifier` `node_object`
    operation_statement     :"operation" `identifier`
                            :"(" [`shape_id`] ")" `operation_results`
    operation_results       :["->" `shape_id`]
                            :["errors" "[" [`shape_id` *("," `shape_id`)] "]"]
    structure_statement     :"structure" `structured_body`
    union_statement         :"union" `structured_body`
    structured_body         :`identifier`
                            :"{" [`structured_member` *("," `structured_member`)] "}"
    structured_member       :`member_traits` `identifier` ":" `shape_id`
    list_statement          :"list" `list_and_set_body`
    set_statement           :"set" `list_and_set_body`
    list_and_set_body       :`identifier` "{" `member_traits` "member" ":" `shape_id` [","] "}"
    map_statement           :"map" `identifier` "{" `map_body` "}"
    map_body                :`map_member` "," `map_member` [","]
    map_member              :`member_traits` ("key" / "value") ":" `shape_id`
    simple_shape            :(   "blob"
                            :  / "boolean"
                            :  / "string"
                            :  / "byte"
                            :  / "short"
                            :  / "integer"
                            :  / "long"
                            :  / "float"
                            :  / "double"
                            :  / "bigInteger"
                            :  / "bigDecimal"
                            :  / "timestamp" ) `identifier`


Apply statement
---------------

The apply statement is used to attach a trait to a shape outside of a shape's
definition.

.. productionlist:: smithy
    apply_statement         :"apply" `shape_id` `trait`

The following example applies the :ref:`deprecated-trait` trait to a shape
named ``MyShape`` using a :ref:`relative shape id <relative-shape-id>`.

::

    apply MyShape @deprecated


.. _trait-statement:

Trait statement
---------------

The trait statement is used to define a trait inside of a namespace. Traits
can only be defined after a *current namespace* has been defined using a
:ref:`namespace statement <namespace-statement>`.

.. productionlist:: smithy
    trait_statement         :"trait" `identifier` `node_object`

The body of a trait statement is a :token:`node object <node_object>` that
supports the same key-value pairs defined in :ref:`trait-definition`.


Trait values
------------

Trait values are :ref:`traits <traits>` attached to :ref:`shapes <shapes>`.
Trait values can only appear immediately before a shape or
:ref:`member <member>` definition.

.. productionlist:: smithy
    inline_traits           :[`trait` *`trait`]
    trait                   :"@" `shape_id` ["(" `trait_body_value` ")"]
    trait_body_value        :`trait_structure` / `node_value`
    trait_structure         :`trait_structure_kvp` *("," `trait_structure_kvp`)
    trait_structure_kvp     :`text` ":" `node_value`
    member_traits           :[`inline_traits`]

The following example applies various traits to a structure shape and its
members.

::

    @documentation("An animal in the animal kingdom")
    structure Animal {
      @required
      name: smithy.api#String,

      @deprecated
      @deprecationReason("Use name instead")
      subject: smithy.api#String,

      @length(min: 0)
      age: smithy.api#Integer,
    }


String values
-------------

String values are utilized in various contexts. String values can be unquoted
if they adhere to the :token:`unquoted_text` production.

Smithy strings are considered *raw strings*, meaning they do not support any
form of escapes other than to escape a closing quote (using ``\"`` or ``\'``)
or to escape an escape (using ``\\``).

.. productionlist:: smithy
    text                :`unquoted_text` / `quoted_text` / `text_block`
    unquoted_text       :(ALPHA / "_") *(ALPHA / DIGIT / "-" / "_" / "$" / "." / "#")
    quoted_text         :`single_quoted_text` / `double_quoted_text`
    single_quoted_text  :"'" *`single_quoted_char` "'"
    single_quoted_char  :  %x20-26
                        :/ %x28-5B
                        :/ %x5D-10FFFF
                        :/ `escaped_char`
                        :/ `preserved_single`
    escaped_char        :`escape` (`escape` / "'" / DQUOTE / "b" / "f" / "n" / "r" / "t" / "/" / `unicode_escape`)
    unicode_escape      :"u" `hex` `hex` `hex` `hex`
    hex                 : DIGIT / %x41-46 / %x61-66
    preserved_single    :`escape` (%x20-26 / %x28-5B / %x5D-10FFFF)
    double_quoted_text  :DQUOTE *`double_quoted_char` DQUOTE
    double_quoted_char  :  %x20-21
                        :/ %x23-5B
                        :/ %x5D-10FFFF
                        :/ `escaped_char`
                        :/ `preserved_double`
    preserved_double    :`escape` (%x20-21 / %x23-5B / %x5D-10FFFF)
    escape              :%x5C ; backslash
    text_block          :DQUOTE DQUOTE DQUOTE `br` `double_quoted_char` DQUOTE DQUOTE DQUOTE

New lines in strings are normalized from CR (\u000D) and CRLF (\u000D\u000A)
to LF (\u000A). This ensures that strings defined in a Smithy model are
equivalent across platforms. If a literal ``\r`` is desired, it can be added
a string value using the Unicode escape ``\u000d``.


.. _text-blocks:

Text blocks
~~~~~~~~~~~

A text block is a string literal that can span multiple lines and
automatically removes any incidental whitespace. A text block is opened with
three double quotes ("""), followed by a newline, zero or more content
characters, and closed with three double quotes.

*Smithy text blocks are heavily based on text blocks defined in* `JEP 355 <https://openjdk.java.net/jeps/355>`_

Text blocks differentiate *incidental whitespace* from
*significant whitespace*. Smithy will re-indent the content of a text block by
removing all incidental whitespace.

::

    @documentation("""
        <div>
            <p>Hello!</p>
        </div>
        """)

The four leading spaces in the above text block are considered insignificant
because they are common across all lines. Because the closing delimiter
appears on its own line, a trailing new line is added to the result. The
content of the text block is re-indented to remove the insignificant
whitespace, making it equivalent to the following:

::

    @documentation("<div>\n    <p>Hello!</p>\n</div>\n")

The closing delimiter can be placed on the same line as content if no new line
is desired at the end of the result. The above example could be rewritten to
not including a trailing new line:

::

    @documentation("""
        <div>
            <p>Hello!</p>
        </div>""")

This example is equivalent to the following:

::

    @documentation("<div>\n    <p>Hello!</p>\n</div>")

The following text blocks are ill-formed:

.. code-block:: none

    """foo"""  // missing new line following open delimiter
    """ """    // missing new line following open delimiter
    """
    "          // missing closing delimiter


.. _incidental-whitespace:

Incidental white space removal
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Smithy will re-indent the content of a text block by removing all
incidental whitespace using the following algorithm:

1. Split the content of the text block at every LF, producing a list of lines.
   The opening LF of the text block is not considered.

   Given the following example ("." is used to represent spaces),

   ::

       @documentation("""
       ....Foo
       ........Baz

       ..
       ....Bar
       ....""")

   the following lines are produced:

   .. code-block:: javascript

       ["    Foo", "        Baz", "", "  ", "    Bar", "    "]

2. Compute the *common whitespace prefix* by iterating over each line,
   counting the number of leading spaces (" ") and taking the minimum count.
   Except for the last line of content, lines that are empty or consist wholly
   of whitespace are not considered. If the last line of content (that is, the
   line that contains the closing delimiter) appears on its own line, then
   that line's leading whitespace **is** considered when determining the
   common whitespace prefix, allowing the closing delimiter to determine the
   amount of indentation to remove.

   Using the previous example, the common whitespace prefix is four spaces.
   The empty third line and the blank fourth lines are not considered when
   computing the common whitespace. The following uses "." to represent the
   common whitespace prefix:

   ::

       @documentation("""
       ....Foo
       ....    Baz

       ....
       ....Bar
       ....""")

3. Remove the common white space prefix from each line.

   This step produces the following values from the previous example:

   .. code-block:: javascript

       ["Foo", "    Baz", "", "", "Bar", ""]

4. Remove any trailing spaces from each line.

5. Concatenate each line together, separated by LF.

   This step produces the following result ("|" is used to represent the
   left margin):

   .. code-block:: none

       |Foo
       |    Baz
       |
       |
       |Bar
       |


Significant trailing line
^^^^^^^^^^^^^^^^^^^^^^^^^

The last line of text block content is used when determining the common
whitespace prefix.

Consider the following example:

::

       @documentation("""
           Foo
               Baz
           Bar
       """)

Because the closing delimiter is at the margin and left of the rest of the
content, the common whitespace prefix is 0 characters, resulting in the
following equivalent string:

::

       @documentation("    Foo\n        Baz\n    Bar\n")

If the closing delimiter is moved to the right of the content, then is has
no bearing on the common whitespace prefix. The common whitespace prefix in
the following example is visualized using "." to represent spaces:

::

       @documentation("""
       ....Foo
       ....    Baz
       ....Bar
               """)

Because lines are trimmed when they are added to the result, the above example
is equivalent to the following:

::

       @documentation("Foo\n    Baz\nBar\n")


Escapes in text blocks
^^^^^^^^^^^^^^^^^^^^^^

Text blocks support all of the :ref:`string escape characters <string-escape-characters>`
of other strings. The use of three double quotes allows unescaped double quotes
(") to appear in text blocks. The following text block is interpreted as
``"hello!"``:

.. code-block:: none

    """
    "hello!"
    """

Three quotes can appear in a text block without being treated as the closing
delimiter as long as one of the quotes are escaped. The following text block
is interpreted as ``foo """\nbaz``:

.. code-block:: none

    """
    foo \"""
    baz"""

String escapes are interpreted **after** :ref:`incidental whitespace <incidental-whitespace>`
is removed from a text block. The following example uses "." to denote spaces:

.. code-block:: none

    """
    ..<div>
    ....<p>Hi\\n....bar</p>
    ..</div>
    .."""

Because string escapes are expanded after incidental whitespace is removed, it
is interpreted as:

.. code-block:: none

    <div>
    ..<p>Hi
    ....bar</p>
    </div>

New lines in the text block can be escaped. This allows for long, single-line
strings to be broken into multiple lines in the IDL. The following example
is interpreted as ``Foo Baz Bam``:

.. code-block:: none

    """
    Foo \
    Baz \
    Bam"""

Escaped new lines can be intermixed with unescaped newlines. The following
example is interpreted as ``Foo\nBaz Bam``:

.. code-block:: none

    """
    Foo
    Baz \
    Bam"""


.. _string-escape-characters:

String escape characters
~~~~~~~~~~~~~~~~~~~~~~~~

The Smithy IDL supports escape sequences only within quoted strings. Smithy
supports all of the same escape sequences as JSON plus escaping of single
quotes.

The following sequences are allowed:

.. list-table::
    :header-rows: 1
    :widths: 10 35 55

    * - Unicode code point
      - Smithy escape
      - Meaning
    * - U+0022
      - ``\"``
      - double quote
    * - U+0027
      - ``\'``
      - single quote
    * - U+005C
      - ``\\``
      - backslash
    * - U+002F
      - ``\/``
      - forward slash
    * - U+0008
      - ``\b``
      - backspace BS
    * - U+000C
      - ``\f``
      - form feed FF
    * - U+000A
      - ``\n``
      - line feed LF
    * - U+000D
      - ``\r``
      - carriage return CR
    * - U+0009
      - ``\t``
      - horizontal tab HT
    * - U+HHHH
      - ``\uHHHH``
      - 4-digit hexadecimal Unicode code point
    * - *nothing*
      - ``\\r\n``, ``\\r``, ``\\n``
      - escaped new line expands to nothing

Any other sequence following a backslash is an error.


.. _node-values:

Node values
-----------

*Node values* are analogous to JSON values. Node values are used to define
:ref:`metadata <metadata>` and :ref:`trait values <trait-values>`.

Smithy's node values have many advantages over JSON: comments,
unquoted keys, unquoted strings, single quoted strings, long strings,
and trailing commas.

.. productionlist:: smithy
    node_value          :  `text`
                        :/ `number`
                        :/ `node_array`
                        :/ `node_object`
    node_array          :"[" [`node_value` *("," `node_value`)]
                        :(( "," "]" ) / "]" )
    node_object         :"{" [`node_object_kvp` *("," `node_object_kvp`)]
                        :(( "," "}" ) / "}" )
    node_object_kvp     :`text` ":" `node_value`
    number              :[`minus`] `int` [`frac`] [`exp`]
    decimal_point       :%x2E ; .
    digit1_9            :%x31-39 ; 1-9
    e                   :%x65 / %x45 ; e E
    exp                 :`e` [`minus` / `plus`] 1*DIGIT
    frac                :`decimal_point` 1*DIGIT
    int                 :`zero` / (`digit1_9` *DIGIT)
    minus               :%x2D ; -
    plus                :%x2B ; +
    zero                :%x30 ; 0

The following example defines a string metadata key:

::

    metadata foo = baz

The following example defines an integer metadata key:

::

    metadata foo = 100

The following example defines an array metadata key:

::

    metadata foo = [hello, 123, true, [false]]

The following example defines a complex object metadata key:

::

    metadata foo = {
      hello: 123,
      'foo': "456",
      testing: "this is " "a single string",
      an_array: [10.5],
      nested-object: {
        hello-there$: true
      }, // <-- Trailing comma
    }


.. _shape-id-abnf:

Shape ID ABNF
=============

:ref:`Shape IDs <shape-id>` adhere to the following ABNF.

.. admonition:: Lexical note
   :class: important

   Whitespace is **significant** in shape IDs.

.. productionlist:: smithy
    identifier             :(ALPHA / "_") *(ALPHA / DIGIT / "_")
    namespace              :`identifier` *("." `identifier`)
    shape_id               :`absolute_shape_id` / `relative_shape_id`
    absolute_shape_id      :`namespace` "#" `relative_shape_id`
    relative_shape_id      :`identifier` ["$" `identifier`]
    LOALPHA                :%x61-7A ; a-z


.. _json-ast:

JSON AST
========

Smithy models written using the Smithy IDL have an isomorphic JSON
abstract syntax tree (AST) representation that can be used to more easily
integrate Smithy into languages and tools that do not have a Smithy IDL
parser.

* Smithy JSON models can be merged together with other JSON models or other
  Smithy IDL models using the rules defined in :ref:`merging-models`.
* Unless specified otherwise, the same constraints and logic is used to load
  JSON models that is used to load Smithy IDL models.


Top level properties
--------------------

Smithy JSON models are objects that can contain the following top-level
properties:

.. list-table::
    :header-rows: 1
    :widths: 10 25 65

    * - Property
      - Type
      - Description
    * - smithy
      - ``string``
      - **Required**. Defines the :ref:`version <smithy-version>` of the
        Smithy specification (e.g., "1.0").
    * - metadata
      - object
      - Defines all of the :ref:`metadata <metadata>` about the model
        using a JSON object.
    * - *[additional properties]*
      - Map<``string``, :ref:`namespace <json-namespace>`>
      - Any additional property is considered a namespace definition
        (e.g., "my.namespace"). Additional properties MUST match the
        :token:`namespace` ABNF grammar.


.. _json-namespace:

Namespace definition
--------------------

A namespace is an object that contains the following properties:

.. list-table::
    :header-rows: 1
    :widths: 10 10 80

    * - Property
      - Type
      - Description
    * - shapes
      - object
      - Defines shapes in a namespace.

        ``shapes`` is a map of shape names to
        :ref:`shape definitions <json-shapes>`. Each shape name MUST adhere to
        the :token:`identifier` ABNF grammar.
    * - traits
      - object
      - Applies traits to shapes outside of a shape's definition.

        ``traits`` is a map of shape names to a map of traits to apply to
        the shape. Each key is a relative shape ID that MUST be present in
        the model, and each value is a map of trait names to trait values.

        Trait names referenced in the ``traits`` property MUST have a
        corresponding trait definition defined in ``traitDefs`` or correspond
        to a trait defined in the ``smithy.api`` namespace.

        Trait names that do not include a namespace are
        :ref:`resolved against the current namespace <trait-name-resolution>`.
    * - traitDefs
      - object
      - Defines trait definitions in a namespace.

        ``traitDefs`` is a map of trait names to trait definitions. Trait
        definitions in the JSON format support the same key-value pairs as
        :ref:`traits defined in the Smithy IDL <trait-definition>`.

        Each trait name in ``traitDefs`` MUST adhere to the
        :token:`identifier` ABNF grammar.


.. _json-shapes:

Shapes
------

:ref:`Shapes <shapes>` are defined using objects that always contain a
``type`` property to define the shape type.

Any additional properties found in shape definitions are considered
:ref:`traits <traits>` to apply to the shape. The following example defines a
``string`` shape with a :ref:`documentation-trait` trait:

.. code-block:: json

    {
      "smithy": "1.0",
      "smithy.example": {
        "shapes": {
          "MyString": {
            "type": "string",
            "documentation": "My documentation string"
          }
        }
      }
    }


Simple shapes
~~~~~~~~~~~~~

:ref:`Simple shapes <simple-types>` are defined as an object. The following
example defines a shape for each simple type:

.. code-block:: json

    {
      "smithy": "1.0",
      "smithy.example": {
        "shapes": {
          "Blob": {"type": "blob"},
          "Boolean": {"type": "boolean"},
          "String": {"type": "string"},
          "Byte": {"type": "byte"},
          "Short": {"type": "short"},
          "Integer": {"type": "integer"},
          "Long": {"type": "long"},
          "Float": {"type": "float"},
          "Double": {"type": "double"},
          "BigInteger": {"type": "bigInteger"},
          "BigDecimal": {"type": "bigDecimal"},
          "Timestamp": {"type": "timestamp"}
        }
      }
    }


List and set shapes
~~~~~~~~~~~~~~~~~~~

The :ref:`list` and :ref:`set` shapes have the following properties:

.. list-table::
    :header-rows: 1
    :widths: 10 20 70

    * - Property
      - Type
      - Description
    * - member
      - :ref:`json-member`
      - **Required**. Member of the list.

The following example defines a list with a string member:

.. code-block:: json

    {
      "smithy": "1.0",
      "smithy.example": {
        "shapes": {
          "MyList": {
            "type": "list",
            "member": { "target": "smithy.api#String" }
          }
        }
      }
    }


Map shape
~~~~~~~~~

A :ref:`map` shape has the following properties:

.. list-table::
    :header-rows: 1
    :widths: 10 20 70

    * - Property
      - Type
      - Description
    * - key
      - :ref:`json-member`
      - **Required**. Defines the shape of the map key that MUST resolve to a
        string shape.
    * - value
      - :ref:`json-member`
      - **Required**. Value shape of the map.

The following example defines a map of strings to numbers:

.. code-block:: json

    {
      "smithy": "1.0",
      "smithy.example": {
        "shapes": {
          "IntegerMap": {
            "type": "map",
            "key": { "target": "smithy.api#String" },
            "value": { "target": "smithy.api#Integer" }
          }
        }
      }
    }


Structure and union shapes
~~~~~~~~~~~~~~~~~~~~~~~~~~

:ref:`Structure <structure>` and :ref:`union <union>` shapes are defined using
an object with the following properties:

.. list-table::
    :header-rows: 1
    :widths: 10 30 60

    * - Property
      - Type
      - Description
    * - members
      - Map<string, :ref:`json-member`>
      - Map of member name to member definitions.

Structure and union member names MUST be case-insensitvely unique across the
entire set of members. Each member name MUST adhere to the :token:`identifier`
ABNF grammar.

The following example defines a structure with one required and one optional
member:

.. code-block:: json

    {
      "smithy": "1.0",
      "smithy.example": {
        "shapes": {
          "MyStructure": {
            "type": "structure",
            "members": {
              "stringMember": {
                "target": "smithy.api#String",
                "required": true
              },
              "numberMember": {
                "target": "smithy.api#Integer"
              }
            }
          }
        }
      }
    }

The following example defines a union:

.. code-block:: json

    {
      "smithy": "1.0",
      "smithy.example": {
        "shapes": {
          "MyUnion": {
            "type": "union",
            "members": {
              "a": {
                "target": "smithy.api#String"
              },
              "b": {
                "target": "smithy.api#Integer"
              }
            }
          }
        }
      }
    }


.. _json-member:

Member shape
~~~~~~~~~~~~

:ref:`Members <member>` are defined in :ref:`aggregate types <aggregate-types>`
to reference other shapes. Like other shapes, any additional properties in a
member definition are considered traits to apply to the member. A member
definition is an object that contains the following properties:

.. list-table::
    :header-rows: 1
    :widths: 10 20 70

    * - Property
      - Type
      - Description
    * - target
      - :ref:`shape-id`
      - **Required**. :ref:`shape-id` string.

The following example defines the member of a list shape and attaches the
documentation trait to the member:

.. code-block:: json

    {
      "smithy": "1.0",
      "smithy.example": {
        "shapes": {
          "MyList": {
            "type": "list",
            "member": {
              "target": "MyString",
              "documentation": "Documentation specific to the member of the list."
            }
          }
        }
      }
    }


.. _service-json-shape:

Service shape
~~~~~~~~~~~~~

:ref:`Service <service>` shapes are defined using an object. Service Shapes
defined in JSON support the same properties as the Smithy IDL.


.. _resource-json-shape:

Resource shape
~~~~~~~~~~~~~~

:ref:`Resource <resource>` shapes are defined using an object. Resource Shapes
defined in JSON support the same properties as the Smithy IDL.


.. _operation-json-shape:

Operation shape
~~~~~~~~~~~~~~~

:ref:`Operation <operation>` shapes are defined using an object with the
following properties:


.. list-table::
    :header-rows: 1
    :widths: 10 28 62

    * - Property
      - Type
      - Description
    * - input
      - :ref:`shape-id`\<:ref:`structure`\>
      - Defines the optional input structure of the operation.
    * - output
      - :ref:`shape-id`\<:ref:`structure`\>
      - Defines the optional output structure of the operation.
    * - errors
      - [ :ref:`shape-id`\<:ref:`structure`\> ]
      - Defines the list of errors that MAY be encountered when invoking
        the operation. Each element in the list is a :ref:`shape ID <shape-id>`
        that MUST resolve to a :ref:`structure` shape that is marked with the
        :ref:`error-trait` trait.

The following example defines an operation, its input, output, and errors:

.. code-block:: json

    {
      "smithy": "1.0",
      "smithy.example": {
        "shapes": {
          "MyOperation": {
            "type": "operation",
            "input": "MyOperationInput",
            "output": "MyOperationOutput",
            "errors": ["BadRequestError", "NotFoundError"]
          },
          "MyOperationInput": {
            "type": "structure",
          },
          "MyOperationOutput": {
            "type": "structure",
          },
          "BadRequestError": {
            "type": "structure",
            "error": "client"
          },
          "NotFoundError": {
            "type": "structure",
            "error": "client"
          }
        }
      }
    }

.. _ABNF: https://tools.ietf.org/html/rfc5234
