..  _prelude:

=============
Prelude model
=============

All Smithy models automatically include a *prelude*. The prelude defines
various simple shapes and every trait defined in the core specification.
When using the :ref:`IDL <idl>`, shapes defined in the prelude can be
referenced from within any namespace using a relative shape ID.

.. tip::

    Prelude shapes SHOULD be used when possible to minimize duplication
    in models.


--------------
Prelude shapes
--------------

.. code-block:: smithy
    :caption: Smithy prelude
    :name: prelude-shapes

    $version: "1.0"

    namespace smithy.api

    string String

    blob Blob

    bigInteger BigInteger

    bigDecimal BigDecimal

    timestamp Timestamp

    document Document

    boolean Boolean

    byte Byte

    short Short

    integer Integer

    long Long

    float Float

    double Double

    /// The single unit type shape, similar to Void and None in other
    /// languages, used to represent no meaningful value.
    @unitType
    structure Unit {}
