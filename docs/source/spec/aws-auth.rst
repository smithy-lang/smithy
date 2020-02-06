.. _aws-authentication:

=========================
AWS Authentication Traits
=========================

This document defines AWS authentication schemes.

.. contents:: Table of contents
    :depth: 2
    :local:
    :backlinks: none


.. _aws.auth#sigv4-trait:

``aws.auth#sigv4`` trait
========================

Trait summary
    The ``aws.auth#sigv4`` trait adds support for `AWS signature version 4`_
    to a service.
Trait selector
    ``service``
Trait value
    An ``object`` that supports the following properties:

    .. list-table::
        :header-rows: 1
        :widths: 10 20 70

        * - Property
          - Type
          - Description
        * - name
          - ``string``
          - **Required**. The signature version 4 service signing name to use
            in the `credential scope`_ when signing requests. This value
            SHOULD match the ``arnNamespace`` property of the
            :ref:`aws.api#service-trait`.

.. tabs::

    .. code-tab:: smithy

        namespace aws.fooBaz

        use aws.api#service
        use aws.auth#sigv4
        use aws.protocols#restJson1

        @service(sdkId: "Some Value")
        @sigv4(name: "foobaz")
        @restJson1
        service FooBaz {
            version: "2018-03-17",
        }

    .. code-tab:: json

        {
            "smithy": "0.5.0",
            "shapes": {
                "aws.fooBaz#FooBaz": {
                    "type": "service",
                    "version": "2018-03-17",
                    "traits": {
                        "aws.protocols#restJson1": true,
                        "aws.api#service": {
                            "sdkId": "Some Value"
                        },
                        "aws.auth#sigv4": {
                            "name": "foobaz"
                        }
                    }
                }
            }
        }


.. _aws.auth#cognitoUserPools-trait:

aws.auth#cognitoUserPools
=========================

Trait summary
    The ``aws.auth#cognitoUserPools`` trait adds support for
    `Amazon Cognito User Pools`_ to a service.
Trait selector
    ``service``
Trait value
    An ``object`` that supports the following properties:

    .. list-table::
        :header-rows: 1
        :widths: 10 20 70

        * - Property
          - Type
          - Description
        * - providerArns
          - ``[string]``
          - **Required**. A list of the Amazon Cognito user pool ARNs. Each
            element is of this format: ``arn:aws:cognito-idp:{region}:{account_id}:userpool/{user_pool_id}``.

.. code-block:: smithy

    namespace aws.fooBaz

    use aws.api#service
    use aws.auth#cognitoUserPools
    use aws.protocols#restJson1

    @service(sdkId: "Some Value")
    @cognitoUserPools(
        providerArns: ["arn:aws:cognito-idp:us-east-1:123:userpool/123"])
    @restJson1
    service FooBaz {
        version: "2018-03-17",
    }


.. _AWS signature version 4: https://docs.aws.amazon.com/general/latest/gr/signature-version-4.html
.. _credential scope: https://docs.aws.amazon.com/general/latest/gr/sigv4-create-string-to-sign.html
.. _Amazon Cognito User Pools: https://docs.aws.amazon.com/cognito/latest/developerguide/cognito-user-identity-pools.html
