.. _lexical-structure:

============================
Smithy IDL lexical structure
============================

This document defines the ABNF_ grammar and syntax for defining models using
the *Smithy interface definition language* (IDL).

Smithy models are defined using either the Smithy IDL or the
:ref:`JSON abstract syntax tree <json-ast>` (AST). The IDL is the preferred
format for authoring and reading models, while the JSON format is preferred
for tooling and integrations.

.. contents:: Table of contents
    :depth: 2
    :local:
    :backlinks: none


.. _smithy-idl-abnf:

---------------
Smithy IDL ABNF
---------------

The Smithy IDL is defined by the following ABNF:

.. productionlist:: smithy
    idl:`ws`
       :/ `control_section`
       :/ `metadata_section`
       :/ `shape_section`


-------------
Lexical notes
-------------

Smithy models MUST be encoded using UTF-8 and SHOULD use Unix style
line endings (``\n``). The Smithy ABNF is whitespace sensitive.
Whitespace is controlled using the following productions:

.. productionlist:: smithy
    ws      :*(`sp` / `newline` / `line_comment`) ; whitespace
    sp      :*(%x20  / %x09) ; " " and \t
    br      :`sp` (`line_comment` / `newline`) `sp` ; break
    newline :%x0A / %x0D.0A ; \n and \r\n


.. _comments:

Comments
========

Comments can occur at any place in the IDL between tokens where whitespace
(:token:`ws`) can occur. Comments in Smithy are defined using two forward
slashes followed by any character. A newline terminates a comment.

.. productionlist:: smithy
    line_comment: "//" *`not_newline` `newline`
    not_newline: %x09 / %x20-10FFFF ; Any character except newline

.. code-block:: smithy
    :caption: Example

    // This is a comment
    namespace com.foo // This is also a comment

    // Another comment
    string MyString


.. _documentation-comment:

Documentation comment
---------------------

Documentation comments are a special kind of comment that provide
documentation for shapes. A documentation comment is formed when three
forward slashes (``"///"``) appear as the first non-whitespace characters
on a line.

.. productionlist:: smithy
    documentation_comment:"///" *`not_newline` `br`

Documentation comments are defined using CommonMark_. The text after the
forward slashes is considered the contents of the line. If the text starts
with a space (" "), the leading space is removed from the content.
Successive documentation comments are combined together using a newline
("\\n") to form the documentation of a shape.

.. note::

    Documentation comments are syntactic sugar equivalent to applying the
    :ref:`documentation-trait`.

The following Smithy IDL example,

.. code-block:: smithy

    namespace smithy.example

    /// This is documentation about a shape.
    ///
    /// - This is a list
    /// - More of the list.
    string MyString

    /// This is documentation about a trait shape.
    ///   More docs here.
    @trait
    structure myTrait {}

is equivalent to the following JSON AST model:

.. code-block:: json

    {
        "smithy": "1.0",
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
                    "smithy.api#documentation": "This is documentation about a trait shapes.\n  More docs here."
                }
            }
        }
    }

Documentation comments are only treated as shape documentation when the
comment appears immediately before a shape, and documentation comments MUST
appear **before** any :ref:`traits <traits>` applied to the shape in order
for the documentation to be applied to a shape.

The following example applies a documentation trait to the shape because the
documentation comment comes before the traits applied to a shape:

.. code-block:: smithy

    /// A deprecated string.
    @deprecated
    string MyString

Documentation comments can also be applied to members of a shape.

.. code-block:: smithy

    /// Documentation about the structure.
    structure Example {
        /// Documentation about the member.
        @sensitive
        foo: String,
    }


.. _control-statement:

---------------
Control section
---------------

The *control section* of a model contains :token:`control statements <control_statement>`
that apply metadata to a *specific file*. Control statements, if defined,
MUST appear at the beginning of a Smithy file before any other statements.

.. productionlist:: smithy
    control_section   :*(`control_statement`)
    control_statement :"$" `ws` `node_object_key` `ws` ":" `ws` `node_value` `br`

Implementations MUST ignore unknown control statements.


.. _smithy-version:

Version statement
=================

The Smithy specification is versioned using a ``major`` . ``minor``
versioning scheme. A version requirement is specified for a model file using
the ``$version`` control statement. When no version number is specified in
the IDL, an implementation SHOULD assume that the model can be loaded.
Because this can lead to unexpected parsing errors, models SHOULD always
include a version.

The value provided in a version control statement is a string that MUST
adhere to the following ABNF:

.. productionlist:: smithy
    version_string :1*DIGIT [ "." 1*DIGIT ]

The following example sets the version to ``1``, meaning that tooling MUST
support a version greater than or equal to ``1.0`` and less than ``2.0``:

.. tabs::

    .. code-tab:: smithy

        $version: "1"

    .. code-tab:: json

        {
            "smithy": "1"
        }

A minor version SHOULD be provided when a model depends on a feature released
in a minor update of the specification. The following example sets the
version requirement of a file to ``1.1``, meaning that tooling MUST support a
version greater than or equal to ``1.1`` and less than ``2.0``:

.. tabs::

    .. code-tab:: smithy

        $version: "1.1"

    .. code-tab:: json

        {
            "smithy": "1.1"
        }

.. rubric:: Version compatibility

A single version statement can appear in a model file, but different versions
MAY be encountered when merging multiple model files together. Multiple
versions are supported if and only if all of the version statements are
supported by the tool loading the models.


.. _shape-id:

--------
Shape ID
--------

A :dfn:`shape ID` is used to refer to shapes and traits in the model.
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


.. _shape-id-abnf:

Shape ID ABNF
=============

Shape IDs are formally defined by the following ABNF:

.. productionlist:: smithy
    shape_id               :`root_shape_id` [`shape_id_member`]
    root_shape_id          :`absolute_root_shape_id` / `identifier`
    absolute_root_shape_id :`namespace` "#" `identifier`
    namespace              :`identifier` *("." `identifier`)
    identifier             :(ALPHA / "_") *(ALPHA / DIGIT / "_")
    shape_id_member        :"$" `identifier`


.. _shape-id-member-names:

Shape ID member names
=====================

A :ref:`member <member>` of an :ref:`aggregate shape <aggregate-types>` can be
referenced in a shape ID by appending a dollar sign (``$``) followed by the
appropriate member name. Member names for each shape are defined as follows:

.. list-table::
    :header-rows: 1
    :widths: 25 40 35

    * - Shape ID
      - Syntax
      - Example
    * - :ref:`structure` member
      - ``<name>$<member-name>``
      - ``ns.example#Shape$baz``
    * - :ref:`union` member
      - ``<name>$<member-name>``
      - ``ns.example#Shape$baz``
    * - :ref:`list` member
      - ``<name>$member``
      - ``ns.example#Shape$member``
    * - :ref:`set` member
      - ``<name>$member``
      - ``ns.example#Shape$member``
    * - :ref:`map` key
      - ``<name>$key``
      - ``ns.example#Shape$key``
    * - :ref:`map` value
      - ``<name>$value``
      - ``ns.example#Shape$value``


.. _shape-id-conflicts:

Shape ID conflicts
==================

While shape IDs used within a model are case-sensitive, no two shapes in
the model can have the same case-insensitive shape ID. For example,
``com.Foo#baz`` and ``com.foo#baz`` are not allowed in the same model. This
property also extends to member names: ``com.foo#Baz$bar`` and
``com.foo#Baz$Bar`` are not allowed on the same structure.


.. _syntactic-shape-ids:

Syntactic shape IDs in the IDL
==============================

Unquoted string values that are not object keys in the Smithy IDL are
considered lexical shape IDs and are resolved to absolute shape IDs using the
process defined in :ref:`relative-shape-id`. Values that are not meant to be
shape IDs MUST be quoted.

For example, the following model resolves the value of the :ref:`error-trait`
to the shape ID ``"smithy.example#client"`` rather than using the string
literal value of ``"client"``, causing the model to be invalid:

.. code-block:: smithy

    namespace smithy.example

    @error(client) // <-- This MUST be "client"
    structure Error

    string client

Object keys in the IDL are not treated as shape IDs. :ref:`Metadata <metadata>`
is used to apply arbitrary information to a model, and it often consists of
objects. In the following metadata example, the object key remains the same
literal string value of ``String`` while the value is treated as a shape ID
and resolves to the string literal ``"smithy.api#String"``.

.. tabs::

    .. code-tab:: smithy

        metadata foo = {
            String: String,
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
            "metadata": {
                "String": "smithy.api#String"
            }
        }


.. _node-values:

-----------
Node values
-----------

*Node values* are analogous to JSON values. Node values are used to define
:ref:`metadata <metadata>` and :ref:`trait values <trait-values>`.
Smithy's node values have many advantages over JSON: comments,
unquoted keys, unquoted strings, single quoted strings, long strings,
and trailing commas.

.. productionlist:: smithy
    node_value :`node_array`
               :/ `node_object`
               :/ `number`
               :/ `node_keywords`
               :/ `shape_id`
               :/ `text_block`
               :/ `quoted_text`

.. rubric:: Array node

.. productionlist:: smithy
    node_array          :`empty_node_array` / `populated_node_array`
    empty_node_array    :"[" `ws` "]"
    populated_node_array:"[" `ws` `node_value` `ws`
                        :       *(`comma` `node_value` `ws`)
                        :       `trailing_comma` "]"
    trailing_comma      :[`comma`]
    comma               :"," `ws`

.. rubric:: Object node

.. productionlist:: smithy
    node_object          :`empty_node_object` / `populated_node_object`
    empty_node_object    :"{" `ws` "}"
    populated_node_object:"{" `ws` `node_object_kvp` `ws`
                         :       *(`comma` `node_object_kvp` `ws`)
                         :       `trailing_comma` "}"
    node_object_kvp      :`node_object_key` `ws` ":" `ws` `node_value`
    node_object_key      :`quoted_text` / `identifier`

.. rubric:: Number node

.. productionlist:: smithy
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

.. rubric:: Node keywords

Several keywords are used when parsing :token:`node_value`.

.. productionlist:: smithy
    node_keywords: "true" / "false" / "null"

* ``true``: The value is treated as a boolean ``true``
* ``false``: The value is treated as a boolean ``false``
* ``null``: The value is treated like a JSON ``null``

.. rubric:: Node examples

The following examples apply metadata to a model to demonstrate how node
values are defined.

The following example defines a string metadata entry:

.. code-block:: smithy

    metadata foo = "baz"

The following example defines an integer metadata entry:

.. code-block:: smithy

    metadata foo = 100

The following example defines an array metadata entry:

.. code-block:: smithy

    metadata foo = ["hello", 123, true, [false]]

The following example defines a complex object metadata entry:

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


-------------
String values
-------------

.. productionlist:: smithy
    quoted_text         :DQUOTE *`quoted_char` DQUOTE
    quoted_char         :%x20-21        ; space - "!"
                        :/ %x23-5B        ; "#" - "["
                        :/ %x5D-10FFFF    ; "]"+
                        :/ `escaped_char`
                        :/ `preserved_double`
    escaped_char        :`escape` (`escape` / "'" / DQUOTE / "b" / "f" / "n" / "r" / "t" / "/" / `unicode_escape`)
    unicode_escape      :"u" `hex` `hex` `hex` `hex`
    hex                 : DIGIT / %x41-46 / %x61-66
    preserved_double    :`escape` (%x20-21 / %x23-5B / %x5D-10FFFF)
    escape              :%x5C ; backslash
    text_block          :`three_dquotes` `br` *`quoted_char` `three_dquotes`
    three_dquotes       :DQUOTE DQUOTE DQUOTE

New lines in strings are normalized from CR (\u000D) and CRLF (\u000D\u000A)
to LF (\u000A). This ensures that strings defined in a Smithy model are
equivalent across platforms. If a literal ``\r`` is desired, it can be added
a string value using the Unicode escape ``\u000d``.


.. _string-escape-characters:

String escape characters
========================

The Smithy IDL supports escape sequences only within quoted strings.  The following
escape sequences are allowed:

.. list-table::
    :header-rows: 1
    :widths: 20 30 50

    * - Unicode code point
      - Escape
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


.. _text-blocks:

Text blocks
===========

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
------------------------------

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
-------------------------

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
----------------------

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


.. _ABNF: https://tools.ietf.org/html/rfc5234
.. _CommonMark: https://spec.commonmark.org/
