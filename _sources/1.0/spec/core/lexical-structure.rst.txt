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


-------------
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
========

Comments can occur at any place in the IDL between tokens where whitespace
is insignificant. Comments in Smithy are defined using two forward slashes
followed by any character. A newline terminates a comment.

.. productionlist:: smithy
    line_comment        :"//" *(`start_comment` *`not_newline`)
    start_comment       :%x09 / %x20-%x46 / %x48-10FFFF ; Any character except "/" and newline
    not_newline         :%x09 / %x20-10FFFF ; Any character except newline

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
    documentation_comment   :"///" *(`not_newline`)

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

Documentation comments MUST NOT be applied to anything other than shapes.
Documentation comments MUST appear immediately before a shape, and they MUST
appear **before** any :ref:`traits <traits>` applied to the shape.

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


.. _control-statement:

-----------------
Control statement
-----------------

Control statements apply metadata to a specific file. Control statements,
if defined, MUST appear at the beginning of a Smithy file before any other
statements.

.. productionlist:: smithy
    control_statement       :"$" `text` ":" `node_value`

Implementations MUST ignore unknown control statements.

.. note::

    Control statements differ from :ref:`metadata <metadata>` because
    control statements apply metadata to a single file, whereas metadata
    is applied to the entire model.


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
    version_string :`major_version` [ "." `minor_version` ]
    major_version  :1*(1 / 2 / 3 / 4 / 5 / 6 / 7 / 8 / 9)
    minor_version  :1*DIGIT

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
    root_shape_id          :`absolute_shape_id` / `identifier`
    absolute_shape_id      :`namespace` "#" `identifier`
    namespace              :`identifier` *("." `identifier`)
    identifier             :(ALPHA / "_") *(ALPHA / DIGIT / "_")
    shape_id_member        :"$" `identifier`
    LOALPHA                :%x61-7A ; a-z

.. admonition:: Lexical note
   :class: important

   Whitespace is **significant** in all shape ID productions.


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

:token:`Unquoted string values <unquoted_text>` in the Smithy IDL are considered
shape IDs and are resolved to absolute shape IDs using the process defined
in :ref:`relative-shape-id`. Values that are not meant to be shape IDs
MUST be quoted.

For example, the following model resolves the value of the :ref:`error-trait`
to the shape ID ``"smithy.example#client"`` rather than using the string
literal value of ``"client"``, causing the model to be invalid:

.. code-block:: smithy

    namespace smithy.example

    @error(client) // <-- This should be "client"
    structure Error

    string client

Object keys in the IDL are not treated as shape IDs. Consider the following
:ref:`metadata <metadata>` definition. The object key remains the same literal
string value of ``String`` while the value is treated as a shape ID and
resolves to the string literal ``"smithy.api#String"``.

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


.. _relative-shape-id:

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


-------------
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


.. _string-escape-characters:

String escape characters
========================

The Smithy IDL supports escape sequences only within quoted strings.  The following
escape sequences are allowed:

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


.. _unquoted-strings:

Unquoted strings
================

Unquoted strings that appear in the IDL are treated as shape IDs. Strings
MUST be quoted if a value is not intended to be converted into a resolved
shape ID.

.. seealso::

   Refer to :ref:`syntactic-shape-ids` for more information.


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
