.. _rules-engine-standard-library:

=============================
Rules engine standard library
=============================

Functions in the Smithy rules engine are named routines that operate on a
finite set of specified inputs, returning an output. See the :ref:`function object documentation <rules-engine-endpoint-rule-set-function>`
for more information. The rules engine has a set of included functions that can
be invoked without additional dependencies, called the standard library, which
are defined as follows:


.. _rules-engine-standard-library-booleanEquals:

``booleanEquals`` function
==========================

Summary
    Evaluates two boolean values for equality, returning true if they match.
Argument type
    * value1: ``bool``
    * value2: ``bool``
Return type
    ``bool``

The following example uses ``booleanEquals`` to check if value of the ``foo``
parameter is equal to the value ``false``:

.. code-block:: json

    {
        "fn": "booleanEquals",
        "argv": [
            {"ref": "foo"},
            true
        ]
    }


.. _rules-engine-standard-library-getAttr:

``getAttr`` function
====================

Summary
    Extracts a value at the given path from an ``object`` or ``array``.
Argument types
    * value: ``object`` or ``array``
    * path: ``string``
Return type
    ``document``

The following example uses ``getAttr`` to extract the ``scheme`` value from the
parsed value of the ``foo`` parameter:

.. code-block:: json

    {
        "fn": "getAttr",
        "argv": [
            {
                "fn": "parseURL",
                "argv": [
                    {"ref": "foo"}
                ]
            },
            "scheme"
        ]
    }


.. _rules-engine-standard-library-getAttr-path-strings:

------------------------
Parsing ``path`` strings
------------------------

Path strings for the `getAttr function`_ are composed of two components:

#. Keys, e.g. ``scheme`` in ``uri#scheme``.
#. Indexes, e.g. ``[2]`` in ``list[2]``.

An index MUST only occur at the end of a path, as indexes always return
``option`` values.

An algorithm for parsing ``path`` strings is as follows:

#. Split the string on the dot character (``.``).
#. Iterate over the parts:

   #. If the part contains the open square bracket character (``[``):

      #. If there are characters before the ``[`` character, parse these
         characters as a key.
      #. Parse the value between the ``[`` and a close square bracket
         character (``]``) as an index.
   #. Otherwise, parse the value as a key.

.. note::
    Implementers SHOULD assume that the ``path`` string has been validated,
    meaning they do not need to perform their own validation at runtime.


.. _rules-engine-standard-library-isSet:

``isSet`` function
==================

Summary
    Evaluates whether a value, such as an endpoint parameter, is not ``null``.
Argument type
    * value: ``option<T>``
Return type
    ``bool``

The following example uses ``isSet`` to check if the ``foo`` parameter is not
null:

.. code-block:: json

    {
        "fn": "isSet",
        "argv": [
            {"ref": "foo"}
        ]
    }

.. important::
    ``isSet`` must accept an ``option`` and only considers optionality.
    ``isSet`` does not consider truthiness.


.. _rules-engine-standard-library-isValidHostLabel:

``isValidHostLabel`` function
=============================

Summary
    Evaluates whether the input string is a compliant :rfc:`1123` host segment.
    When ``allowSubDomains`` is true, evaluates whether the input string is
    composed of values that are each compliant :rfc:`1123` host segments joined
    by dot (``.``) characters.
Argument type
    * value: ``string``
    * allowSubDomains: ``bool``
Return type
    ``bool``

The following example uses ``isValidHostLabel`` to check if the value of the
``foo`` parameter is an :rfc:`1123` compliant host segment.

.. code-block:: json

    {
        "fn": "isValidHostLabel",
        "argv": [
            {"ref": "foo"},
            false
        ]
    }


.. _rules-engine-standard-library-not:

``not`` function
================

Summary
    Performs logical negation on the provided boolean value, returning the
    negated value.
Argument type
    * value: ``bool``
Return type
    ``bool``

The following example uses ``not`` to negate the value of the
``foo`` parameter:

.. code-block:: json

    {
        "fn": "not",
        "argv": [
            {"ref": "foo"}
        ]
    }

The following example uses ``not`` to negate the value of an `isSet function`_:

.. code-block:: json

    {
        "fn": "not",
        "argv": [
            {
                "fn": "isSet",
                "argv": [
                    {"ref": "foo"}
                ]
            }
        ]
    }


.. _rules-engine-standard-library-parseURL:

``parseURL`` function
=====================

Summary
    Computes a `URL structure`_ given an input ``string``.
Argument type
    * value: ``string``
Return type
    ``option<URL>``

    *Contains the parsed URL, or an empty optional if the URL could not be
    parsed*

.. important::
    If the URL given contains a query portion, the URL MUST be rejected and the
    function MUST return an empty optional.


The following example uses ``parseURL`` to parse the value of the ``foo``
parameter into its component parts:

.. code-block:: json

    {
        "fn": "parseURL",
        "argv": [
            {"ref": "foo"}
        ]
    }


.. _rules-engine-standard-library-parseURL-URL:

-----------------
``URL`` structure
-----------------

The ``URL`` structure is returned from the `parseURL function`_ when its input
is a valid URL. The ``URL`` object contains the following properties:

.. list-table::
    :header-rows: 1
    :widths: 10 20 70

    * - Property
      - Type
      - Description
    * - scheme
      - ``string``
      - The URL scheme, such as ``https``. The value returned MUST NOT
        include the ``://`` separator.
    * - authority
      - ``string``
      - The host and optional port component of the URL. A default port
        MUST NOT be included. A userinfo segment MUST NOT be included.
    * - path
      - ``string``
      - The unmodified path segment of the URL.
    * - normalizedPath
      - ``string``
      - The path segment of the URL. This value is guaranteed to start and
        end with a ``/`` character.
    * - isIp
      - ``bool``
      - Indicates whether the authority is an IPv4 _or_ IPv6 address.


.. _rules-engine-standard-library-parseURL-examples:

--------
Examples
--------

The following table shows valid and invalid values for an input to the
`parseURL function`_:

.. list-table::
    :header-rows: 1
    :widths: 25 10 10 15 15 15 10

    * - Input
      - Valid?
      - scheme
      - authority
      - path
      - normalizedPath
      - isIp
    * - https://example.com
      - ``true``
      - ``https``
      - ``example.com``
      - ``/``
      - ``/``
      - ``false``
    * - https://example.com:8443?foo=bar&faz=baz
      - ``false``
      -
      -
      -
      -
      -
    * - http://example.com:80/foo/bar
      - ``true``
      - ``http``
      - ``example.com:80``
      - ``/foo/bar``
      - ``/foo/bar/``
      - ``false``
    * - https://127.0.0.1
      - ``true``
      - ``https``
      - ``127.0.0.1``
      - ``/``
      - ``/``
      - ``true``
    * - https://[fe80::1]
      - ``true``
      - ``https``
      - ``[fe80::1]``
      - ``/``
      - ``/``
      - ``true``


.. _rules-engine-standard-library-stringEquals:

``stringEquals`` function
=========================

Summary
    Evaluates two string values for equality, returning true if they match.
Argument type
    * value1: ``string``
    * value2: ``string``
Return type
    ``bool``

The following example uses ``stringEquals`` to check if value of the ``foo``
parameter is equal to the value ``something``:

.. code-block:: json

    {
        "fn": "booleanEquals",
        "argv": [
            {"ref": "foo"},
            "something"
        ]
    }


.. _rules-engine-standard-library-substring:

``substring`` function
======================

Summary
    Computes a portion of a given ``string`` based on the provided start and
    end indices.
Argument type
    * input: ``string``
    * startIndex: ``int``
    * endIndex: ``int``
    * reverse: ``bool``
Return type
    ``option<string>``

The startIndex is inclusive and the endIndex is exclusive.

.. important::
    If the string is not long enough to fully include the substring, the
    function MUST return an empty optional. The length of the returned string,
    when present, will always be ``sendIndex - startIndex``.

    The function MUST return an empty optional when the input contains
    non-ASCII characters.

The following example uses ``substring`` to extract the first four characters of
value of the ``foo`` parameter:

.. code-block:: json

    {
        "fn": "substring",
        "argv": [
            {"ref": "foo"},
            0,
            4,
            false
        ]
    }


.. _rules-engine-standard-library-uriEncode:

``uriEncode`` function
======================

Summary
    Performs :rfc:`3986#section-2.1` defined percent-encoding on the input
    value.
Argument type
    * value: ``string``
Return type
    ``string``

The function MUST percent-encode all characters except the unreserved
characters that RFC 3986 defines: ``A-Z``, ``a-z``, ``0-9``, hyphen (``-``),
underscore (``_``), period (``.``), and tilde (``~``). This includes percent-
encoding the following printable/visible ASCII characters as well as all
unicode characters: ``/:,?#[]{}|@! $&'()*+;=%<>"^`\``. The function MUST use
uppercase hexadecimal digits for all percent-encodings to ensure consistency.

.. note::
    The space character must be percent encoded.


The following example uses ``uriEncode`` to percent-encode the value of the
``foo`` parameter:

.. code-block:: json

    {
        "fn": "uriEncode",
        "argv": [
            {"ref": "foo"}
        ]
    }


.. _rules-engine-standard-library-adding-functions:

Adding functions through extensions
===================================

Extensions to the rules engine can provide additional functions. Code
generators MAY support these additional functions and SHOULD document which
extensions are supported. Additional functions MUST be namespaced, using
two colon ``:`` characters to separate namespace portions. This is utilized to
add the :ref:`AWS rules engine functions <rules-engine-aws-library-functions>`.

The rules engine is highly extensible through
``software.amazon.smithy.rulesengine.language.EndpointRuleSetExtension``
`service providers`_. See the `Javadocs`_ for more information.

.. _Javadocs: https://smithy.io/javadoc/__smithy_version__/software/amazon/smithy/rulesengine/language/EndpointRuleSetExtension.html
.. _service providers: https://docs.oracle.com/javase/tutorial/sound/SPI-intro.html
