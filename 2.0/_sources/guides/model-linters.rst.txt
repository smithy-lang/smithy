==============
Linting Models
==============

This guide describes how to apply optional linters to a Smithy model.


----------------
Linting overview
----------------

A *linter* is a special kind of :ref:`model validator <validation>`
that is configurable. Linter implementations are found in code. The
`smithy-linters`_ package in Maven Central contains several linters that
can be used to apply additional validation to Smithy models.

The following example adds ``smithy-linters`` as a dependency to a Smithy project:

.. tab:: Smithy CLI

    .. code-block:: json
        :caption: smithy-build.json

        {
            "...": "...",
            "maven": {
                "dependencies": [
                    "software.amazon.smithy:smithy-linters:__smithy_version__"
                ]
            },
            "...": "..."
        }

.. tab:: Gradle

    .. tab:: Kotlin

        .. code-block:: kotlin
            :caption: build.gradle.kts

            dependencies {
                ...
                implementation("software.amazon.smithy:smithy-linters:__smithy_version__")
            }

    .. tab:: Groovy

        .. code-block:: groovy
            :caption: build.gradle

            dependencies {
                ...
                implementation 'software.amazon.smithy:smithy-linters:__smithy_version__'
            }


After the dependency is added and available on the Java classpath, validators
defined in the package and registered using `Java SPI`_ are available for
use in Smithy models.


-----------------------------
Linters in ``smithy-linters``
-----------------------------

For Java developers using Smithy's reference implementation, the following
linters (except for UnreferencedShape) are defined in the  ``software.amazon.smithy:smithy-linters``
Maven package.


.. _UnreferencedShape:

UnreferencedShape
=================

Emits when a shape is not connected to the rest of the model. If no
configuration is provided, the linter will check if a shape is connected to
the closure of any service shape. A selector can be provided to define a
custom set of "root" shapes to customize how the linter determines if a shape
is unreferenced. Shapes that are connected through the :ref:`idref-trait`
are considered connected.

Rationale
    Just like unused variables in code, removing unused shapes from a model
    makes the model easier to maintain.

Default severity
    ``NOTE``

Configuration
    .. list-table::
       :header-rows: 1
       :widths: 20 20 60

       * - Property
         - Type
         - Description
       * - rootShapeSelector
         - ``string``
         - A :ref:`selector <selectors>` that specifies the root shape(s)
           from which to detect if other shapes are connected. Defaults
           to "service", meaning any shape connected to any service shape
           in the model is considered referenced.

Example:

.. code-block:: smithy

    $version: "2"

    metadata validators = [
        {
            // Find shapes that aren't connected to service shapes or shapes
            // marked with the trait, smithy.example#myCustomTrait.
            name: "UnreferencedShape"
            configuration: {
                rootShapeSelector: ":is(service, [trait|smithy.example#myCustomTrait])"
            }
        }
    ]

.. note::

    For backward compatibility reasons, the ``UnreferencedShape`` validator is available
    in ``software.amazon.smithy:smithy-model`` Maven package and does not require
    additional dependencies.


.. _AbbreviationName:

AbbreviationName
================

Validates that shape names and member names do not represent abbreviations
with all uppercase letters. For example, instead of using "XMLRequest" or
"instanceID", this validator recommends using "XmlRequest" and "instanceId".

Rationale
    Using a strict form of camelCase where abbreviations are written just
    like other words makes names more predictable and easier to work with
    in tooling. For example, a tool that generates code in Python might wish
    to represent camelCase words using snake_case; utilizing strict camel
    casing makes it easier to split words apart.

Default severity
    ``DANGER``

Configuration
    .. list-table::
       :header-rows: 1
       :widths: 20 20 60

       * - Property
         - Type
         - Description
       * - allowedAbbreviations
         - [ ``string`` ]
         - A case-insensitive list of abbreviations to allow to be all capital
           letters. Defaults to an empty list.

Example:

.. code-block:: smithy

    $version: "2"

    metadata validators = [
        {name: "AbbreviationName"}
    ]


.. _CamelCase:

CamelCase
=========

Validates that shape names and member names adhere to a consistent style of
camel casing. By default, this validator will ensure that shape names use
UpperCamelCase, trait shape names use lowerCamelCase, and that member names
use *either* lowerCamelCase or UpperCamelCase (depending on which is currently more
prevalent in each service closure).

Rationale
    Utilizing a consistent camelCase style makes it easier to understand a
    model and can lead to consistent naming in code generated from Smithy
    models.

Default severity
    ``DANGER``

Configuration
    .. list-table::
       :header-rows: 1
       :widths: 20 20 60

       * - Property
         - Type
         - Description
       * - memberNames
         - ``string``
         - Specifies the camelCase style of member names. Can be set to either
           "upper", "lower", or "auto" (the default).

Example:

.. code-block:: smithy

    $version: "2"

    metadata validators = [
        {name: "CamelCase"}
    ]

.. _MissingSensitiveTrait:

MissingSensitiveTrait
=====================

This validator scans shape or member names and identifies ones that look like they could contain
sensitive information but are not marked with the ``@sensitive`` trait. This does not apply to
shapes where the ``@sensitive`` trait would be invalid. Users may also configure this validator
with a custom list of terms, and choose to ignore the built-in defaults. The defaults terms include
types of personal information such as 'birth day', 'billing address', 'zip code', or 'gender',
as well as information that could be maliciously exploited such as  'password', 'secret key', or 'credit card'.

Rationale
    Sensitive information often incurs legal requirements regarding the handling and logging
    of it. Mistakenly not marking sensitive data accordingly carries a large risk, and it is
    helpful to have an automated validator to catch instances of this rather than rely on best efforts.

Default severity
    ``WARNING``

Configuration
    .. list-table::
       :header-rows: 1
       :widths: 20 20 60

       * - Property
         - Type
         - Description
       * - terms
         - [ ``string`` ]
         - A list of search terms that match shape or member names
           case-insensitively based on word boundaries (for example, the term
           "access key id" matches "AccessKeyId", "access_key_id", and
           "accesskeyid"). See :ref:`words-boundaries` for details.
       * - excludeDefaults
         - ``boolean``
         - A flag indicating whether or not to disregard the default set
           of terms. This property is not required and defaults to false.
           If set to true, ``terms`` must be provided.

Example:

.. code-block:: smithy

    $version: "2"

    metadata validators = [{
        name: "MissingSensitiveTrait"
        configuration: {
            excludeDefaults: false,
            terms: ["home planet"]
        }
    }]

.. _NoninclusiveTerms:

NoninclusiveTerms
=================

Validates that all text content in a model (i.e. shape names, member names,
documentation, trait values, etc.) does not contain words that perpetuate cultural
biases. This validator has a built-in set of bias terms that are commonly found
in APIs along with suggested alternatives.

Noninclusive terms are case-insensitively substring matched and can have any
number of leading or trailing whitespace or non-whitespace characters.

This validator has built-in mappings from noninclusive terms to match model
text to suggested alternatives. The configuration allows for additional terms
to suggestions mappings to either override or append the built-in mappings. If
a match occurs and the suggested alternatives is empty, no suggestion is made
in the generated warning message.

Rationale
    Intent doesn't always match impact. The use of noninclusive language like
    "whitelist" and "blacklist" perpetuates bias through past association of
    acceptance and denial based on skin color. Other words should be used that
    are not only inclusive, but more clearly communicate meaning. Words like
    allowList and denyList much more clearly indicate that something is
    allowed or denied.

Default severity
    ``WARNING``

Configuration
    .. list-table::
       :header-rows: 1
       :widths: 20 20 60

       * - Property
         - Type
         - Description
       * - terms
         - { ``keyword`` -> [ ``alternatives`` ] }
         - A set of noninclusive terms to suggestions to either override or replace
           the built-in mappings. This property is not required unless
           ``excludeDefaults`` is true. The default value is the empty set.
       * - excludeDefaults
         - ``boolean``
         - A flag indicating whether or not the mappings set specified by ``terms``
           configuration replaces the built-in set or appends additional mappings.
           This property is not required and defaults to false.

Example:

.. code-block:: smithy

    $version: "2"

    metadata validators = [{
        name: "NoninclusiveTerms"
        configuration: {
            excludeDefaults: false,
            terms: {
                mankind: ["humankind"],
                mailman: ["mail carrier", "postal worker"]
            }
        }
    }]


.. _ReservedWords:

ReservedWords
=============

Validates that shape names and member names do not match a configured set of
reserved words.

Rationale
    Tools that generate code from Smithy models SHOULD automatically convert
    reserved words into symbols that are safe to use in the targeted
    programming language. This validator can be used to warn about these
    conversions as well as to prevent sensitive words, like internal
    code-names, from appearing in public artifacts.

Default Severity
    ``DANGER``

Configuration
    A single key, ``reserved``, is **Required** in the configuration. Its
    value is a list of objects with the following properties:

    .. list-table::
        :header-rows: 1
        :widths: 20 20 60

        * - Property
          - Type
          - Description
        * - words
          - [ ``string`` ]
          - A list of words that shape or member names MUST not case-insensitively
            match. Supports a leading and trailing wildcard character of "*".
            See :ref:`reserved-words-wildcards` for details.
        * - terms
          - [ ``string`` ]
          - A list of search terms that match shape or member names
            case-insensitively based on word boundaries (for example, the term
            "access key id" matches "AccessKeyId", "access_key_id", and
            "accesskeyid"). See :ref:`words-boundaries` for details.
        * - selector
          - ``string``
          - Specifies a selector of shapes to validate for this configuration.
            Defaults to validating all shapes, including member names.

            .. note::

                When evaluating member shapes, the *member name* will be
                evaluated instead of the shape name.
        * - reason
          - ``string``
          - A reason to display for why this set of words is reserved.

Example:

.. code-block:: smithy

    $version: "2"

    metadata validators = [{
        id: "FooReservedWords"
        name: "ReservedWords"
        configuration: {
            reserved: [
                {
                    words: ["Codename"]
                    reason: "This is the internal project name."
                }
            ]
        }
    }]


.. _reserved-words-wildcards:

Wildcards in ReservedWords
--------------------------

The ReservedWords validator allows leading and trailing wildcard characters to
be specified.

- Using both a leading and trailing wildcard indicates that shape or member
  names match when case-insensitively **containing** the word. The following
  table shows matches for a reserved word of ``*codename*``:

  .. list-table::
      :header-rows: 1
      :widths: 75 25

      * - Example
        - Result
      * - Create\ **Codename**\ Input
        - Match
      * - **Codename**\ Resource
        - Match
      * - Referenced\ **Codename**
        - Match
      * - **Codename**
        - Match

- Using a leading wildcard indicates that shape or member names match when
  case-insensitively **ending with** the word. The following table shows
  matches for a reserved word of ``*codename``:

  .. list-table::
      :header-rows: 1
      :widths: 75 25

      * - Example
        - Result
      * - CreateCodenameInput
        - No match
      * - CodenameResource
        - No match
      * - Referenced\ **Codename**
        - Match
      * - **Codename**
        - Match

- Using a trailing wildcard indicates that shape or member names match when
  case-insensitively **starting with** the word. The following table shows
  matches for a reserved word of ``codename*``:

  .. list-table::
      :header-rows: 1
      :widths: 75 25

      * - Example
        - Result
      * - CreateCodenameInput
        - No match
      * - **Codename**\ Resource
        - Match
      * - ReferencedCodename
        - No Match
      * - **Codename**
        - Match

- Using no wildcards indicates that shape or member names match when
  case-insensitively **the same as** the word. The following table shows
  matches for a reserved word of ``codename``:

  .. list-table::
      :header-rows: 1
      :widths: 75 25

      * - Example
        - Result
      * - CreateCodenameInput
        - No match
      * - CodenameResource
        - No match
      * - ReferencedCodename
        - No match
      * - **Codename**
        - Match

.. _words-boundaries:

Words boundary matching
-----------------------

Word boundaries can be used to find terms of interest. Word boundary search
text consists of one or more alphanumeric words separated by a single
space. When comparing against another string, the contents of the string
are separated into words based on word boundaries. Those words are
case-insensitively compared against the words in the search text for a match.

Word boundaries are detected when the casing between two characters changes,
or the type of character between two characters changes. The following table
demonstrates how comparison text is parsed into words.

.. list-table::
    :header-rows: 1
    :widths: 50 50

    * - Comparison text
      - Parsed words
    * - accessKey
      - access key
    * - accessKeyID
      - access key id
    * - accessKeyIDValue
      - access key id value
    * - accesskeyId
      - accesskey id
    * - accessKey1
      - access key 1
    * - access_keyID
      - access key id

The following table shows matches for a search term of ``secret id``,
meaning the word "secret" needs to be followed by the word "id". Word
boundary searches also match if the search terms concatenated together with
no spaces is considered a word in the search text (for example,
``secret id`` will match the word ``secretid``).

.. list-table::
   :header-rows: 1
   :widths: 75 25

   * - Comparison text
     - Result
   * - Some\ **SecretId**
     - Match
   * - Some\ **SecretID**\ Value
     - Match
   * - Some\ **Secret__ID**\ __value
     - Match
   * - **secret_id**
     - Match
   * - **secret_id**\ 100
     - Match
   * - **secretid**
     - Match
   * - **secretid**\ _value
     - Match
   * - secretidvalue
     - No Match
   * - SecretThingId
     - No match
   * - SomeSecretid
     - No match

.. admonition:: Syntax restrictions

    * Empty search terms are not valid.
    * Only a single space can appear between words in word boundary patterns.
    * Leading and trailing spaces are not permitted in word boundary patterns.
    * Word boundary patterns can only contain alphanumeric characters.


.. _StandardOperationVerb:

StandardOperationVerb
=====================

Looks at each operation shape name and determines if the first word in the
operation shape name is one of the defined standard verbs or if it is a verb
that has better alternatives.

.. note::

    Operations names MUST use a verb as the first word in the shape name
    in order for this validator to properly function.

Rationale
    Using consistent verbs for operation shape names helps consumers of the
    API to more easily understand the semantics of an operation.

Default severity
    ``DANGER``

Configuration
    .. list-table::
       :header-rows: 1
       :widths: 20 20 60

       * - Property
         - Type
         - Description
       * - verbs
         - [ ``string`` ]
         - The list of verbs that each operation shape name MUST start with.
       * - prefixes
         - [ ``string`` ]
         - A list of prefixes that MAY come before one of the valid verbs.
           Prefixes are often used to group families of operations under a
           common prefix (e.g., ``batch`` might be a common prefix in some
           organizations). Only a single prefix is honored.
       * - suggestAlternatives
         - ``object``
         - Used to recommend alternative verbs. Each key is the name of a verb
           that should be changed, and each value is a list of suggested
           verbs to use instead.

.. note::

    At least one ``verb`` or one ``suggestAlternatives`` key-value pair MUST
    be provided.

Example:

.. code-block:: smithy

    $version: "2"

    metadata validators = [{
        name: "StandardOperationVerb"
        configuration: {
            verbs: ["Register", "Deregister", "Associate"]
            prefixes: ["Batch"]
            suggestAlternatives: {
                "Make": ["Create"]
                "Transition": ["Update"]
            }
        }
    }]


.. _StutteredShapeName:
.. _RepeatedShapeName:

RepeatedShapeName
=================

Validates that :ref:`structure` member names and :ref:`union` member
names do not case-insensitively repeat their container shape names.

As an example, if a structure named "Table" contained a member named
"TableName", then this validator would emit a WARNING event.

Rationale
    Repeating a shape name in the members of identifier of the shape is
    redundant.

Default severity
    ``WARNING``

Configuration
    .. list-table::
       :header-rows: 1
       :widths: 20 20 60

       * - Property
         - Type
         - Description
       * - exactMatch
         - ``boolean``
         - If set to true, the validator will only warn if the member name
           is case-insensitively identical to the containing shape's name.


.. _InputOutputStructureReuse:

InputOutputStructureReuse
=========================

Validates that every operation defines a dedicated input and output shape
marked with the :ref:`input-trait` and :ref:`output-trait`.

Rationale
    1. Using the same structure for both input and output can lead to
       backward-compatibility problems in the future if the members or traits
       used in input needs to diverge from those used in output. It is always
       better to use structures that are exclusively used as input or exclusively
       used as output.
    2. Referencing the same input or output structure from multiple operations
       can lead to backward-compatibility problems in the future if the
       inputs or outputs of the operations ever need to diverge. By using the
       same structure, you are unnecessarily tying the interfaces of these
       operations together.

Default severity
    ``DANGER``


.. _MissingPaginatedTrait:

MissingPaginatedTrait
=====================

Checks for operations that look like they should be paginated but do not
have the :ref:`paginated-trait`.

Rationale
    Paginating operations that can return potentially unbounded lists of
    data helps to maintain a predictable SLA and helps to prevent operational
    issues in the future.

Default severity
    ``DANGER``

Configuration
    .. list-table::
       :header-rows: 1
       :widths: 20 20 60

       * - Property
         - Type
         - Description
       * - verbsRequirePagination
         - [``string``]
         - Defines the case-insensitive operation verb prefixes for operations
           that MUST be paginated. A ``DANGER`` event is emitted for any
           operation that has a shape name that starts with one of these verbs.
           Defaults to ``["list", "search"]``.
       * - verbsSuggestPagination
         - [``string``]
         - Defines the case-insensitive operation verb prefixes for operations
           that SHOULD be paginated. A ``WARNING`` event is emitted when an
           operation is found that matches one of these prefixes, the operation
           has output, and the output contains at least one top-level member
           that targets a :ref:`list`. Defaults to ``["describe", "get"]``
       * - inputMembersRequirePagination
         - [``string``]
         - Defines the case-insensitive operation input member names that
           indicate that an operation MUST be paginated. A ``DANGER`` event
           is emitted if an operation is found to have an input member name
           that case-insensitively matches one of these member names.
           Defaults to ``["maxresults", "maxitems", "pagesize", "limit",
           "nexttoken", "pagetoken", "token", "marker"]``
       * - outputMembersRequirePagination
         - [``string``]
         - Defines the case-insensitive operation output member names that
           indicate that an operation MUST be paginated. A ``DANGER`` event
           is emitted if an operation is found to have an output member name
           that case-insensitively matches one of these member names.
           Defaults to ``["nexttoken", "pagetoken", "token", "marker", "nextpage", "nextpagetoken", "position", "nextmarker",
           "paginationtoken", "nextpagemarker"]``.

Example:

.. code-block:: smithy

    metadata validators = [
        {name: "MissingPaginatedTrait"}
    ]


.. _ShouldHaveUsedTimestamp:

ShouldHaveUsedTimestamp
=======================

Looks for shapes that likely represent time, but that do not use a
timestamp shape.

The ShouldHaveUsedTimestamp validator checks the following names:

* string shape names
* short, integer, long, float, and double shape names
* structure member names
* union member names

The ShouldHaveUsedTimestamp validator checks each of the above names to see if
they likely represent a time value. If a name does look like a time value,
the shape or targeted shape MUST be a timestamp shape.

A name is assumed to represent a time value if it:

* Begins or ends with the word "time"
* Begins or ends with the word "date"
* Ends with the word "at"
* Ends with the word "on"
* Contains the exact string "timestamp" or "Timestamp"

For the purpose of this validator, words are matched case insensitively. Words
are separated by either an underscore character, or by mixed case characters.
For example, "FooBar", "fooBar", "foo_bar", "Foo_Bar", and "FOO_BAR" all
contain the same two words, "foo" and "bar".

Rationale
    Smithy tooling can convert timestamp shapes into idiomatic language types
    that make them easier to work with in client tooling.

Default severity
    ``DANGER``

Configuration
    .. list-table::
       :header-rows: 1
       :widths: 20 20 60

       * - Property
         - Type
         - Description
       * - additionalPatterns
         - [ ``string`` ]
         - A list of regular expression patterns that identify names that
           represent time.


.. _MissingClientOptionalTrait:

MissingClientOptionalTrait
==========================

Allows services to control backward compatibility guarantees for
members marked as :ref:`@required <required-trait>` and
:ref:`@default <default-trait>` by requiring the application of the
:ref:`@clientOptional <clientOptional-trait>` trait.

Rationale
    Different service providers have different backward compatibility
    guarantees for :ref:`@required <required-trait>` and
    :ref:`@default <default-trait>` structure members. Some
    services wish to reserve the right to remove the ``@required`` trait at
    any time, while others are able to strictly follow the backward-compatibility
    guarantees of the ``@required`` trait. For example, it is considered
    backward compatible to remove the ``@required`` trait from a member and
    replace it with the ``@default`` trait. However, this isn't possible for
    members that target structure or union shapes because they can have no
    default value. The risk associated with such members may be unacceptable
    for some services.

Default severity
    ``DANGER``

Configuration
    .. list-table::
       :header-rows: 1
       :widths: 20 20 60

       * - Property
         - Type
         - Description
       * - onRequiredOrDefault
         - ``boolean``
         - Requires that members marked with the ``@required`` or ``@default``
           trait are also marked with the ``@clientOptional`` trait.
       * - onRequiredStructureOrUnion
         - ``boolean``
         - Requires that ``@required`` members that target structure or union
           shapes are also marked with the ``@clientOptional`` trait.
           ``@required`` members that target structures and unions are risky
           because there is no backward compatible way to replace the
           ``@required`` trait with the ``@default`` trait if the member ever
           needs to be made optional.

The following example requires that ``@required`` members that target a structure or
union are marked with the ``@clientOptional`` trait.

.. code-block:: smithy

    $version: "2"

    metadata validators = [
        {
            name: "MissingClientOptionalTrait",

            // Limit validation to a specific set of namespaces.
            namespaces: ["smithy.example"],

            configuration: {
                onRequiredStructureOrUnion: true
            }
        }
    ]

This validation can be suppressed for any member that the service provider
decides is not at risk of ever needing to become optional in the future:

.. code-block:: smithy

    structure Sprocket {
        @required
        @suppress(["MissingClientOptionalTrait"])
        owner: OwnerStructure
    }


-------------------------
Writing custom validators
-------------------------

Custom validators can be written in Java to apply more advanced model validation.
Writing a custom validator involves writing an implementation of a
Smithy validator in Java, creating a JAR, and making the JAR available on the
classpath.

Custom validators are implementations of the
``software.amazon.smithy.model.validation.Validator`` interface. Most
validators should extend from ``software.amazon.smithy.model.validation.AbstractValidator``.

The following linter emits a ``ValidationEvent`` for every shape in the
model that is not documented.

.. code-block:: java

    package com.example.mypackage;

    import java.util.List;
    import java.util.stream.Collectors;
    import software.amazon.smithy.model.Model;
    import software.amazon.smithy.model.traits.DocumentationTrait;
    import software.amazon.smithy.model.validation.AbstractValidator;
    import software.amazon.smithy.model.validation.ValidationEvent;

    public class DocumentationValidator extends AbstractValidator {
        @Override
        public List<ValidationEvent> validate(Model model) {
            return model.shapes()
                    .filter(shape -> !shape.hasTrait(DocumentationTrait.class))
                    .map(shape -> error(shape, "This shape is not documented!"))
                    .collect(Collectors.toList());
        }
    }

Validators need to be registered as Java service providers. Add the following
class name to a file named ``software.amazon.smithy.model.validation.Validator``
found in the ``src/main/resources/META-INF/services`` directory of a standard Gradle
Java package:

.. code-block:: none

    com.example.mypackage.DocumentationValidator

When added to the classpath (typically as a dependency of a published JAR),
the custom validator is automatically applied to a model each time the
model is loaded.


----------------------
Writing custom Linters
----------------------

Like custom validators, custom linters can be written in Java to apply more
advanced model validation.

Custom linters are implementations of the
``software.amazon.smithy.model.validation.Validator`` interface. Because
linters are configurable, they are created using an implementation of the
``software.amazon.smithy.model.validation.ValidatorService`` interface.

The following validator emits a ``ValidationEvent`` for every shape in the
model that has documentation that contains a forbidden string.

.. code-block:: java

    package com.example.mypackage;

    import java.util.List;
    import java.util.Optional;
    import java.util.stream.Collectors;
    import java.util.stream.Stream;
    import software.amazon.smithy.model.Model;
    import software.amazon.smithy.model.node.NodeMapper;
    import software.amazon.smithy.model.shapes.Shape;
    import software.amazon.smithy.model.traits.DocumentationTrait;
    import software.amazon.smithy.model.validation.AbstractValidator;
    import software.amazon.smithy.model.validation.ValidationEvent;
    import software.amazon.smithy.model.validation.ValidatorService;

    public class ForbiddenDocumentationValidator extends AbstractValidator {

        /**
         * ForbiddenDocumentation configuration settings.
         */
        public static final class Config {
            private List<String> forbid;

            public List<String> getForbid() {
                return forbid;
            }

            public void setForbid(List<String> forbid) {
                this.forbid = forbid;
            }
        }

        // Does the actual work of converting metadata found in a Smithy
        // model into an actual implementation of a Validator.
        public static final class Provider extends ValidatorService.Provider {
            public Provider() {
                super(ForbiddenDocumentationValidator.class, configuration -> {
                    // Deserialize the Node value into the Config POJO.
                    NodeMapper mapper = new NodeMapper();
                    ForbiddenDocumentationValidator.Config config = mapper.deserialize(configuration, Config.class);
                    return new ForbiddenDocumentationValidator(config);
                });
            }
        }

        private final List<String> forbid;

        // The constructor is private since the validator is only intended to
        // be created when loading a model via the Provider class.
        private ForbiddenDocumentationValidator(Config config) {
            this.forbid = config.forbid;
        }

        @Override
        public List<ValidationEvent> validate(Model model) {
            // Find every shape that violates the linter and return a list
            // of ValidationEvents.
            return model.shapes()
                    .filter(shape -> shape.hasTrait(DocumentationTrait.class))
                    .flatMap(shape -> validateShape(shape).map(Stream::of).orElseGet(Stream::empty))
                    .collect(Collectors.toList());
        }

        private Optional<ValidationEvent> validateShape(Shape shape) {
            // Grab the trait by type.
            DocumentationTrait trait = shape.expectTrait(DocumentationTrait.class);
            String docString = trait.getValue();

            for (String text : forbid) {
                if (docString.contains(text)) {
                    // Emit an event that points at the location of the trait
                    // and associates the warning with the shape.
                    return Optional.of(warning(shape, trait, "Documentation uses forbidden text: " + text));
                }
            }

            return Optional.empty();
        }
    }

Configurable linters need to be registered as Java service providers. Add the following
class name to a file named ``software.amazon.smithy.model.validation.ValidatorService``
found in the ``src/main/resources/META-INF/services`` directory of a standard Gradle
Java package:

.. code-block:: none

    com.example.mypackage.ForbiddenDocumentationValidator$Provider

When added to the classpath (typically as a dependency of a published JAR),
the custom validator is available to be used as a validator. The following
example warns each time the word "meow" appears in documentation:

.. code-block:: smithy

    $version: "2"

    metadata validators = [
        {
            name: "ForbiddenDocumentation"
            configuration: {
                forbid: ["meow"]
            }
        }
    ]

.. tip::

    The :ref:`EmitEachSelector` can get you pretty far without needing to
    write any Java code. For example, the above linter can be implemented
    using the following Smithy model:

    .. code-block:: smithy

        $version: "2"

        metadata validators = [
            {
                name: "EmitEachSelector"
                id: "ForbiddenDocumentation"
                message: "Documentation uses forbidden text"
                configuration: {
                    selector: "[trait|documentation*='meow']"
                }
            }
        ]

.. _smithy-linters: https://search.maven.org/artifact/software.amazon.smithy/smithy-linters
.. _Java SPI: https://docs.oracle.com/javase/tutorial/sound/SPI-intro.html
