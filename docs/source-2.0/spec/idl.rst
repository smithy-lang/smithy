.. _idl:

==========
Smithy IDL
==========

Smithy models are defined using either the Smithy interface definition language
(IDL) or the :ref:`JSON abstract syntax tree <json-ast>` (AST). This document
defines the :rfc:`ABNF <5234>` grammar and syntax for defining models with the
Smithy IDL.


-------------------
Smithy IDL overview
-------------------

The Smithy IDL is made up of 3, ordered sections, each of which is optional:

1. **Control section**; defines parser directives like which version of the
   IDL to use.
2. **Metadata section**; applies metadata to the entire model.
3. **Shape section**; where shapes and traits are defined. A namespace MUST
   be defined before any shapes or traits can be defined.
   :token:`smithy:UseStatement`\s can be defined after a namespace and before shapes
   or traits to refer to shapes in other namespaces using a shorter name.

The following example defines a model file with each section:

.. tab:: Smithy

    .. code-block:: smithy

        // (1) Control section
        $version: "2"

        // (2) Metadata section
        metadata foo = "bar"

        // (3) Shape section
        namespace smithy.example

        use smithy.other.namespace#MyString

        structure MyStructure {
            @required
            foo: MyString
        }

.. tab:: JSON

    .. code-block:: smithy

        {
            "smithy": "2",
            "metadata": {
                "foo": "bar"
            },
            "shapes": {
                "smithy.example#MyStructure": {
                    "type": "structure",
                    "members": {
                        "foo": {
                            "target": "smithy.other.namespace#MyString",
                            "traits": {
                                "smithy.api#required": {}
                            }
                        }
                    }
                }
            }
        }


-------------
Lexical notes
-------------

* Smithy models MUST be encoded using UTF-8 and SHOULD use Unix style
  line endings (``\n``).
* The Smithy ABNF is whitespace sensitive.
* Except for within strings, commas in the Smithy IDL are considered
  whitespace. Commas can be used anywhere where they make the model
  easier to read (for example, in complex traits defined on a single line).


.. _smithy-idl-abnf:

---------------
Smithy IDL ABNF
---------------

The Smithy IDL is defined by the following ABNF which uses case-sensitive
string support defined in :rfc:`7405`.

.. productionlist:: smithy
    idl:[`WS`] `ControlSection` `MetadataSection` `ShapeSection`

.. rubric:: Whitespace

.. productionlist:: smithy
    WS   :1*(`SP` / `NL` / `Comment` / `Comma`) ; whitespace
    Comma:","
    SP   :1*(%x20 / %x09) ; one or more spaces or tabs
    NL   :%x0A / %x0D.0A ; Newline: \n and \r\n
    NotNL:%x09 / %x20-10FFFF ; Any character except newline
    BR   :[`SP`] 1*(`Comment` / `NL`) [`WS`]; line break followed by whitespace

.. rubric:: Comments

.. productionlist:: smithy
    Comment              :`DocumentationComment` / `LineComment`
    DocumentationComment :"///" *`NotNL` `NL`
    LineComment          :"//" [(%x09 / %x20-2E / %x30-10FFF) *`NotNL`] `NL`
                         :  ; First character after "//" can't be "/"

.. rubric:: Control

.. productionlist:: smithy
    ControlSection   :*(`ControlStatement`)
    ControlStatement :"$" `NodeObjectKey` [`SP`] ":" [`SP`] `NodeValue` `BR`

.. rubric:: Metadata

.. productionlist:: smithy
    MetadataSection   :*(`MetadataStatement`)
    MetadataStatement :%s"metadata" `SP` `NodeObjectKey` [`SP`] "=" [`SP`] `NodeValue` `BR`

.. rubric:: Node values

.. productionlist:: smithy
    NodeValue           :`NodeArray`
                        :/ `NodeObject`
                        :/ `Number`
                        :/ `NodeKeyword`
                        :/ `NodeStringValue`
    NodeArray           :"[" [`WS`] *(`NodeValue` [`WS`]) "]"
    NodeObject          :"{" [`WS`] [`NodeObjectKvp` *(`WS` `NodeObjectKvp`)] [`WS`] "}"
    NodeObjectKvp       :`NodeObjectKey` [`WS`] ":" [`WS`] `NodeValue`
    NodeObjectKey       :`QuotedText` / `Identifier`
    Number              :[`Minus`] `Int` [`Frac`] [`Exp`]
    DecimalPoint        :%x2E ; .
    DigitOneToNine      :%x31-39 ; 1-9
    E                   :%x65 / %x45 ; e E
    Exp                 :`E` [`Minus` / `Plus`] 1*DIGIT
    Frac                :`DecimalPoint` 1*DIGIT
    Int                 :`Zero` / (`DigitOneToNine` *DIGIT)
    Minus               :%x2D ; -
    Plus                :%x2B ; +
    Zero                :%x30 ; 0
    NodeKeyword         :%s"true" / %s"false" / %s"null"
    NodeStringValue     :`ShapeId` / `TextBlock` / `QuotedText`
    QuotedText          :DQUOTE *`QuotedChar` DQUOTE
    QuotedChar          :%x09        ; tab
                        :/ %x20-21     ; space - "!"
                        :/ %x23-5B     ; "#" - "["
                        :/ %x5D-10FFFF ; "]"+
                        :/ `EscapedChar`
                        :/ `NL`
    EscapedChar         :`Escape` (`Escape` / DQUOTE / %s"b" / %s"f"
                        :           / %s"n" / %s"r" / %s"t" / "/"
                        :           / `UnicodeEscape`)
    UnicodeEscape       :%s"u" `Hex` `Hex` `Hex` `Hex`
    Hex                 :DIGIT / %x41-46 / %x61-66
    Escape              :%x5C ; backslash
    TextBlock           :`ThreeDquotes` [`SP`] `NL` *`TextBlockContent` `ThreeDquotes`
    TextBlockContent    :`QuotedChar` / (1*2DQUOTE 1*`QuotedChar`)
    ThreeDquotes        :DQUOTE DQUOTE DQUOTE

.. rubric:: Shapes

.. productionlist:: smithy
    ShapeSection            :[`NamespaceStatement` `UseSection` [`ShapeStatements`]]
    NamespaceStatement      :%s"namespace" `SP` `Namespace` `BR`
    UseSection              :*(`UseStatement`)
    UseStatement            :%s"use" `SP` `AbsoluteRootShapeId` `BR`
    ShapeStatements         :`ShapeOrApplyStatement` *(`BR` `ShapeOrApplyStatement`)
    ShapeOrApplyStatement   :`ShapeStatement` / `ApplyStatement`
    ShapeStatement          :`TraitStatements` `Shape`
    Shape                   :`SimpleShape`
                            :/ `EnumShape`
                            :/ `AggregateShape`
                            :/ `EntityShape`
                            :/ `OperationShape`
    SimpleShape             :`SimpleTypeName` `SP` `Identifier` [`Mixins`]
    SimpleTypeName          :%s"blob" / %s"boolean" / %s"document" / %s"string"
                            :/ %s"byte" / %s"short" / %s"integer" / %s"long"
                            :/ %s"float" / %s"double" / %s"bigInteger"
                            :/ %s"bigDecimal" / %s"timestamp"
    Mixins                  :[`SP`] %s"with" [`WS`] "[" [`WS`] 1*(`ShapeId` [`WS`]) "]"
    EnumShape               :`EnumTypeName` `SP` `Identifier` [`Mixins`] [`WS`] `EnumShapeMembers`
    EnumTypeName            :%s"enum" / %s"intEnum"
    EnumShapeMembers        :"{" [`WS`] 1*(`EnumShapeMember` [`WS`]) "}"
    EnumShapeMember         :`TraitStatements` `Identifier` [`ValueAssignment`]
    ValueAssignment         :[`SP`] "=" [`SP`] `NodeValue` [`SP`] [`Comma`] `BR`
    AggregateShape          :`AggregateTypeName` `SP` `Identifier` [`ForResource`] [`Mixins`]
                            :   [`WS`] `ShapeMembers`
    AggregateTypeName       :%s"list" / %s"map" / %s"union" / %s"structure"
    ForResource             :`SP` %s"for" `SP` `ShapeId`
    ShapeMembers            :"{" [`WS`] *(`ShapeMember` [`WS`]) "}"
    ShapeMember             :`TraitStatements` (`ExplicitShapeMember` / `ElidedShapeMember`)
                            :   [`ValueAssignment`]
    ExplicitShapeMember     :`Identifier` [`SP`] ":" [`SP`] `ShapeId`
    ElidedShapeMember       :"$" `Identifier`
    EntityShape             :`EntityTypeName` `SP` `Identifier` [`Mixins`] [`WS`] `NodeObject`
    EntityTypeName          :%s"service" / %s"resource"
    OperationShape          :%s"operation" `SP` `Identifier` [`Mixins`] [`WS`] `OperationBody`
    OperationBody           :"{" [`WS`] *(`OperationProperty` [`WS`]) "}"
    OperationProperty       :`OperationInput` / `OperationOutput` / `OperationErrors`
    OperationInput          :%s"input" [`WS`] (`InlineAggregateShape` / (":" [`WS`] `ShapeId`))
    OperationOutput         :%s"output" [`WS`] (`InlineAggregateShape` / (":" [`WS`] `ShapeId`))
    OperationErrors         :%s"errors" [`WS`] ":" [`WS`] "[" [`WS`] *(`ShapeId` [`WS`]) "]"
    InlineAggregateShape    :":=" [`WS`] `TraitStatements` [`ForResource`] [`Mixins`]
                            :   [`WS`] `ShapeMembers`

.. rubric:: Traits

.. productionlist:: smithy
    TraitStatements         :*(`Trait` [`WS`])
    Trait                   :"@" `ShapeId` [`TraitBody`]
    TraitBody               :"(" [`WS`] [`TraitStructure` / `TraitNode`] ")"
    TraitStructure          :1*(`NodeObjectKvp` [`WS`])
    TraitNode               :`NodeValue` [`WS`]
    ApplyStatement          :`ApplyStatementSingular` / `ApplyStatementBlock`
    ApplyStatementSingular  :%s"apply" `SP` `ShapeId` `WS` `Trait`
    ApplyStatementBlock     :%s"apply" `SP` `ShapeId` `WS` "{" [`WS`] `TraitStatements` "}"

.. rubric:: Shape ID

.. seealso::

    Refer to :ref:`shape-id` for the ABNF grammar of shape IDs.


.. _comments:

--------
Comments
--------

A :token:`comment <smithy:Comment>` can appear at any place between tokens where
whitespace (:token:`smithy:WS`) can appear. Comments in Smithy are defined using two
forward slashes followed by any character. A newline terminates a comment.

.. code-block:: smithy

    $version: "2"

    // This is a comment
    namespace com.foo // This is also a comment

    // Another comment
    string MyString

.. note::

    Three forward slashes can be used to define the documentation of a shape
    using a special :ref:`documentation comment <documentation-comment>`.


.. _control-statement:

---------------
Control section
---------------

The :token:`control section <smithy:ControlSection>` of a model contains
:token:`control statements <smithy:ControlStatement>` that apply parser directives
to a *specific IDL file*. Because control statements influence parsing, they
MUST appear at the beginning of a file before any other statements and have
no effect on the :ref:`semantic model <semantic-model>`.

The following control statements are currently supported:

.. list-table::
    :header-rows: 1
    :widths: 10 10 80

    * - Name
      - Type
      - Description
    * - version
      - string
      - Defines the :ref:`version <smithy-version>` of the Smithy IDL used in
        the model file.
    * - operationInputSuffix
      - string
      - Defines the suffix used when generating names for
        :ref:`inline operation input <idl-inline-input-output>`.
    * - operationOutputSuffix
      - string
      - Defines the suffix used when generating names for
        :ref:`inline operation output <idl-inline-input-output>`.

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

The following example sets the version to ``2``, meaning that tooling MUST
support a version greater than or equal to ``2.0`` and less than ``3.0``:

.. tab:: Smithy

    .. code-block:: smithy

        $version: "2"

.. tab:: JSON

    .. code-block:: json

        {
            "smithy": "2"
        }

A minor version SHOULD be provided when a model depends on a feature released
in a minor update of the specification. The following example sets the
version requirement of a file to ``2.1``, meaning that tooling MUST support a
version greater than or equal to ``2.1`` and less than ``3.0``:

.. tab:: Smithy

    .. code-block:: smithy

        $version: "2.1"

.. tab:: JSON

    .. code-block:: json

        {
            "smithy": "2.1"
        }

.. rubric:: Version compatibility

A single version statement can appear in a model file, but different versions
MAY be encountered when merging multiple model files together. Multiple
versions are supported if and only if all of the version statements are
supported by the tool loading the models.


.. _metadata-section:

----------------
Metadata section
----------------

The :token:`metadata section <smithy:MetadataSection>` is used to apply untyped
:ref:`metadata <metadata>` to the entire model. A :token:`smithy:MetadataStatement`
consists of the metadata key to set, followed by ``=``, followed by the
:token:`node value <smithy:NodeValue>` to assign to the key.

The following example defines metadata in the model:

.. tab:: Smithy

    .. code-block:: smithy

        $version: "2"
        metadata greeting = "hello"
        metadata "stringList" = ["a", "b", "c"]

.. tab:: JSON

    .. code-block:: json

        {
            "smithy": "2",
            "metadata": {
                "greeting": "hello",
                "stringList": ["a", "b", "c"]
            }
        }

Metadata is not defined within a namespace. Unquoted object property values
are considered :ref:`syntactic shape IDs <syntactic-shape-ids>` and resolve
to the prelude namespace, ``smithy.api``.

The following Smithy IDL model:

.. code-block:: smithy

    $version: "2"
    metadata exampleSyntacticShapeId = required

Is equivalent to the following JSON AST model:

.. code-block:: json

    {
        "smithy": "2",
        "metadata": {
            "exampleSyntacticShapeId": "smithy.api#required"
        }
    }


-------------
Shape section
-------------

The :token:`shape section <smithy:ShapeSection>` of the IDL is used to define
shapes and apply traits to shapes.


.. _namespaces:

Namespaces
==========

Shapes can only be defined after a namespace is declared. A namespace is
declared using a :token:`namespace statement <smithy:NamespaceStatement>`. Only
one namespace can appear per file.

The following example defines a string shape named ``MyString`` in the
``smithy.example`` namespace:

.. tab:: Smithy

    .. code-block:: smithy

        $version: "2"
        namespace smithy.example

        string MyString

.. tab:: JSON

    .. code-block:: json

        {
            "smithy": "2",
            "shapes": {
                "smithy.example#MyString": {
                    "type": "string"
                }
            }
        }


.. _use-statement:

Referring to shapes
===================

The :token:`use section <smithy:UseSection>` of the IDL is used to import shapes
into the current namespace so that they can be referred to using a
:ref:`relative shape ID <relative-shape-id>`. The :token:`UseStatement <smithy:UseStatement>`\s
that make up this section have no effect on the :ref:`semantic model <semantic-model>`.

The following example uses ``smithy.example#Foo`` and ``smithy.example#Baz``
so that they can be referred to using only ``Foo`` and ``Baz``.

.. code-block:: smithy

    $version: "2"
    namespace smithy.hello

    use smithy.example#Foo
    use smithy.example#Baz

    map MyMap {
        // Resolves to smithy.example#Foo
        key: Foo
        // Resolves to smithy.example#Baz
        value: Baz
    }

A use statement can refer to :ref:`traits <traits>` too. The following example
uses the ``smithy.example#test`` and ``smithy.example#anotherTrait``
traits so that they can be applied using relative shape IDs:

.. code-block:: smithy

    $version: "2"
    namespace smithy.hello

    use smithy.example#test
    use smithy.example#anotherTrait

    @test // <-- Resolves to smithy.example#test
    string MyString

.. rubric:: Use statement validation

#. A shape cannot be defined in a file with the same name as one of the
   shapes imported with a ``use`` statement.
#. Shapes IDs with members names cannot be imported with a use statement.


.. _relative-shape-id:

Relative shape ID resolution
----------------------------

Relative shape IDs are resolved using the following process:

#. If a :token:`smithy:UseStatement` has imported a shape with the same name,
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

    $version: "2"
    namespace smithy.example

    use foo.baz#Bar

    string MyString

    structure MyStructure {
        // Resolves to smithy.example#MyString
        // There is a shape named MyString defined in the same namespace.
        a: MyString

        // Resolves to smithy.example#MyString
        // Absolute shape IDs do not perform namespace resolution.
        b: smithy.example#MyString

        // Resolves to foo.baz#Bar
        // The "use foo.baz#Bar" statement imported the Bar symbol,
        // allowing the shape to be referenced using a relative shape ID.
        c: Bar

        // Resolves to smithy.api#String
        // No shape named String was imported through a use statement
        // the smithy.example namespace does not contain a shape named
        // String, and the prelude model contains a shape named String.
        d: String

        // Resolves to smithy.example#MyBoolean.
        // There is a shape named MyBoolean defined in the same namespace.
        // Forward references are supported both within the same file and
        // across multiple files.
        e: MyBoolean

        // Resolves to smithy.example#InvalidShape. A shape by this name has
        // not been imported through a use statement, a shape by this name
        // does not exist in the current namespace, and a shape by this name
        // does not exist in the prelude model.
        f: InvalidShape
    }

    boolean MyBoolean


.. _syntactic-shape-ids:

Syntactic shape IDs
-------------------

Unquoted string values that are not object keys in the Smithy IDL are
considered lexical shape IDs and are resolved to absolute shape IDs using the
process defined in :ref:`relative-shape-id`.

The following model defines a list that references a string shape defined
in another namespace.

.. code-block:: smithy

    $version: "2"
    namespace smithy.example

    use smithy.other#MyString

    list MyList {
        member: MyString
    }

The above model is equivalent to the following JSON AST model:

.. code-block:: json

    {
        "smithy": "2",
        "shapes": {
            "smithy.example#MyList": {
                "type": "list",
                "members": {
                    "target": "smithy.other#MyString"
                }
            }
        }
    }

.. rubric:: Use quotes for literal strings

Values that are not meant to be shape IDs MUST be quoted. The following
model is syntactically valid but semantically incorrect because
it resolves the value of the :ref:`error-trait` to the shape ID
``"smithy.example#client"`` rather than using the string literal value of
``"client"``:

.. code-block:: smithy

    $version: "2"
    namespace smithy.example

    @error(client) // <-- This MUST be "client"
    structure Error

    string client

The above example is equivalent to the following incorrect JSON AST:

.. code-block:: json

    {
        "smithy": "2",
        "shapes": {
            "smithy.example#Error": {
                "type": "structure",
                "traits": {
                    "smithy.api#error": "smithy.example#client"
                }
            },
            "smithy.example#client": {
                "type": "string"
            }
        }
    }

.. rubric:: Object keys

Object keys are not treated as shape IDs. The following example defines a
:ref:`metadata <metadata-section>` object, and when loaded into the
:ref:`semantic model <semantic-model>`, the object key ``String`` remains
the same literal string value of ``String`` while the value is treated as
a shape ID and resolves to the string literal ``"smithy.api#String"``.

.. code-block:: smithy

    metadata foo = {
        String: String,
    }

The above example is equivalent to the following JSON AST:

.. code-block:: json

    {
        "smithy": "2",
        "metadata": {
            "String": "smithy.api#String"
        }
    }

.. rubric:: Semantic model

Syntactic shape IDs are syntactic sugar for defining fully-qualified
shape IDs inside of strings, and this difference is inconsequential in the
:ref:`semantic model <semantic-model>`. A syntactic shape ID SHOULD be
resolved to a string that contains a fully-qualified shape ID when parsing
the model.

.. rubric:: Validation

When a syntactic shape ID is found that does not target an actual shape in
the fully loaded semantic model, an implementation SHOULD emit a DANGER
:ref:`validation event <validation>` with an ID of `SyntacticShapeIdTarget`.
This validation brings attention to the broken reference and helps to ensure
that modelers do not unintentionally use a syntactic shape ID when they should
have used a string. A DANGER severity is used so that the validation can be
:ref:`suppressed <suppression-definition>` in the rare cases that the broken
reference can be ignored.


Defining shapes
===============

Shapes are defined using a :token:`smithy:ShapeStatement`.


.. _idl-simple:

Simple shapes
-------------

:ref:`Simple shapes <simple-types>` are defined using a
:token:`smithy:SimpleShape`.

The following example defines a ``string`` shape:

.. tab:: Smithy

    .. code-block:: smithy

        $version: "2"
        namespace smithy.example

        string MyString

.. tab:: JSON

    .. code-block:: json

        {
            "smithy": "2",
            "shapes": {
                "smithy.example#String": {
                    "type": "string"
                }
            }
        }

The following example defines an ``integer`` shape with a :ref:`range-trait`:

.. tab:: Smithy

    .. code-block:: smithy

        $version: "2"
        namespace smithy.example

        @range(min: 0, max: 1000)
        integer MaxResults

.. tab:: JSON

    .. code-block:: json

        {
            "smithy": "2",
            "shapes": {
                "smithy.example#MaxResults": {
                    "type": "integer",
                    "traits": {
                        "smithy.api#range": {
                            "min": 0,
                            "max": 100
                        }
                    }
                }
            }
        }


.. _idl-enum:

Enum shapes
-----------

The :ref:`enum` shape is defined using an :token:`smithy:EnumShape`.

The following example defines an :ref:`enum` shape:

.. code-block:: smithy

    $version: "2"
    namespace smithy.example

    enum Suit {
        DIAMOND
        CLUB
        HEART
        SPADE
    }

Syntactic sugar can be used to assign an :ref:`enumvalue-trait` to an enum
member. The following example defines an enum shape with custom values and
traits:

.. code-block:: smithy

    $version: "2"
    namespace smithy.example

    enum Suit {
        @deprecated
        DIAMOND = "diamond"

        CLUB = "club"
        HEART = "heart"
        SPADE = "spade"
    }

The above enum is exactly equivalent to the following enum:

.. code-block:: smithy

    $version: "2"
    namespace smithy.example

    enum Suit {
        @deprecated
        @enumValue("diamond")
        DIAMOND

        @enumValue("club")
        CLUB

        @enumValue("heart")
        HEART

        @enumValue("spade")
        SPADE
    }


.. _idl-int-enum:

IntEnum shapes
--------------

The :ref:`intEnum` shape is defined using an
:token:`smithy:EnumShape`.

.. note::
    The :ref:`enumValue trait <enumValue-trait>` is required on all
    :ref:`intEnum` members.

Syntactic sugar can be used to assign an :ref:`enumvalue-trait` to an intEnum
member. The following example defines an :ref:`intEnum` shape:

.. code-block:: smithy

    $version: "2"
    namespace smithy.example

    intEnum Suit {
        DIAMOND = 1
        CLUB = 2
        HEART = 3
        SPADE = 4
    }

The above intEnum is exactly equivalent to the following intEnum:

.. code-block:: smithy

    $version: "2"
    namespace smithy.example

    intEnum Suit {
        @enumValue(1)
        DIAMOND

        @enumValue(2)
        CLUB

        @enumValue(3)
        HEART

        @enumValue(4)
        SPADE
    }


.. _idl-list:

List shapes
-----------

A :ref:`list <list>` shape is defined using a :token:`smithy:AggregateShape`.

The following example defines a list with a string member from the
:ref:`prelude <prelude>`:

.. tab:: Smithy

    .. code-block:: smithy

        $version: "2"
        namespace smithy.example

        list MyList {
            member: String
        }

.. tab:: JSON

    .. code-block:: json

        {
            "smithy": "2",
            "shapes": {
                "smithy.example#MyList": {
                    "type": "list",
                    "member": {
                        "target": "smithy.api#String"
                    }
                }
            }
        }

Traits can be applied to the list shape and its member:

.. tab:: Smithy

    .. code-block:: smithy

        $version: "2"
        namespace smithy.example

        @length(min: 3, max: 10)
        list MyList {
            @length(min: 1, max: 100)
            member: String
        }

.. tab:: JSON

    .. code-block:: json

        {
            "smithy": "2",
            "shapes": {
                "smithy.example#MyList": {
                    "type": "list",
                    "member": {
                        "target": "smithy.api#String",
                        "traits": {
                            "smithy.api#length": {
                                "min": 1,
                                "max": 100
                            }
                        }
                    },
                    "traits": {
                        "smithy.api#length": {
                            "min": 3,
                            "max": 10
                        }
                    }
                }
            }
        }


.. _idl-map:

Map shapes
----------

A :ref:`map <map>` shape is defined using a :token:`smithy:AggregateShape`.

The following example defines a map of strings to integers:

.. tab:: Smithy

    .. code-block:: smithy

        $version: "2"
        namespace smithy.example

        map IntegerMap {
            key: String,
            value: Integer
        }

.. tab:: JSON

    .. code-block:: json

        {
            "smithy": "2",
            "shapes": {
                "type": "map",
                "smithy.example#IntegerMap": {
                    "key": {
                        "target": "smithy.api#String"
                    },
                    "value": {
                        "target": "smithy.api#String"
                    }
                }
            }
        }

Traits can be applied to the map shape and its members:

.. tab:: Smithy

    .. code-block:: smithy

        $version: "2"
        namespace smithy.example

        @length(min: 0, max: 100)
        map IntegerMap {
            @length(min: 1, max: 10)
            key: String,

            @range(min: 1, max: 1000)
            value: Integer
        }

.. tab:: JSON

    .. code-block:: json

        {
            "smithy": "2",
            "shapes": {
                "smithy.example#IntegerMap": {
                    "type": "map",
                    "key": {
                        "target": "smithy.api#String",
                        "traits": {
                            "smithy.api#length": {
                                "min": 1,
                                "max": 10
                            }
                        }
                    },
                    "value": {
                        "target": "smithy.api#Integer",
                        "traits": {
                            "smithy.api#range": {
                                "min": 1,
                                "max": 1000
                            }
                        }
                    },
                    "traits": {
                        "smithy.api#length": {
                            "min": 0,
                            "max": 100
                        }
                    }
                }
            }
        }


.. _idl-structure:

Structure shapes
----------------

A :ref:`structure <structure>` shape is defined using a
:token:`smithy:AggregateShape`.

The following example defines a structure with two members:

.. tab:: Smithy

    .. code-block:: smithy

        $version: "2"
        namespace smithy.example

        structure MyStructure {
            foo: String
            baz: Integer
        }

.. tab:: JSON

    .. code-block:: json

        {
            "smithy": "2",
            "shapes": {
                "smithy.example#MyStructure": {
                    "type": "structure",
                    "members": {
                        "foo": {
                            "target": "smithy.api#String"
                        },
                        "baz": {
                            "target": "smithy.api#Integer"
                        }
                    }
                }
            }
        }

Traits can be applied to structure members:

.. tab:: Smithy

    .. code-block:: smithy

        $version: "2"
        namespace smithy.example

        /// This is MyStructure.
        structure MyStructure {
            /// This is documentation for `foo`.
            @required
            foo: String

            /// This is documentation for `baz`.
            @deprecated
            baz: Integer
        }

.. tab:: JSON

    .. code-block:: json

        {
            "smithy": "2",
            "shapes": {
                "smithy.example#MyStructure": {
                    "type": "structure",
                    "members": {
                        "foo": {
                            "target": "smithy.api#String",
                            "traits": {
                                "smithy.api#documentation": "This is documentation for `foo`.",
                                "smithy.api#required": {}
                            }
                        },
                        "baz": {
                            "target": "smithy.api#Integer",
                            "traits": {
                                "smithy.api#documentation": "This is documentation for `baz`.",
                                "smithy.api#deprecated": {}
                            }
                        }
                    },
                    "traits": {
                        "smithy.api#documentation": "This is MyStructure."
                    }
                }
            }
        }

Syntactic sugar can be used to apply the :ref:`default-trait` to a structure
member. The following example:

.. code-block:: smithy

    structure Example {
        normative: Boolean = true
    }

Is exactly equivalent to:

.. code-block:: smithy

    structure Example {
        @default(true)
        normative: Boolean
    }


.. _idl-union:

Union shapes
------------

A :ref:`union <union>` shape is defined using a :token:`smithy:AggregateShape`.

The following example defines a union shape with several members:

.. tab:: Smithy

    .. code-block:: smithy

        $version: "2"
        namespace smithy.example

        union MyUnion {
            i32: Integer

            @length(min: 1, max: 100)
            string: String

            time: Timestamp
        }

.. tab:: JSON

    .. code-block:: json

        {
            "smithy": "2",
            "shapes": {
                "smithy.example#MyUnion": {
                    "type": "union",
                    "members": {
                        "i32": {
                            "target": "smithy.api#Integer"
                        },
                        "string": {
                            "target": "smithy.api#String",
                            "smithy.api#length": {
                                "min": 1,
                                "max": 100
                            }
                        },
                        "time": {
                            "target": "smithy.api#Timestamp"
                        }
                    }
                }
            }
        }


.. _idl-service:

Service shape
-------------

A service shape is defined using a :token:`smithy:EntityShape` and the provided
:token:`smithy:NodeObject` supports the same properties defined in the
:ref:`service specification <service>`.

The following example defines a service named ``ModelRepository`` that binds
a resource named ``Model`` and an operation named ``PingService``:

.. tab:: Smithy

    .. code-block:: smithy

        $version: "2"
        namespace smithy.example

        service ModelRepository {
            version: "2020-07-13",
            resources: [Model],
            operations: [PingService]
        }

.. tab:: JSON

    .. code-block:: json

        {
            "smithy": "2",
            "shapes": {
                "smithy.example#ModelRepository": {
                    "type": "service",
                    "resources": [
                        {
                            "target": "smithy.example#Model"
                        }
                    ],
                    "operations": [
                        {
                            "target": "smithy.example#PingService"
                        }
                    ]
                }
            }
        }


.. _idl-operation:

Operation shape
---------------

An operation shape is defined using an :token:`smithy:OperationShape` and
the same properties defined in the :ref:`operation specification <operation>`.

The following example defines an operation shape that accepts an input
structure named ``Input``, returns an output structure named ``Output``, and
can potentially return the ``Unavailable`` or ``BadRequest``
:ref:`error structures <error-trait>`.

.. tab:: Smithy

    .. code-block:: smithy

        $version: "2"
        namespace smithy.example

        operation PingService {
            input: PingServiceInput,
            output: PingServiceOutput,
            errors: [UnavailableError, BadRequestError]
        }

.. tab:: JSON

    .. code-block:: json

        {
            "smithy": "2",
            "shapes": {
                "smithy.example#PingService": {
                    "type": "operation",
                    "input": {
                        "target": "smithy.example#PingServiceInput"
                    },
                    "output": {
                        "target": "smithy.example#PingServiceOutput"
                    },
                    "errors": [
                        {
                            "target": "smithy.example#UnavailableError"
                        },
                        {
                            "target": "smithy.example#BadRequestError"
                        }
                    ]
                }
            }
        }


.. _idl-inline-input-output:

Inline input / output shapes
++++++++++++++++++++++++++++

The input and output properties of operations can be defined using a more
succinct, inline syntax.

A structure defined using inline syntax is automatically marked with the
:ref:`input-trait` for inputs and the :ref:`output-trait` for outputs.

A structure defined using inline syntax is given a generated shape name. For
inputs, the generated name is the name of the operation shape with the suffix
``Input`` added. For outputs, the generated name is the name of the operation
shape with the ``Output`` suffix added.

For example, the following model:

.. code-block:: smithy

    operation GetUser {
        // The generated shape name is GetUserInput
        input := {
            userId: String
        }

        // The generated shape name is GetUserOutput
        output := {
            username: String
            userId: String
        }
    }

Is equivalent to:

.. code-block:: smithy

    operation GetUser {
        input: GetUserInput
        output: GetUserOutput
    }

    @input
    structure GetUserInput {
        userId: String
    }

    @output
    structure GetUserOutput {
        username: String
        userId: String
    }

Traits and mixins can be applied to the inline structure:

.. code-block:: smithy

    @mixin
    structure BaseUser {
        userId: String
    }

    operation GetUser {
        input := @references([{resource: User}]) {
            userId: String
        }

        output := with [BaseUser] {
            username: String
        }
    }

    operation PutUser {
        input :=
            @references([{resource: User}])
            with [BaseUser] {}
    }

The suffixes for the generated names can be customized using the
``operationInputSuffix`` and ``operationOutputSuffix`` control statements.

.. code-block:: smithy

    $version: "2"
    $operationInputSuffix: "Request"
    $operationOutputSuffix: "Response"

    namespace smithy.example

    operation GetUser {
        // The generated shape name is GetUserRequest
        input := {
            userId: String
        }

        // The generated shape name is GetUserResponse
        output := {
            username: String
            userId: String
        }
    }


.. _idl-resource:

Resource shape
--------------

A resource shape is defined using a :token:`smithy:EntityShape` and the
provided :token:`smithy:NodeObject` supports the same properties defined in the
:ref:`resource specification <resource>`.

The following example defines a resource shape that has a single identifier,
and defines a :ref:`read <read-lifecycle>` operation:

.. tab:: Smithy

    .. code-block:: smithy

        $version: "2"
        namespace smithy.example

        resource SprocketResource {
            identifiers: {
                sprocketId: String,
            },
            read: GetSprocket,
        }

.. tab:: JSON

    .. code-block:: json

        {
            "smithy": "2",
            "shapes": {
                "smithy.example#Sprocket": {
                    "type": "resource",
                    "identifiers": {
                        "sprocketId": {
                            "target": "smithy.api#String"
                        }
                    },
                    "read": {
                        "target": "smithy.example#SprocketResource"
                    }
                }
            }
        }

.. seealso::

    The :ref:`target elision syntax <idl-target-elision>` for an easy way to
    define structures that reference resource identifiers without having to
    repeat the target definition.

.. _idl-mixins:

Mixins
------

:ref:`Mixins <mixins>` can be added to a shape using the optional
:token:`smithy:Mixins` clause of a shape definition.

For example:

.. code-block:: smithy

    @mixin
    structure BaseUser {
        userId: String
    }

    structure UserDetails with [BaseUser] {
        username: String
    }

    @mixin
    @sensitive
    string SensitiveString

    @pattern("^[a-zA-Z\.]*$")
    string SensitiveText with [SensitiveString]


.. _idl-target-elision:

Target Elision
--------------

Having to completely redefine a :ref:`resource identifier <resource-identifiers>`
to use it in a structure or redefine a member from a :ref:`mixin <mixins>` to add
additional traits can be cumbersome and potentially error-prone. Target elision
syntax can be used to cut down on that repetition by prefixing the member name
with a ``$``. If a member is prefixed this way, its target will automatically be
set to the target of a mixin member with the same name. The following example
shows how to elide the target for a member inherited from a mixin:

.. code-block:: smithy

    $version: "2"
    namespace smithy.example

    @mixin
    structure IdBearer {
        id: String
    }

    structure IdRequired with [IdBearer] {
        @required
        $id
    }

Additionally, structure shapes can reference a :ref:`resource <idl-resource>`
shape to define members that represent the resource's identifiers without having
to redefine the target shape. In addition to prefixing a member with ``$``, the
structure must also add ``for`` followed by the resource referenced in
the shape's definition before any mixins are specified.

To resolve elided types, first check if any bound resource defines an
identifier that case-sensitively matches the elided member name. If a match is
found, the type targeted by that identifier is used for the elided type. If no
identifier matches the elided member name, mixin members are case-sensitively
checked, and if a match is found, the type targeted by the mixin member is
used as the elided type. It is an error if neither the resource or mixin
members matches an elided member name.

The following example shows a structure reusing an identifier definition from
a resource:

.. code-block:: smithy

    $version: "2"
    namespace smithy.example

    resource User {
        identifiers: {
            name: String
            uuid: String
        }
    }

    structure UserSummary for User {
        $name
        age: Short
    }

Note that the ``UserSummary`` structure does not attempt to define the
``uuid`` identifier. When referencing a resource in this way, only the
identifiers that are explicitly referenced are added to the structure. This
allows structures to define subsets of identifiers, which can be useful for
operations like create operations where some of those identifiers may be
generated by the service.

Structures may only reference one resource shape in this way.

When using both mixins and a resource reference, the referenced resource will
be checked first. The following example is invalid:

.. code-block:: smithy

    $version: "2"
    namespace smithy.example

    resource User {
        identifiers: {
            uuid: String
        }
    }

    @mixin
    structure UserIdentifiers {
        uuid: Blob
    }

    // This is invalid because the `uuid` member's target is set to
    // String, which then conflicts with the UserIdentifiers mixin.
    structure UserSummary for User with [UserIdentifiers] {
        $uuid
    }


.. _documentation-comment:

Documentation comment
=====================

:token:`Documentation comments <smithy:DocumentationComment>` are a
special kind of :token:`smithy:Comment` that provide
:ref:`documentation <documentation-trait>` for shapes. A documentation
comment is formed when three forward slashes (``"///"``) appear as the
first non-whitespace characters on a line.

Documentation comments are defined using CommonMark_. The text after the
forward slashes is considered the contents of the line. If the text starts
with a space (" "), the leading space is removed from the content.
Successive documentation comments are combined together using a newline
("\\n") to form the documentation of a shape.

The following Smithy IDL example,

.. code-block:: smithy

    $version: "2"
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
        "smithy": "2",
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
                    "smithy.api#documentation": "This is documentation about a trait shape.\n  More docs here."
                }
            }
        }
    }

.. rubric:: Placement

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
        @required
        foo: String,
    }

.. rubric:: Semantic model

Documentation comments are syntactic sugar equivalent to applying the
:ref:`documentation-trait`, and this difference is inconsequential
in the :ref:`semantic model <semantic-model>`.


.. _idl-applying-traits:

Applying traits
===============

Trait values immediately preceding a shape definition are applied to the
shape. The shape ID of a trait is *resolved* against :token:`smithy:UseStatement`\s
and the current namespace in exactly the same way as
:ref:`other shape IDs <relative-shape-id>`.

The following example applies the :ref:`length-trait` and
:ref:`documentation-trait` to ``MyString``:

.. tab:: Smithy

    .. code-block:: smithy

        $version: "2"
        namespace smithy.example

        @length(min: 1, max: 100)
        @documentation("Contains a string")
        string MyString

.. tab:: JSON

    .. code-block:: json

        {
            "smithy": "2",
            "shapes": {
                "smithy.example#MyString": {
                    "type": "string",
                    "traits": {
                        "smithy.api#documentation": "Contains a string",
                        "smithy.api#length": {
                            "min": 1,
                            "max": 100
                        }
                    }
                }
            }
        }


.. _trait-values:

Trait values
------------

The value that can be provided for a trait depends on its type. A value for a
trait is defined by enclosing the value in parenthesis. Trait values can only
appear immediately before a shape.

The following example applies various traits to a structure shape and its
members.

.. code-block:: smithy

    @documentation("An animal in the animal kingdom")
    structure Animal {
        @required
        name: smithy.api#String,

        @length(min: 0)
        @tags(["private-beta"])
        age: smithy.api#Integer,
    }


Structure, map, and union trait values
--------------------------------------

Traits that are a ``structure``, ``union``, or ``map`` are defined using
a special syntax that places key-value pairs inside of the trait
parenthesis. Wrapping braces, "{" and "}", are not permitted.

.. code-block:: smithy

    @structuredTrait(foo: "bar", baz: "bam")

Nested structure, map, and union values are defined using
:ref:`node value <node-values>` productions:

.. code-block:: smithy

    @structuredTrait(
        foo: {
            bar: "baz",
            qux: "true",
        }
    )

Omitting a value is allowed on ``list``, ``set``, ``map``, and ``structure``
traits if the shapes have no ``length`` constraints or ``required`` members.
The following applications of the ``foo`` trait are equivalent:

.. tab:: Smithy

    .. code-block:: smithy

        $version: "2"
        namespace smithy.example

        @trait
        structure foo {}

        @foo
        string MyString1

        @foo()
        string MyString2

.. tab:: JSON

    .. code-block:: json

        {
            "smithy": "2",
            "shapes": {
                "smithy.example#foo": {
                    "type": "structure",
                    "traits": {
                        "smithy.api#trait": {}
                    }
                },
                "smithy.example#MyString1": {
                    "type": "string",
                    "traits": {
                        "smithy.api#foo": {}
                    }
                },
                "smithy.example#MyString2": {
                    "type": "string",
                    "traits": {
                        "smithy.api#foo": {}
                    }
                }
            }
        }


List and set trait values
-------------------------

Traits that are a ``list`` or ``set`` shape are defined inside
of brackets (``[``) and (``]``) using a :token:`smithy:NodeArray` production.

.. code-block:: smithy

    @tags(["a", "b"])


Other trait values
------------------

All other trait values MUST adhere to the JSON type mappings defined
in :ref:`trait-node-values`.

The following example defines a string trait value:

.. code-block:: smithy

    @documentation("Hello")


.. _apply-statement:

Apply statement
---------------

Traits can be applied to shapes outside of a shape's definition using an
:token:`smithy:ApplyStatement`.

The following example applies the :ref:`documentation-trait` to the
``smithy.example#MyString`` shape:

.. tab:: Smithy

    .. code-block:: smithy

        $version: "2"
        namespace smithy.example

        apply MyString @documentation("This is my string!")

.. tab:: JSON

    .. code-block:: json

        {
            "smithy": "2",
            "shapes": {
                "smithy.example#MyString": {
                    "type": "apply",
                    "traits": {
                        "smithy.api#documentation": "This is my string!"
                    }
                }
            }
        }

Multiple traits can be applied to the same shape using a block apply
statement. The following example applies the :ref:`documentation-trait`
and :ref:`length-trait` to the ``smithy.example#MyString`` shape:

.. tab:: Smithy

    .. code-block:: smithy

        $version: "2"
        namespace smithy.example

        apply MyString {
            @documentation("This is my string!")
            @length(min: 1, max: 10)
        }

.. tab:: JSON

    .. code-block:: json

        {
            "smithy": "2",
            "shapes": {
                "smithy.example#MyString": {
                    "type": "apply",
                    "traits": {
                        "smithy.api#documentation": "This is my string!",
                        "smithy.api#length": {
                            "min": 1,
                            "max": 10
                        }
                    }
                }
            }
        }

Traits can be applied to members too:

.. code-block:: smithy

    $version: "2"
    namespace smithy.example

    apply MyStructure$foo @documentation("Structure member documentation")
    apply MyUnion$foo @documentation("Union member documentation")
    apply MyList$member @documentation("List member documentation")
    apply MySet$member @documentation("Set member documentation")
    apply MyMap$key @documentation("Map key documentation")
    apply MyMap$value @documentation("Map key documentation")

.. seealso::

    Refer to :ref:`trait conflict resolution <trait-conflict-resolution>`
    for information on how trait conflicts are resolved.

.. note::

    In the semantic model, applying traits outside of a shape definition is
    treated exactly the same as applying the trait inside of a shape
    definition.


.. _node-values:

-----------
Node values
-----------

*Node values* are analogous to JSON values. Node values are used to define
:ref:`metadata <metadata>` and :ref:`trait values <traits>`. Smithy's
node values have many advantages over JSON: comments, unquoted keys, unquoted
strings, text blocks, and trailing commas.

The following example defines a complex object metadata entry using a
node value:

.. code-block:: smithy

    metadata foo = {
        hello: 123,
        "foo": "456",
        testing: """
            Hello!
            """,
        an_array: [10.5],
        nested-object: {
            hello-there$: true
        }, // <-- Trailing comma
    }

.. rubric:: Array node

An array node is defined like a JSON array. A :token:`smithy:NodeArray` contains
zero or more heterogeneous :token:`smithy:NodeValue`\s. A trailing comma is allowed
in a ``NodeArray``.

The following examples define arrays with zero, one, and two values:

* ``[]``
* ``[true]``
* ``[1, "hello",]``

.. rubric:: Object node

An object node is defined like a JSON object. A :token:`smithy:NodeObject` contains
zero or more key value pairs of strings (a :token:`smithy:NodeObjectKey`) that map
to heterogeneous :token:`smithy:NodeValue`\s. A trailing comma is allowed
in a ``NodeObject``.

The following examples define objects with zero, one, and two key value pairs:

* ``{}``
* ``{foo: true}``
* ``{foo: "hello", "bar": [1, 2, {}]}``

.. rubric:: Number node

A node :token:`smithy:Number` contains numeric data. It is defined like a JSON
number. The following examples define several ``Number`` values:

* ``0``
* ``0.0``
* ``1234``
* ``-1234.1234``
* ``1e+2``
* ``1.0e-10``

.. rubric:: Node keywords

Several keywords are used when parsing :token:`smithy:NodeValue`.

* ``true``: The value is treated as a boolean ``true``
* ``false``: The value is treated as a boolean ``false``
* ``null``: The value is treated like a JSON ``null``


String values
=============

A ``NodeValue`` can contain :token:`smithy:NodeStringValue` productions that all
define strings.

.. rubric:: New lines

New lines in strings are normalized from CR (\u000D) and CRLF (\u000D\u000A)
to LF (\u000A). This ensures that strings defined in a Smithy model are
equivalent across platforms. If a literal ``\r`` is desired, it can be added
a string value using the Unicode escape ``\u000d``.

.. rubric:: String equivalence

The ``NodeStringValue`` production defines several productions used to
define strings, and in order for these productions to work in concert with
the :ref:`JSON AST format <json-ast>`, each of these production MUST be
treated like equivalent string values when loaded into the
:ref:`semantic model <semantic-model>`.


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

A text block is a string literal that can span multiple lines and automatically
removes any incidental whitespace. Smithy text blocks are heavily inspired by
text blocks defined in `JEP 355 <https://openjdk.java.net/jeps/355>`_.

A text block is opened with three double quotes ("""), followed by a newline,
zero or more content characters, and closed with three double quotes.
Text blocks differentiate *incidental whitespace* from *significant whitespace*.
Smithy will re-indent the content of a text block by removing all incidental
whitespace.

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

.. code-block::

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
       :class: no-copybutton

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
    ....<p>Hi\n....bar</p>
    ..</div>
    .."""

Because string escapes are expanded after incidental whitespace is removed, it
is interpreted as:

.. code-block:: none
    :class: no-copybutton

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

.. _CommonMark: https://spec.commonmark.org/
