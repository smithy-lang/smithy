package software.amazon.smithy.build;

import java.util.List;
import java.util.ArrayList;

import software.amazon.smithy.model.*;
import software.amazon.smithy.model.node.*;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.*;
import software.amazon.smithy.utils.ListUtils;

public final class ProtoReservedFieldsTrait extends AbstractTrait {
	public static final ShapeId ID = ShapeId.from("ns.foo#protoReservedFields");

	private final List<Integer> reserved;

	public ProtoReservedFieldsTrait(Builder builder) {
		super(ID, SourceLocation.NONE);
		this.reserved = ListUtils.copyOf(builder.reserved);
	}

	public List<Integer> getReserved() {
		return this.reserved;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static final class Builder extends AbstractTraitBuilder<ProtoReservedFieldsTrait, Builder> {
		private final List<Integer> reserved = new ArrayList<>();

		public Builder add(Integer reserved) {
			this.reserved.add(reserved);
			return this;
		}

		@Override
		public ProtoReservedFieldsTrait build() {
			return new ProtoReservedFieldsTrait(this);
		}
	}

	public static final class Provider extends AbstractTrait.Provider {
		public Provider() {
			super(ID);
		}

		@Override
		public ProtoReservedFieldsTrait createTrait(ShapeId target, Node value) {
			Builder builder = new Builder().sourceLocation(value);
			for (Node definition : value.expectArrayNode().getElements()) {
				definition.asNumberNode().map(NumberNode::getValue).map(Number::intValue).ifPresent(builder::add);
			}
			return builder.build();
		}
	}

	@Override
	protected Node createNode() {
		return Node.arrayNode();
	}
}
