package software.amazon.smithy.model.knowledge;

import java.util.Optional;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.PaginatedTrait;

/**
 * Resolved and valid pagination information about an operation.
 */
public final class PaginationInfo {

    private final OperationShape operation;
    private final StructureShape input;
    private final StructureShape output;
    private final PaginatedTrait paginatedTrait;
    private final MemberShape inputToken;
    private final MemberShape outputToken;

    PaginationInfo(
            OperationShape operation,
            StructureShape input,
            StructureShape output,
            PaginatedTrait paginatedTrait,
            MemberShape inputToken,
            MemberShape outputToken
    ) {
        this.operation = operation;
        this.input = input;
        this.output = output;
        this.paginatedTrait = paginatedTrait;
        this.inputToken = inputToken;
        this.outputToken = outputToken;
    }

    public OperationShape getOperation() {
        return operation;
    }

    public StructureShape getInput() {
        return input;
    }

    public StructureShape getOutput() {
        return output;
    }

    public PaginatedTrait getPaginatedTrait() {
        return paginatedTrait;
    }

    public MemberShape getInputTokenMember() {
        return inputToken;
    }

    public MemberShape getOutputTokenMember() {
        return outputToken;
    }

    public Optional<MemberShape> getItemsMember() {
        return paginatedTrait.getItems().flatMap(output::getMember);
    }

    public Optional<MemberShape> getPageSizeMember() {
        return paginatedTrait.getPageSize().flatMap(input::getMember);
    }
}
