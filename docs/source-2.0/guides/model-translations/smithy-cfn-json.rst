.. _smithy-cfn-json:

==========================================================
Converting Smithy to CloudFormation JSON
==========================================================

This guide describes how Smithy models can be serialized to JSON AST with
CloudFormation ``Fn::Sub`` intrinsic function wrapping using the
``smithy-cfn-json`` plugin.

------------
Introduction
------------

The ``smithy-cfn-json`` plugin serializes a Smithy model to its JSON AST
representation with automatic CloudFormation ``Fn::Sub`` substitution wrapping.
The output is intended for use as the ``Body`` property of an
``AWS::ApiGateway::RestApi`` CloudFormation resource, enabling direct Smithy
model import without OpenAPI conversion.

String values containing ``${...}`` variable syntax at known trait paths are
automatically wrapped in ``{"Fn::Sub": "..."}`` objects so that CloudFormation
resolves the references at deploy time before passing the body to the API
Gateway SmithyImporter.

.. _smithy-cfn-json-configuration:

-----------------------------------------------
Converting to JSON AST with smithy-build
-----------------------------------------------

The ``smithy-cfn-json`` plugin contained in the
``software.amazon.smithy:smithy-aws-apigateway-cfn`` package can be used with
smithy-build to produce CloudFormation-ready JSON from Smithy models.

The following example shows how to configure the plugin in
``smithy-build.json``:

.. code-block:: json
    :caption: smithy-build.json

    {
        "version": "1.0",
        "plugins": {
            "smithy-cfn-json": {
                "service": "com.example#MyService"
            }
        }
    }

The plugin writes ``{ServiceName}.smithy.json`` to the build output directory.

.. _smithy-cfn-json-settings:

----------------------
Configuration settings
----------------------

.. _smithy-cfn-json-setting-service:

service (``string``)
====================

**Required**. The Smithy service :ref:`shape ID <shape-id>` to export.

.. code-block:: json
    :caption: smithy-build.json

    {
        "version": "1.0",
        "plugins": {
            "smithy-cfn-json": {
                "service": "com.example#MyService"
            }
        }
    }

.. _smithy-cfn-json-setting-disableCloudFormationSubstitution:

disableCloudFormationSubstitution (``boolean``)
===============================================

Set to ``true`` to disable automatic ``Fn::Sub`` wrapping of string values
that contain ``${...}`` variable references. Defaults to ``false``.

.. code-block:: json
    :caption: smithy-build.json

    {
        "version": "1.0",
        "plugins": {
            "smithy-cfn-json": {
                "service": "com.example#MyService",
                "disableCloudFormationSubstitution": true
            }
        }
    }

.. _smithy-cfn-json-substitution:

------------------------------
CloudFormation substitution
------------------------------

When ``disableCloudFormationSubstitution`` is ``false`` (the default), string
values containing ``${...}`` variable syntax at the following trait paths are
automatically wrapped in ``{"Fn::Sub": "..."}`` objects:

* ``aws.apigateway#integration`` — ``uri``, ``credentials``, ``connectionId``,
  ``integrationTarget``
* ``aws.apigateway#authorizers`` — ``*/uri``, ``*/credentials``
* ``aws.auth#cognitoUserPools`` — ``providerArns/*``

CloudFormation resolves ``Fn::Sub`` at deploy time before passing the body to
the API Gateway SmithyImporter.

.. _smithy-cfn-json-example:

-------
Example
-------

Given the following Smithy model input:

.. code-block:: smithy

    @integration(
        type: "aws_proxy"
        uri: "${MyLambdaFunction.Arn}"
        httpMethod: "POST"
        credentials: "${ApiGatewayRole.Arn}"
    )

The plugin produces the following in the generated JSON AST:

.. code-block:: json

    {
        "aws.apigateway#integration": {
            "type": "aws_proxy",
            "uri": {"Fn::Sub": "${MyLambdaFunction.Arn}"},
            "httpMethod": "POST",
            "credentials": {"Fn::Sub": "${ApiGatewayRole.Arn}"}
        }
    }

Values that do not contain ``${...}`` syntax are left as plain strings.
