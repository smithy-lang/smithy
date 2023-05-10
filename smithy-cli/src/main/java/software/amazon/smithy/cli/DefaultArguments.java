/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.cli;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

final class DefaultArguments implements Arguments {

    private final String[] args;
    private final Map<Class<? extends ArgumentReceiver>, ArgumentReceiver> receivers = new LinkedHashMap<>();
    private List<String> positional;
    private boolean inPositional = false;
    private int position = 0;

    DefaultArguments(String[] args) {
        this.args = args;
    }

    @Override
    public void addReceiver(ArgumentReceiver receiver) {
        receivers.put(receiver.getClass(), receiver);
    }

    @Override
    public boolean hasReceiver(Class<? extends ArgumentReceiver> receiverClass) {
        return receivers.containsKey(receiverClass);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends ArgumentReceiver> T getReceiver(Class<T> type) {
        return (T) Objects.requireNonNull(receivers.get(type));
    }

    @Override
    public Iterable<ArgumentReceiver> getReceivers() {
        return receivers.values();
    }

    @Override
    public boolean hasNext() {
        return position < args.length;
    }

    @Override
    public String peek() {
        String peek = hasNext() ? args[position] : null;

        if (peek != null && !inPositional) {
            // Automatically skip "--" and consider the remaining arguments positional.
            if (peek.equals("--")) {
                inPositional = true;
                position++;
                return peek();
            }

            for (ArgumentReceiver interceptor : receivers.values()) {
                if (interceptor.testOption(peek)) {
                    position++;
                    return peek();
                }

                Consumer<String> optionConsumer = interceptor.testParameter(peek);
                if (optionConsumer != null) {
                    position++;
                    optionConsumer.accept(shiftFor(peek));
                    return peek();
                }
            }
        }

        return peek;
    }

    @Override
    public String shift() {
        String peek = peek();

        if (peek != null) {
            position++;
        }

        return peek;
    }

    @Override
    public String shiftFor(String parameter) {
        if (!hasNext()) {
            throw new CliError("Expected argument for '" + parameter + "'");
        }

        String next = args[position];
        position++;
        return next;
    }

    @Override
    public List<String> getPositional() {
        if (positional == null) {
            positional = new ArrayList<>();

            while (hasNext()) {
                String next = shift();
                if (next != null) {
                    if (!inPositional && next.startsWith("-")) {
                        throw new CliError("Unexpected CLI argument: " + next);
                    } else {
                        inPositional = true;
                        positional.add(next);
                    }
                }
            }
        }

        return positional;
    }
}
