package software.amazon.smithy.openapi.fromsmithy.plugins;

import java.util.List;
import java.util.logging.Logger;
import software.amazon.smithy.model.knowledge.HttpBindingIndex;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ToShapeId;
import software.amazon.smithy.openapi.OpenApiConstants;
import software.amazon.smithy.openapi.OpenApiException;
import software.amazon.smithy.openapi.fromsmithy.Context;
import software.amazon.smithy.openapi.fromsmithy.OpenApiProtocol;
import software.amazon.smithy.openapi.fromsmithy.SmithyOpenApiPlugin;
import software.amazon.smithy.openapi.model.OpenApi;

/**
 * Checks for prefix headers in the input or output of an operation,
 * and handles the settings for throwing an exception or warning if
 * they're found.
 *
 * <p>Custom values for this setting need to be handled in a
 * {@link OpenApiProtocol}.
 */
public class CheckForPrefixHeaders implements SmithyOpenApiPlugin {
    private static final Logger LOGGER = Logger.getLogger(CheckForGreedyLabels.class.getName());

    @Override
    public void before(Context context, OpenApi.Builder builder) {
        var httpBindings = context.getModel().getKnowledge(HttpBindingIndex.class);
        context.getModel().getShapeIndex().shapes(OperationShape.class).forEach(operation -> {
            check(context, httpBindings.getRequestBindings(operation, HttpBindingIndex.Location.PREFIX_HEADERS));
            checkForResponseHeaders(context, httpBindings, operation);
            operation.getErrors().forEach(error -> checkForResponseHeaders(context, httpBindings, error));
        });
    }

    private void checkForResponseHeaders(Context context, HttpBindingIndex bindingIndex, ToShapeId shapeId) {
        check(context, bindingIndex.getResponseBindings(shapeId, HttpBindingIndex.Location.PREFIX_HEADERS));
    }

    private void check(Context context, List<HttpBindingIndex.Binding> bindings) {
        String setting = context.getConfig().getStringMemberOrDefault(
                OpenApiConstants.ON_HTTP_PREFIX_HEADERS, OpenApiConstants.ON_HTTP_PREFIX_HEADERS_FAIL);

        for (var binding : bindings) {
            switch (setting) {
                case OpenApiConstants.ON_HTTP_PREFIX_HEADERS_WARN:
                    LOGGER.warning(createMessage(binding));
                    break;
                case OpenApiConstants.ON_HTTP_PREFIX_HEADERS_FAIL:
                    throw new OpenApiException(createMessage(binding));
                default:
                    break;
            }
        }
    }

    private static String createMessage(HttpBindingIndex.Binding binding) {
        MemberShape member = binding.getMember();
        return String.format(
                "The `httpPrefixHeaders` trait is not supported by OpenAPI and was found on `%s`", member.getId());
    }
}
