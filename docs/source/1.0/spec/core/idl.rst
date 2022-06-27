.. _idl:

==========
Smithy IDL
==========

Smithy models are defined using either the Smithy interface definition language
(IDL) or the :ref:`JSON abstract syntax tree <json-ast>` (AST). This document
defines the ABNF_ grammar and syntax for defining models with the Smithy IDL.

.. contents:: Table of contents
    :depth: 1
    :local:
    :backlinks: none


-------------------
Smithy IDL overview
-------------------

The Smithy IDL is made up of 3, ordered sections, each of which is optional:

1. **Control section**; defines parser directives like which version of the
   IDL to use.
2. **Metadata section**; applies metadata to the entire model.
3. **Shape section**; where shapes and traits are defined. A namespace MUST
   be defined before any shapes or traits can be defined.
   :token:`smithy:use_statement`\s can be defined after a namespace and before shapes
   or traits to refer to shapes in other namespaces using a shorter name.

The following example defines a model file with each section:

.. tabs::

    .. code-tab:: smithy

            // (1) Control section
            $version: "1.0"

            // (2) Metadata section
            metadata foo = "bar"

            // (3) Shape section
            namespace smithy.example

            use smithy.other.namespace#MyString

            structure MyStructure {
                @required
                foo: MyString
            }

    .. code-tab:: json

        {
            "smithy": "1.0",
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

Smithy models MUST be encoded using UTF-8 and SHOULD use Unix style
line endings (``\n``). The Smithy ABNF is whitespace sensitive.


.. _smithy-idl-abnf:

---------------
Smithy IDL ABNF
---------------

The Smithy IDL is defined by the following ABNF:

.. productionlist:: smithy
    idl:`ws` `control_section` `metadata_section` `shape_section`

.. rubric:: Whitespace

.. productionlist:: smithy
    ws      :*(`sp` / `newline` / `comment`) ; whitespace
    sp      :*(%x20  / %x09) ; " " and \t
    br      :`sp` (`comment` / `newline`) `sp` ; break
    newline :%x0A / %x0D.0A ; \n and \r\n

.. rubric:: Comments

.. productionlist:: smithy
    comment: `documentation_comment` / `line_comment`
    documentation_comment:"///" *`not_newline` `br`
    line_comment: "//" *`not_newline` `newline`
    not_newline: %x09 / %x20-10FFFF ; Any character except newline

.. rubric:: Control

.. productionlist:: smithy
    control_section   :*(`control_statement`)
    control_statement :"$" `ws` `node_object_key` `ws` ":" `ws` `node_value` `br`

.. rubric:: Metadata

.. productionlist:: smithy
    metadata_section   :*(`metadata_statement`)
    metadata_statement :"metadata" `ws` `node_object_key` `ws` "=" `ws` `node_value` `br`

.. rubric:: Node values

.. productionlist:: smithy
    node_value :`node_array`
               :/ `node_object`
               :/ `number`
               :/ `node_keywords`
               :/ `node_string_value`
    node_array          :`empty_node_array` / `populated_node_array`
    empty_node_array    :"[" `ws` "]"
    populated_node_array:"[" `ws` `node_value` `ws`
                        :       *(`comma` `node_value` `ws`)
                        :       `trailing_comma` "]"
    trailing_comma      :[`comma`]
    comma               :"," `ws`
    node_object          :`empty_node_object` / `populated_node_object`
    empty_node_object    :"{" `ws` "}"
    populated_node_object:"{" `ws` `node_object_kvp` `ws`
                         :       *(`comma` `node_object_kvp` `ws`)
                         :       `trailing_comma` "}"
    node_object_kvp      :`node_object_key` `ws` ":" `ws` `node_value`
    node_object_key      :`quoted_text` / `identifier`
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
    node_keywords: "true" / "false" / "null"
    node_string_value   :`shape_id` / `text_block` / `quoted_text`
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

.. rubric:: Shapes

.. productionlist:: smithy
    shape_section :[`namespace_statement` [`use_section`] [`shape_statements`]]
    namespace_statement :"namespace" `ws` `namespace` `br`
    use_section   :*(`use_statement`)
    use_statement :"use" `ws` `absolute_root_shape_id` `br`
    shape_statements             :*(`shape_statement` / `apply_statement`)
    shape_statement              :`trait_statements` `shape_body` `br`
    shape_body                   :`simple_shape_statement`
                                 :/ `list_statement`
                                 :/ `set_statement`
                                 :/ `map_statement`
                                 :/ `structure_statement`
                                 :/ `union_statement`
                                 :/ `service_statement`
                                 :/ `operation_statement`
                                 :/ `resource_statement`
    simple_shape_statement :`simple_type_name` `ws` `identifier`
    simple_type_name       :"blob" / "boolean" / "document" / "string"
                           :/ "byte" / "short" / "integer" / "long"
                           :/ "float" / "double" / "bigInteger"
                           :/ "bigDecimal" / "timestamp"
    shape_members           :`empty_shape_members` / `populated_shape_members`
    empty_shape_members     :"{" `ws` "}"
    populated_shape_members :"{" `ws` `shape_member_kvp`
                            :  *(`comma` `shape_member_kvp` `ws`) `trailing_comma` "}"
    shape_member_kvp        :`trait_statements` `identifier` `ws` ":" `ws` `shape_id`
    list_statement :"list" `ws` `identifier` `ws` `shape_members`
    set_statement :"set" `ws` `identifier` `ws` `shape_members`
    map_statement :"map" `ws` `identifier` `ws` `shape_members`
    structure_statement     :"structure" `ws` `identifier` `ws` `shape_members`
    union_statement :"union" `ws` `identifier` `ws` `shape_members`
    service_statement :"service" `ws` `identifier` `ws` `node_object`
    operation_statement :"operation" `ws` `identifier` `ws` `node_object`
    resource_statement :"resource" `ws` `identifier` `ws` `node_object`

.. rubric:: Traits

.. productionlist:: smithy
    trait_statements    : *(`ws` `trait`) `ws`
    trait               :"@" `shape_id` [`trait_body`]
    trait_body          :"(" `ws` `trait_body_value` `ws` ")"
    trait_body_value    :`trait_structure` / `node_value`
    trait_structure     :`trait_structure_kvp` *(`ws` `comma` `trait_structure_kvp`)
    trait_structure_kvp :`node_object_key` `ws` ":" `ws` `node_value`
    apply_statement :"apply" `ws` `shape_id` `ws` `trait` `br`

.. rubric:: Shape ID

.. seealso::

    Refer to :ref:`shape-id` for the ABNF grammar of shape IDs.


.. _comments:

--------
Comments
--------

A :token:`comment <smithy:comment>` can appear at any place between tokens where
whitespace (:token:`smithy:ws`) can appear. Comments in Smithy are defined using two
forward slashes followed by any character. A newline terminates a comment.

.. code-block:: smithy

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

The :token:`control section <smithy:control_section>` of a model contains
:token:`control statements <smithy:control_statement>` that apply parser directives
to a *specific IDL file*. Because control statements influence parsing, they
MUST appear at the beginning of a file before any other statements and have
no effect on the :ref:`semantic model <semantic-model>`

The :ref:`version <smithy-version>` statement is currently the only control
statement defined in the Smithy IDL. Implementations MUST ignore unknown
control statements.


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


.. _metadata-section:

----------------
Metadata section
----------------

The :token:`metadata section <smithy:metadata_section>` is used to apply untyped
:ref:`metadata <metadata>` to the entire model. A :token:`smithy:metadata_statement`
consists of the metadata key to set, followed by ``=``, followed by the
:token:`node value <smithy:node_value>` to assign to the key.

The following example defines metadata in the model:

.. tabs::

    .. code-tab:: smithy

        metadata greeting = "hello"
        metadata "stringList" = ["a", "b", "c"]

    .. code-tab:: json

        {
            "smithy": "1.0",
            "metadata": {
                "greeting": "hello",
                "stringList": ["a", "b", "c"]
            }
        }


-------------
Shape section
-------------

The :token:`shape section <smithy:shape_section>` of the IDL is used to define
shapes and apply traits to shapes.


.. _namespaces:

Namespaces
==========

Shapes can only be defined after a namespace is declared. A namespace is
declared using a :token:`namespace statement <smithy:namespace_statement>`. Only
one namespace can appear per file.

The following example defines a string shape named ``MyString`` in the
``smithy.example`` namespace:

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        string MyString

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#MyString": {
                    "type": "string"
                }
            }
        }


.. _use-statement:

Referring to shapes
===================

The :token:`use section <smithy:use_section>` of the IDL is used to import shapes
into the current namespace so that they can be referred to using a
:ref:`relative shape ID <relative-shape-id>`. The :token:`use_statement <smithy:use_statement>`\s
that make up this section have no effect on the :ref:`semantic model <semantic-model>`.

The following example uses ``smithy.example#Foo`` and ``smithy.example#Baz``
so that they can be referred to using only ``Foo`` and ``Baz``.

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

A use statement can refer to :ref:`traits <traits>` too. The following example
uses the ``smithy.example#test`` and ``smithy.example#anotherTrait``
traits so that they can be applied using relative shape IDs:

.. code-block:: smithy

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

#. If a :token:`smithy:use_statement` has imported a shape with the same name,
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

        // Resolves to smithy.api#String
        // No shape named String was imported through a use statement
        // the smithy.example namespace does not contain a shape named
        // String, and the prelude model contains a shape named String.
        d: String,

        // Resolves to smithy.example#MyBoolean.
        // There is a shape named MyBoolean defined in the same namespace.
        // Forward references are supported both within the same file and
        // across multiple files.
        e: MyBoolean,

        // Resolves to smithy.example#InvalidShape. A shape by this name has
        // not been imported through a use statement, a shape by this name
        // does not exist in the current namespace, and a shape by this name
        // does not exist in the prelude model.
        f: InvalidShape,
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

    namespace smithy.example

    use smithy.other#MyString

    list MyList {
        member: MyString
    }

The above model is equivalent to the following JSON AST model:

.. code-block:: json

    {
        "smithy": "1.0",
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

    namespace smithy.example

    @error(client) // <-- This MUST be "client"
    structure Error

    string client

The above example is equivalent to the following incorrect JSON AST:

.. code-block:: json

    {
        "smithy": "1.0",
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
        "smithy": "1.0",
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

Shapes are defined using a :token:`smithy:shape_statement`.


.. _idl-simple:

Simple shapes
-------------

:ref:`Simple shapes <simple-types>` are defined using a
:token:`smithy:simple_shape_statement`.

The following example defines a ``string`` shape:

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        string MyString

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#String": {
                    "type": "string"
                }
            }
        }

The following example defines an ``integer`` shape with a :ref:`range-trait`:

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        @range(min: 0, max: 1000)
        integer MaxResults

    .. code-tab:: json

        {
            "smithy": "1.0",
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


.. _idl-list:

List shapes
-----------

A :ref:`list <list>` shape is defined using a :token:`smithy:list_statement`.

The following example defines a list with a string member from the
:ref:`prelude <prelude>`:

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        list MyList {
            member: String
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
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

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        @length(min: 3, max: 10)
        list MyList {
            @length(min: 1, max: 100)
            member: String
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
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


.. _idl-set:

Set shapes
----------

A :ref:`set <set>` set shape is defined using a :token:`smithy:set_statement`.

The following example defines a set of strings:

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        set StringSet {
            member: String
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#StringSet": {
                    "type": "set",
                    "member": {
                        "target": "smithy.api#String"
                    }
                }
            }
        }

Traits can be applied to the set shape and its members:

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        @deprecated
        set StringSet {
            @pattern("\\w+")
            member: String
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#StringSet": {
                    "type": "set",
                    "member": {
                        "target": "smithy.api#String",
                        "traits": {
                            "smithy.api#pattern": "\\w+"
                        }
                    },
                    "traits": {
                        "smithy.api#deprecated": {}
                    }
                }
            }
        }


.. _idl-map:

Map shapes
----------

A :ref:`map <map>` shape is defined using a :token:`smithy:map_statement`.

The following example defines a map of strings to integers:

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        map IntegerMap {
            key: String,
            value: Integer
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
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

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        @length(min: 0, max: 100)
        map IntegerMap {
            @length(min: 1, max: 10)
            key: String,

            @range(min: 1, max: 1000)
            value: Integer
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
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
:token:`smithy:structure_statement`.

The following example defines a structure with two members:

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        structure MyStructure {
            foo: String,
            baz: Integer,
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
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

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        /// This is MyStructure.
        structure MyStructure {
            /// This is documentation for `foo`.
            @required
            foo: String,

            /// This is documentation for `baz`.
            @deprecated
            baz: Integer,
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
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


.. _idl-union:

Union shapes
------------

A :ref:`union <union>` shape is defined using a :token:`smithy:union_statement`.

The following example defines a union shape with several members:

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        union MyUnion {
            i32: Integer,

            @length(min: 1, max: 100)
            string: String,

            time: Timestamp,
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
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

A service shape is defined using a :token:`smithy:service_statement` and the provided
:token:`smithy:node_object` supports the same properties defined in the
:ref:`service specification <service>`.

The following example defines a service named ``ModelRepository`` that binds
a resource named ``Model`` and an operation named ``PingService``:

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        service ModelRepository {
            version: "2020-07-13",
            resources: [Model],
            operations: [PingService]
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
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

An operation shape is defined using an :token:`smithy:operation_statement` and the
provided :token:`smithy:node_object` supports the same properties defined in the
:ref:`operation specification <operation>`.

The following example defines an operation shape that accepts an input
structure named ``Input``, returns an output structure named ``Output``, and
can potentially return the ``Unavailable`` or ``BadRequest``
:ref:`error structures <error-trait>`.

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        operation PingService {
            input: PingServiceInput,
            output: PingServiceOutput,
            errors: [UnavailableError, BadRequestError]
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
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


.. _idl-resource:

Resource shape
--------------

A resource shape is defined using a :token:`smithy:resource_statement` and the
provided :token:`smithy:node_object` supports the same properties defined in the
:ref:`resource specification <resource>`.

The following example defines a resource shape that has a single identifier,
and defines a :ref:`read <read-lifecycle>` operation:

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        resource SprocketResource {
            identifiers: {
                sprocketId: String,
            },
            read: GetSprocket,
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
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


.. _documentation-comment:

Documentation comment
=====================

:token:`Documentation comments <smithy:documentation_comment>` are a
special kind of :token:`smithy:comment` that provide
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
shape. The shape ID of a trait is *resolved* against :token:`smithy:use_statement`\s
and the current namespace in exactly the same way as
:ref:`other shape IDs <relative-shape-id>`.

The following example applies the :ref:`length-trait` and
:ref:`documentation-trait` to ``MyString``:

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        @length(min: 1, max: 100)
        @documentation("Contains a string")
        string MyString

    .. code-tab:: json

        {
            "smithy": "1.0",
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

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        @trait
        structure foo {}

        @foo
        string MyString1

        @foo()
        string MyString2

    .. code-tab:: json

        {
            "smithy": "1.0",
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
of brackets (``[``) and (``]``) using a :token:`smithy:node_array` production.

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
:token:`smithy:apply_statement`.

The following example applies the :ref:`documentation-trait` and
:ref:`length-trait` to the ``smithy.example#MyString`` shape:

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        apply MyString @documentation("This is my string!")
        apply MyString @length(min: 1, max: 10)

    .. code-tab:: json

        {
            "smithy": "1.0",
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

An array node is defined like a JSON array. A :token:`smithy:node_array` contains
zero or more heterogeneous :token:`smithy:node_value`\s. A trailing comma is allowed
in a ``node_array``.

The following examples define arrays with zero, one, and two values:

* ``[]``
* ``[true]``
* ``[1, "hello",]``

.. rubric:: Object node

An object node is defined like a JSON object. A :token:`smithy:node_object` contains
zero or more key value pairs of strings (a :token:`smithy:node_object_key`) that map
to heterogeneous :token:`smithy:node_value`\s. A trailing comma is allowed
in a ``node_object``.

The following examples define objects with zero, one, and two key value pairs:

* ``{}``
* ``{foo: true}``
* ``{foo: "hello", "bar": [1, 2, {}]}``

.. rubric:: Number node

A node :token:`smithy:number` contains numeric data. It is defined like a JSON
number. The following examples define several ``number`` values:

* ``0``
* ``0.0``
* ``1234``
* ``-1234.1234``
* ``1e+2``
* ``1.0e-10``

.. rubric:: Node keywords

Several keywords are used when parsing :token:`smithy:node_value`.

* ``true``: The value is treated as a boolean ``true``
* ``false``: The value is treated as a boolean ``false``
* ``null``: The value is treated like a JSON ``null``


String values
=============

A ``node_value`` can contain :token:`smithy:node_string_value` productions that all
define strings.

.. rubric:: New lines

New lines in strings are normalized from CR (\u000D) and CRLF (\u000D\u000A)
to LF (\u000A). This ensures that strings defined in a Smithy model are
equivalent across platforms. If a literal ``\r`` is desired, it can be added
a string value using the Unicode escape ``\u000d``.

.. rubric:: String equivalence

The ``node_string_value`` production defines several productions used to
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
    ....<p>Hi\n....bar</p>
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
