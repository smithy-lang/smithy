.. _smithy-language-specification:

=============================
Smithy Language Specification
=============================

This document defines the ABNF_ grammar and syntax for defining models using
the *Smithy interface definition language* (IDL) and the JSON abstract syntax
tree (AST).

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
    statement               :`control_statement`
                            :/ `metadata_statement`
                            :/ `use_statement`
                            :/ `namespace_statement`
                            :/ `apply_statement`
                            :/ `documentation_comment`
                            :/ `shape_statement`


Lexical notes
-------------

Smithy models MUST be encoded using UTF-8 and SHOULD use Unix style
line endings (``\n``).

Whitespace is insignificant except for the following cases:

* :token:`br` production which indicates that a new line MUST occur
* :ref:`shape ID ABNF productions <shape-id-abnf>`

.. productionlist:: smithy
    br                  :%x0A / %x0D.0A ; \n and \r\n

.. _comments:

Comments
~~~~~~~~

Comments can occur at any place in the IDL between tokens where whitespace
is insignificant. Comments in Smithy are defined using two forward slashes
followed by any character. A newline terminates a comment.

.. productionlist:: smithy
    line_comment        :"//" *(start_comment *`not_newline`)
    start_comment       :%x09 / %x20-%x46 / %x48-10FFFF ; Any character except "/" and newline
    not_newline         :%x09 / %x20-10FFFF ; Any character except newline

.. code-block:: smithy
    :caption: Example

    // This is a comment
    namespace com.foo // This is also a comment

    // Another comment
    string MyString


.. _control-statement:

Control statement
-----------------

Control statements apply metadata to a file. Control statements MUST be
defined at the beginning of a Smithy file before any other statements.

.. productionlist:: smithy
    control_statement       :"$" `text` ":" `node_value`

Implementations SHOULD ignore unknown control statements.


.. _version-statement:

Version statement
~~~~~~~~~~~~~~~~~

The version control statement is used to set the :ref:`version <smithy-version>`
of a Smithy model file. The value of a version statement MUST be a string.
Only a single version statement can appear in a model file.

.. code-block:: smithy
    :caption: Example

    $version: "1.0.0"


.. _metadata-statement:

Metadata statement
------------------

The metadata statement is used to attach arbitrary :ref:`metadata <metadata>`
to a model.

.. productionlist:: smithy
    metadata_statement:"metadata" `metadata_key` "=" `metadata_value`
    metadata_key:`text`
    metadata_value:`node_value`

.. code-block:: smithy
    :caption: Example

    metadata example.string1 = "hello there"
    metadata example.string2 = 'hello there'
    metadata example.bool1 = true
    metadata example.bool2 = false
    metadata example.number = 10
    metadata example.array = [10, true, "hello"]
    metadata example.object = {foo: "baz"}
    metadata example.null = null

Top-level metadata key-value pair conflicts are resolved by
:ref:`merging metadata <merging-metadata>`. Metadata statements MUST appear
before any namespace statements or shapes are defined.


.. _namespace-statement:

Namespace statement
-------------------

The namespace statement is used to set the *current namespace*. Shapes
can only be defined if a current namespace is defined. Only a single
namespace can appear in an IDL model file.

.. productionlist:: smithy
    namespace_statement     :"namespace" `namespace`

.. code-block:: smithy
    :caption: Example

    namespace com.foo.baz


.. _use-statement:

Use statement
-------------

A use statement is used to import shapes and traits into the current namespace
so that they can be referred to using relative shape. A use statement MUST
come after a :ref:`namespace statement <namespace-statement>` and before any
shapes are defined in an IDL model file.

.. productionlist:: smithy
    use_statement         :"use" `absolute_shape_id`

The following example imports ``smithy.example#Foo`` and
``smithy.example#Baz`` so that they can be referred to by relative shape IDs:

.. code-block:: smithy

    namespace smithy.hello

    use smithy.example#Foo
    use smithy.example#Baz

    map MyMap {
        // Resolves to smithy.example#Foo
        key: Foo,
        // Resolves to smithy.example#Baz
        value: Baz,
    }

A use statement can import traits too. The following example imports the
``smithy.example#test`` and ``smithy.example#anotherTrait`` traits so that
they can be applied using relative shape IDs:

.. code-block:: smithy

    namespace smithy.hello

    use smithy.example#test
    use smithy.example#anotherTrait

    @test // <-- Resolves to smithy.example#test
    string MyString

.. important::

    #. A shape cannot be defined in a file with the same name as one of the
       shapes imported with a ``use`` statement.
    #. Shapes IDs with members names cannot be imported with a use statement.

See :ref:`relative-shape-id` for an in-depth description of how relative
shape IDs are resolved in the IDL.


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
    operation_statement     :"operation" `identifier` `node_object`
    structure_statement     :"structure" `structured_body`
    union_statement         :"union" `structured_body`
    structured_body         :`identifier` "{" [`structured_member` *("," `structured_member`)] "}"
    structured_member       :`member_traits` `identifier` ":" `shape_id`
    list_statement          :"list" `list_and_set_body`
    set_statement           :"set" `list_and_set_body`
    list_and_set_body       :`identifier` "{" `member_traits` "member" ":" `shape_id` [","] "}"
    map_statement           :"map" `identifier` "{" `map_body` "}"
    map_body                :`map_member` "," `map_member` [","]
    map_member              :`member_traits` ("key" / "value") ":" `shape_id`
    simple_shape            :`simple_shape_name` `identifier`
    simple_shape_name       :"blob" / "boolean" / "document" / "string" / "byte" / "short"
                            :/ "integer" / "long" / "float" / "double" / "bigInteger"
                            :/ "bigDecimal" / "timestamp"


Apply statement
---------------

The apply statement is used to attach a trait to a shape outside of a shape's
definition.

.. productionlist:: smithy
    apply_statement         :"apply" `shape_id` `trait`

The following example applies the :ref:`deprecated-trait` trait to a shape
named ``MyShape`` using a :ref:`relative shape id <relative-shape-id>`.

.. code-block:: smithy

    apply MyShape @deprecated


.. _documentation-comment:

Documentation comment
---------------------

Documentation comments are a special kind of comment that provide
documentation for shapes. A documentation comment is formed when three
forward slashes (``"///"``) appear as the first non-whitespace characters
on a line.

.. productionlist:: smithy
    documentation_comment   :"///" *(`not_newline`)

Documentation comments are defined using CommonMark_. The text after the
forward slashes is considered the contents of the line. If the text starts
with a space (" "), the leading space is removed from the content.
Successive documentation comments are combined together using a newline
("\\n") to form the documentation of a shape or trait definition.

.. note::

    Documentation comments are just syntactic sugar for applying
    the :ref:`documentation-trait`.

The following Smithy IDL example,

.. code-block:: smithy

    namespace smithy.example

    /// This is documentation about a shape.
    ///
    /// - This is a list
    /// - More of the list.
    string MyString

    /// This is documentation about a trait definition.
    ///   More docs here.
    @trait
    structure myTrait {}

is equivalent to the following JSON model:

.. code-block:: json

    {
        "smithy": "1.0.0",
        "shapes": {
            "smithy.example#MyString": {
                "type": "string",
                "traits": {
                    "smithy.api#documentation": "This is documentation about a shape.\n\n- This is a list\n- More of the list."
                }
            },
            "smithy.example#myTrait": {
                "type": "structure",
                "traits": {
                    "smithy.api#trait": {},
                    "smithy.api#documentation": "This is documentation about a trait definition.\n  More docs here."
                }
            }
        }
    }

Documentation comments MUST appear immediately before a shape, and they MUST
appear **before** any traits applied to the shape.

The following example is valid because the documentation comment comes
before the traits applied to a shape:

.. code-block:: smithy

    /// A deprecated string.
    @deprecated
    string MyString

Documentation comments can be applied to members of a shape.

.. code-block:: smithy

    // Documentation about the structure.
    structure Example {
        /// Documentation about the member.
        @sensitive
        foo: String,
    }

Documentation comments MUST NOT be applied to anything other than shapes.
The following documentation comments are all invalid.

.. code-block:: smithy

    /// Invalid (cannot apply to control statements)
    $version: "1.0.0"

    /// Invalid (cannot apply to namespaces)
    namespace smithy.example

    /// Invalid (cannot apply to metadata)
    metadata foo = "baz"

    @deprecated
    /// Invalid (comes after the @deprecated trait)
    structure Example {
        /// Invalid (cannot apply docs to '}')
    }

    /// Invalid (nothing comes after the comment)


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

.. code-block:: smithy

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

.. productionlist:: smithy
    text                :`unquoted_text` / `quoted_text` / `text_block`
    unquoted_text       :(ALPHA / "_") *(ALPHA / DIGIT / "_" / "$" / "." / "#")
    escaped_char        :`escape` (`escape` / "'" / DQUOTE / "b" / "f" / "n" / "r" / "t" / "/" / `unicode_escape`)
    unicode_escape      :"u" `hex` `hex` `hex` `hex`
    hex                 : DIGIT / %x41-46 / %x61-66
    quoted_text         :DQUOTE *`quoted_char` DQUOTE
    quoted_char         :%x20-21
                        :/ %x23-5B
                        :/ %x5D-10FFFF
                        :/ `escaped_char`
                        :/ `preserved_double`
    preserved_double    :`escape` (%x20-21 / %x23-5B / %x5D-10FFFF)
    escape              :%x5C ; backslash
    text_block          :DQUOTE DQUOTE DQUOTE `br` `quoted_char` DQUOTE DQUOTE DQUOTE

New lines in strings are normalized from CR (\u000D) and CRLF (\u000D\u000A)
to LF (\u000A). This ensures that strings defined in a Smithy model are
equivalent across platforms. If a literal ``\r`` is desired, it can be added
a string value using the Unicode escape ``\u000d``.


.. _unquoted-strings:

Unquoted strings
~~~~~~~~~~~~~~~~

Unquoted strings that appear in the IDL as part of a trait value or metadata
value are treated as shape IDs. Strings MUST be quoted if a value is not
intended to be converted into a resolved shape ID.

.. seealso::

   Refer to :ref:`syntactic-shape-ids` for more information.


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

.. code-block:: smithy

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

.. code-block:: smithy

    @documentation("<div>\n    <p>Hello!</p>\n</div>\n")

The closing delimiter can be placed on the same line as content if no new line
is desired at the end of the result. The above example could be rewritten to
not including a trailing new line:

.. code-block:: smithy

    @documentation("""
        <div>
            <p>Hello!</p>
        </div>""")

This example is equivalent to the following:

.. code-block:: smithy

    @documentation("<div>\n    <p>Hello!</p>\n</div>")

The following text blocks are ill-formed:

.. code-block:: smithy

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

   .. code-block:: smithy

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

   .. code-block:: smithy

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

.. code-block:: smithy

       @documentation("""
           Foo
               Baz
           Bar
       """)

Because the closing delimiter is at the margin and left of the rest of the
content, the common whitespace prefix is 0 characters, resulting in the
following equivalent string:

.. code-block:: smithy

       @documentation("    Foo\n        Baz\n    Bar\n")

If the closing delimiter is moved to the right of the content, then it has
no bearing on the common whitespace prefix. The common whitespace prefix in
the following example is visualized using "." to represent spaces:

.. code-block:: smithy

       @documentation("""
       ....Foo
       ....    Baz
       ....Bar
               """)

Because lines are trimmed when they are added to the result, the above example
is equivalent to the following:

.. code-block:: smithy

       @documentation("Foo\n    Baz\nBar\n")


Escapes in text blocks
^^^^^^^^^^^^^^^^^^^^^^

Text blocks support all of the :ref:`string escape characters <string-escape-characters>`
of other strings. The use of three double quotes allows unescaped double quotes
(") to appear in text blocks. The following text block is interpreted as
``"hello!"``:

.. code-block:: smithy

    """
    "hello!"
    """

Three quotes can appear in a text block without being treated as the closing
delimiter as long as one of the quotes are escaped. The following text block
is interpreted as ``foo """\nbaz``:

.. code-block:: smithy

    """
    foo \"""
    baz"""

String escapes are interpreted **after** :ref:`incidental whitespace <incidental-whitespace>`
is removed from a text block. The following example uses "." to denote spaces:

.. code-block:: smithy

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

.. code-block:: smithy

    """
    Foo \
    Baz \
    Bam"""

Escaped new lines can be intermixed with unescaped newlines. The following
example is interpreted as ``Foo\nBaz Bam``:

.. code-block:: smithy

    """
    Foo
    Baz \
    Bam"""


.. _string-escape-characters:

String escape characters
~~~~~~~~~~~~~~~~~~~~~~~~

The Smithy IDL supports escape sequences only within quoted strings. Smithy
supports all of the same escape sequences as JSON.

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
    node_value          :`text` / `number` / `node_array` / `node_object`
    node_array          :"[" [`node_value` *("," `node_value`)] (( "," "]" ) / "]" )
    node_object         :"{" [`node_object_kvp` *("," `node_object_kvp`)] (( "," "}" ) / "}" )
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

.. code-block:: smithy

    metadata foo = "baz"

The following example defines an integer metadata key:

.. code-block:: smithy

    metadata foo = 100

The following example defines an array metadata key:

.. code-block:: smithy

    metadata foo = ["hello", 123, true, [false]]

The following example defines a complex object metadata key:

.. code-block:: smithy

    metadata foo = {
        hello: 123,
        'foo': "456",
        testing: """
            Hello!
            """,
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
* All shape IDs in the JSON AST MUST be absolute shape IDs that contain a
  namespace. One of the main drivers of the simplicity of the the JSON AST
  over the Smithy IDL is that relative and forward references never need to
  be resolved.


Top level properties
--------------------

Smithy JSON models are objects that can contain the following top-level
properties:

.. list-table::
    :header-rows: 1
    :widths: 10 30 60

    * - Property
      - Type
      - Description
    * - smithy
      - ``string``
      - **Required**. Defines the :ref:`version <smithy-version>` of the
        Smithy specification (e.g., "|version|").
    * - metadata
      - object
      - Defines all of the :ref:`metadata <metadata>` about the model
        using a JSON object. Each key is the metadata key to set, and each
        value is the metadata value to assign to the key.
    * - shapes
      - Map<:ref:`shape ID <shape-id>`, :ref:`AST shape <ast-shapes>`>
      - A map of absolute shape IDs to shape definitions.


.. _ast-shapes:

AST shapes
----------

AST :ref:`shapes <shapes>` are defined using objects that always contain
a ``type`` property to define the shape type or ``apply``.

.. code-block:: json

    {
        "smithy": "1.0.0",
        "shapes": {
            "smithy.example#MyString": {
                "type": "string"
            }
        }
    }

All entries in the ``shapes`` map can contain a ``traits`` property that
defines the traits attached to the shape. ``traits`` is a map of where
each key is the absolute shape ID of a trait definition and each value is
the value to assign to the trait.

.. code-block:: json

    {
        "traits": {
            "smithy.example#documentation": "Hi!",
            "smithy.example#tags": [
                "a",
                "b"
            ]
        }
    }

The following example defines a string shape with a documentation trait.

.. code-block:: json

    {
        "smithy": "1.0.0",
        "shapes": {
            "smithy.example#MyString": {
                "type": "string",
                "traits": {
                    "smithy.api#documentation": "My documentation string"
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
        "smithy": "1.0.0",
        "shapes": {
            "smithy.example#Blob": {
                "type": "blob"
            },
            "smithy.example#Boolean": {
                "type": "boolean"
            },
            "smithy.example#Document": {
                "type": "document"
            },
            "smithy.example#String": {
                "type": "string"
            },
            "smithy.example#Byte": {
                "type": "byte"
            },
            "smithy.example#Short": {
                "type": "short"
            },
            "smithy.example#Integer": {
                "type": "integer"
            },
            "smithy.example#Long": {
                "type": "long"
            },
            "smithy.example#Float": {
                "type": "float"
            },
            "smithy.example#Double": {
                "type": "double"
            },
            "smithy.example#BigInteger": {
                "type": "bigInteger"
            },
            "smithy.example#BigDecimal": {
                "type": "bigDecimal"
            },
            "smithy.example#Timestamp": {
                "type": "timestamp"
            }
        }
    }


List and set shapes
~~~~~~~~~~~~~~~~~~~

:ref:`list` and :ref:`set` shapes have a required ``member`` property
that is an :ref:`AST member <ast-member>`.

The following example defines a list with a string member:

.. code-block:: json

    {
        "smithy": "1.0.0",
        "shapes": {
            "smithy.example#MyList": {
                "type": "list",
                "member": {
                    "target": "smithy.api#String"
                }
            }
        }
    }

The following example defines a set with a string member:

.. code-block:: json

    {
        "smithy": "1.0.0",
        "shapes": {
            "smithy.example#MySet": {
                "type": "set",
                "member": {
                    "target": "smithy.api#String"
                }
            }
        }
    }


.. _ast-member:

AST member
~~~~~~~~~~

An *AST member definition* defines a member of a shape. It is a special
kind of :ref:`AST shape reference <ast-shape-reference>` that also
contains an optional ``traits`` property that defines traits attached to
the member. Each key in the ``traits`` property is the absolute shape ID
of the trait to apply, and each value is the value to assign to the
trait.

.. code-block:: json

    {
        "target": "smithy.example#MyShape",
        "traits": {
            "smithy.example#documentation": "Hi!"
        }
    }

The following example defines a list shape and its member.

.. code-block:: json

    {
        "smithy": "1.0.0",
        "shapes": {
            "smithy.example#MyList": {
                "type": "list",
                "member": {
                    "target": "smithy.api#String",
                    "traits": {
                        "smithy.api#documentation": "Member documentation"
                    }
                }
            }
        }
    }


.. _ast-shape-reference:

AST shape reference
~~~~~~~~~~~~~~~~~~~

An *AST shape reference* is an object with a single property, ``target``
that maps to an absolute shape ID.

.. code-block:: json

    {
        "target": "smithy.example#MyShape"
    }

The following example defines a shape reference inside of the ``operations``
list of a service shape.

.. code-block:: json

    {
        "smithy": "1.0.0",
        "shapes": {
            "smithy.example#Service": {
                "type": "service",
                "operations": [
                    {
                        "target": "smithy.example#Operation"
                    }
                ]
            },
            "smithy.example#Operation": {
                "type": "operation"
            }
        }
    }


Map shape
~~~~~~~~~

A :ref:`map` shape has a required ``key`` and ``value``
:ref:`AST member <ast-member>`. The shape referenced by the ``key`` member
MUST target a string shape.

The following example defines a map of strings to numbers:

.. code-block:: json

    {
        "smithy": "1.0.0",
        "shapes": {
            "smithy.example#IntegerMap": {
                "type": "map",
                "key": {
                    "target": "smithy.api#String"
                },
                "value": {
                    "target": "smithy.api#Integer"
                }
            }
        }
    }


Structure and union shapes
~~~~~~~~~~~~~~~~~~~~~~~~~~

:ref:`Structure <structure>` and :ref:`union <union>` shapes are defined
with a ``members`` property that contains a map of member names to
:ref:`AST member <ast-member>` definitions. A union shape requires at least
one member, and a structure shape MAY omit the ``members`` property
entirely if the structure contains no members.

Structure and union member names MUST be case-insensitively unique across the
entire set of members. Each member name MUST adhere to the :token:`identifier`
ABNF grammar.

The following example defines a structure with one required and one optional
member:

.. code-block:: json

    {
        "smithy": "1.0.0",
        "shapes": {
            "smithy.example#MyStructure": {
                "type": "structure",
                "members": {
                    "stringMember": {
                        "target": "smithy.api#String",
                        "traits": {
                            "smithy.api#required": true
                        }
                    },
                    "numberMember": {
                        "target": "smithy.api#Integer"
                    }
                }
            }
        }
    }

The following example defines a union:

.. code-block:: json

    {
        "smithy": "1.0.0",
        "shapes": {
            "smithy.example#MyUnion": {
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


.. _service-ast-shape:

Service shape
~~~~~~~~~~~~~

:ref:`Service <service>` shapes are defined using an object. Service
shapes defined in JSON support the same properties as the Smithy IDL.

.. list-table::
    :header-rows: 1
    :widths: 10 28 62

    * - Property
      - Type
      - Description
    * - type
      - string
      - ``service``
    * - version
      - ``string``
      - **Required**. Defines the version of the service. The version can be
        provided in any format (e.g., ``2017-02-11``, ``2.0``, etc).
    * - :ref:`operations <service-operations>`
      - [:ref:`AST shape reference <ast-shape-reference>`]
      - Binds a list of operations to the service. Each reference MUST target
        an operation.
    * - :ref:`resources <service-resources>`
      - [:ref:`AST shape reference <ast-shape-reference>`]
      - Binds a list of resources to the service. Each reference MUST target
        a resource.
    * - traits
      - Map\<:ref:`shape ID <shape-id>`, trait value>
      - Traits to apply to the service

.. code-block:: json

    {
        "smithy": "1.0.0",
        "shapes": {
            "smithy.example#MyService": {
                "type": "service",
                "version": "2017-02-11",
                "operations": [
                    {
                        "target": "smithy.example#GetServerTime"
                    }
                ],
                "resources": [
                    {
                        "target": "smithy.example#SomeResource"
                    }
                ]
            }
        }
    }


.. _resource-ast-shape:

Resource shape
~~~~~~~~~~~~~~

:ref:`Resource <resource>` shapes are defined using an object. Resource
shapes defined in JSON support the same properties as the Smithy IDL.

.. list-table::
    :header-rows: 1
    :widths: 10 28 62

    * - Property
      - Type
      - Description
    * - type
      - string
      - ``service``
    * - :ref:`identifiers <resource-identifiers>`
      - Map<String, :ref:`AST shape reference <ast-shape-reference>`>
      - Defines identifier names and shape IDs used to identify the resource.
    * - :ref:`create <create-lifecycle>`
      - :ref:`AST shape reference <ast-shape-reference>`
      - Defines the lifecycle operation used to create a resource using one
        or more identifiers created by the service.
    * - :ref:`put <put-lifecycle>`
      - :ref:`AST shape reference <ast-shape-reference>`
      - Defines an idempotent lifecycle operation used to create a resource
        using identifiers provided by the client.
    * - :ref:`read <read-lifecycle>`
      - :ref:`AST shape reference <ast-shape-reference>`
      - Defines the lifecycle operation used to retrieve the resource.
    * - :ref:`update <update-lifecycle>`
      - :ref:`AST shape reference <ast-shape-reference>`
      - Defines the lifecycle operation used to update the resource.
    * - :ref:`delete <delete-lifecycle>`
      - :ref:`AST shape reference <ast-shape-reference>`
      - Defines the lifecycle operation used to delete the resource.
    * - :ref:`list <list-lifecycle>`
      - :ref:`AST shape reference <ast-shape-reference>`
      - Defines the lifecycle operation used to list resources of this type.
    * - operations
      - [:ref:`AST shape reference <ast-shape-reference>`]
      - Binds a list of non-lifecycle instance operations to the resource.
        Each reference MUST target an operation.
    * - collectionOperations
      - [:ref:`AST shape reference <ast-shape-reference>`]
      - Binds a list of non-lifecycle collection operations to the resource.
        Each reference MUST target an operation.
    * - resources
      - [:ref:`AST shape reference <ast-shape-reference>`]
      - Binds a list of resources to this resource as a child resource,
        forming a containment relationship. The resources MUST NOT have a
        cyclical containment hierarchy, and a resource can not be bound more
        than once in the entire closure of a resource or service.
        Each reference MUST target a resource.
    * - traits
      - Map\<:ref:`shape ID <shape-id>`, trait value>
      - Traits to apply to the resource.

.. code-block:: json

    {
        "smithy": "1.0.0",
        "shapes": {
            "smithy.example#Thing": {
                "type": "resource",
                "identifiers": {
                    "forecastId": {
                        "target": "smithy.api#String"
                    },
                },
                "create": {
                    "target": "smithy.example#CreateThing"
                },
                "read": {
                    "target": "smithy.example#GetThing"
                },
                "update": {
                    "target": "smithy.example#Updatething"
                },
                "delete": {
                    "target": "smithy.example#DeleteThing"
                },
                "list": {
                    "target": "smithy.example#ListThings"
                },
                "operations": [
                    {
                        "target": "smithy.example#SomeInstanceOperation"
                    }
                ],
                "collectionOperations": [
                    {
                        "target": "smithy.example#SomeCollectionOperation"
                    }
                ],
                "resources": [
                    {
                        "target": "smithy.example#SomeResource"
                    }
                ]
            }
        }
    }


.. _operation-ast-shape:

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
    * - type
      - string
      - ``operation``
    * - input
      - :ref:`AST shape reference <ast-shape-reference>`
      - Defines the optional input structure of the operation. The ``input``
        of an operation MUST resolve to a :ref:`structure`.
    * - output
      - :ref:`AST shape reference <ast-shape-reference>`
      - Defines the optional output structure of the operation. The ``output``
        of an operation MUST resolve to a :ref:`structure`.
    * - errors
      - [:ref:`AST shape reference <ast-shape-reference>`]
      - Defines the list of errors that MAY be encountered when invoking
        the operation. Each reference MUST resolve to a :ref:`structure`
        shape that is marked with the :ref:`error-trait` trait.
    * - traits
      - Map\<:ref:`shape ID <shape-id>`, trait value>
      - Traits to apply to the operation.

The following example defines an operation, its input, output, and errors:

.. code-block:: json

    {
        "smithy": "1.0.0",
        "shapes": {
            "smithy.example#MyOperation": {
                "type": "operation",
                "input": {
                    "target": "smithy.example#MyOperationInput"
                },
                "output": {
                    "target": "smithy.example#MyOperationOutput"
                },
                "errors": [
                    {
                        "target": "smithy.example#BadRequestError"
                    },
                    {
                        "target": "smithy.example#NotFoundError"
                    }
                ]
            },
            "smithy.example#MyOperationInput": {
                "type": "structure"
            },
            "smithy.example#MyOperationOutput": {
                "type": "structure"
            },
            "smithy.example#BadRequestError": {
                "type": "structure",
                "traits": {
                    "smithy.api#error": "client"
                }
            },
            "smithy.example#NotFoundError": {
                "type": "structure",
                "traits": {
                    "smithy.api#error": "client"
                }
            }
        }
    }


AST apply type
~~~~~~~~~~~~~~

Traits can be applied to shapes outside of their definition by setting
``type`` to ``apply``. The ``apply`` type does not actually define a shape
for the shape ID; the shape ID MUST reference a shape or member of a shape.
The ``apply`` type allows only the ``traits`` property.

.. code-block:: json

    {
        "smithy": "1.0.0",
        "shapes": {
            "smithy.example#Struct": {
                "type": "structure",
                "members": {
                    "foo": {
                        "target": "smithy.api#String"
                    }
                }
            },
            "smithy.example#Struct$foo": {
                "type": "apply",
                "traits": {
                    "smithy.api#documentation": "My documentation string"
                }
            }
        }
    }


.. _ABNF: https://tools.ietf.org/html/rfc5234
.. _CommonMark: https://spec.commonmark.org/
