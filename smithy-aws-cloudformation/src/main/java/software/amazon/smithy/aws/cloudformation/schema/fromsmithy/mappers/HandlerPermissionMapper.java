/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.aws.cloudformation.schema.fromsmithy.mappers;

import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import software.amazon.smithy.aws.cloudformation.schema.fromsmithy.CfnMapper;
import software.amazon.smithy.aws.cloudformation.schema.fromsmithy.Context;
import software.amazon.smithy.aws.cloudformation.schema.model.Handler;
import software.amazon.smithy.aws.cloudformation.schema.model.ResourceSchema;
import software.amazon.smithy.aws.iam.traits.IamActionTrait;
import software.amazon.smithy.aws.iam.traits.RequiredActionsTrait;
import software.amazon.smithy.aws.traits.ServiceTrait;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.NoReplaceTrait;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.SetUtils;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Generates the resource's handler permissions list based on the lifecycle operation
 * used and any permissions listed in the {@code aws.iam#requiredActions} trait.
 *
 * @see <a href="https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-schema.html#schema-properties-handlers">handlers Docs</a>
 */
@SmithyInternalApi
public final class HandlerPermissionMapper implements CfnMapper {
    @Override
    public void before(Context context, ResourceSchema.Builder resourceSchema) {
        if (context.getConfig().getDisableHandlerPermissionGeneration()) {
            return;
        }

        Model model = context.getModel();
        ServiceShape service = context.getService();
        ResourceShape resource = context.getResource();

        // Start the create and update handler permission gathering.
        // TODO Break this out to its own knowledge index if it becomes useful in more contexts.
        Set<String> createPermissions = resource.getCreate()
                .map(operation -> getPermissionsEntriesForOperation(model, service, operation))
                .orElseGet(TreeSet::new);
        Set<String> updatePermissions = resource.getUpdate()
                .map(operation -> getPermissionsEntriesForOperation(model, service, operation))
                .orElseGet(TreeSet::new);

        // Add the permissions from the resource's put lifecycle operation
        // to the relevant handlers.
        Set<String> putPermissions = resource.getPut()
                .map(operation -> getPermissionsEntriesForOperation(model, service, operation))
                .orElse(SetUtils.of());
        createPermissions.addAll(putPermissions);
        // Put operations without the noReplace trait are used for updates.
        resource.getPut()
                .map(model::expectShape)
                .filter(shape -> !shape.hasTrait(NoReplaceTrait.class))
                .ifPresent(shape -> updatePermissions.addAll(putPermissions));

        // Set the create and update handlers, if they have permissions, now that they're complete.
        if (!createPermissions.isEmpty()) {
            resourceSchema.addHandler("create", Handler.builder().permissions(createPermissions).build());
        }
        if (!updatePermissions.isEmpty()) {
            resourceSchema.addHandler("update", Handler.builder().permissions(updatePermissions).build());
        }

        // Add the handler permission sets that don't need operation
        // permissions to be combined.
        resource.getRead()
                .map(operation -> getPermissionsEntriesForOperation(model, service, operation))
                .ifPresent(permissions -> resourceSchema.addHandler("read", Handler.builder()
                        .permissions(permissions).build()));

        resource.getDelete()
                .map(operation -> getPermissionsEntriesForOperation(model, service, operation))
                .ifPresent(permissions -> resourceSchema.addHandler("delete", Handler.builder()
                        .permissions(permissions).build()));

        resource.getList()
                .map(operation -> getPermissionsEntriesForOperation(model, service, operation))
                .ifPresent(permissions -> resourceSchema.addHandler("list", Handler.builder()
                        .permissions(permissions).build()));
    }

    private Set<String> getPermissionsEntriesForOperation(Model model, ServiceShape service, ShapeId operationId) {
        OperationShape operation = model.expectShape(operationId, OperationShape.class);
        Set<String> permissionsEntries = new TreeSet<>();

        // Add the operation's permission name itself.
        String operationActionName =
                service.getTrait(ServiceTrait.class)
                        .map(ServiceTrait::getArnNamespace)
                        .orElse(service.getId().getName())
                .toLowerCase(Locale.US);
        operationActionName += ":" + operationId.getName(service);
        permissionsEntries.add(operationActionName);

        // Add all the other required actions for the operation.
        permissionsEntries.addAll(operation.getTrait(IamActionTrait.class)
                .map(IamActionTrait::getRequiredActions)
                .orElseGet(() -> operation.getTrait(RequiredActionsTrait.class)
                        .map(RequiredActionsTrait::getValues)
                        .orElse(ListUtils.of())));
        return permissionsEntries;
    }
}
