/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.mqtt.traits;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Represents a parsed MQTT topic.
 */
public final class Topic {
    private static final Pattern LABEL_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+$");

    private final String topic;
    private final List<Level> levels;

    private Topic(String topic, List<Level> levels) {
        this.topic = topic;
        this.levels = Collections.unmodifiableList(levels);
    }

    /**
     * Parses an MQTT topic and labels.
     *
     * @param topic Topic to parse.
     * @return Returns the parsed topic.
     * @throws TopicSyntaxException if the topic is malformed.
     */
    public static Topic parse(String topic) {
        List<Level> levels = new ArrayList<>();
        Set<String> labels = new HashSet<>();

        for (String level : topic.split("/")) {
            if (level.contains("#") || level.contains("+")) {
                throw new TopicSyntaxException(format(
                        "Wildcard levels are not allowed in MQTT topics. Found `%s` in `%s`",
                        level,
                        topic));
            } else if (level.startsWith("{") && level.endsWith("}")) {
                String label = level.substring(1, level.length() - 1);
                if (!LABEL_PATTERN.matcher(label).matches()) {
                    throw new TopicSyntaxException(format(
                            "Invalid topic label name `%s` found in `%s`",
                            label,
                            topic));
                } else if (labels.contains(label)) {
                    throw new TopicSyntaxException(format("Duplicate topic label `%s` found in `%s`", label, topic));
                }
                labels.add(label);
                levels.add(new Level(label, true));
            } else if (level.contains("{") || level.contains("}")) {
                throw new TopicSyntaxException(format(
                        "Topic labels must span an entire level. Found `%s` in `%s`",
                        level,
                        topic));
            } else {
                levels.add(new Level(level, false));
            }
        }

        return new Topic(topic, levels);
    }

    /**
     * Gets all of the hierarchical levels of the topic.
     *
     * @return Returns the topic levels.
     */
    public List<Level> getLevels() {
        return levels;
    }

    /**
     * Gets all of the label levels in the topic.
     *
     * @return Returns the label levels.
     */
    public List<Level> getLabels() {
        return levels.stream()
                .filter(Level::isLabel)
                .collect(Collectors.toList());
    }

    /**
     * Returns true if the topic contains the given label string.
     *
     * @param label Label to check for.
     * @return Returns true if the label exists in the topic.
     */
    public boolean hasLabel(String label) {
        for (Level level : levels) {
            if (level.isLabel && level.value.equals(label)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Determines if this topic conflicts with another topic.
     *
     * @param other Topic to check against.
     * @return Returns true if there is a conflict.
     */
    public boolean conflictsWith(Topic other) {
        int minSize = Math.min(levels.size(), other.levels.size());

        for (int i = 0; i < minSize; i++) {
            Level thisLevel = levels.get(i);
            Level otherLevel = other.levels.get(i);
            // Both are static levels with different values.
            if (!thisLevel.isLabel() && !otherLevel.isLabel()
                    && !thisLevel.getContent().equals(otherLevel.getContent())) {
                return false;
            } else if (thisLevel.isLabel() != otherLevel.isLabel()) {
                // One is static and the other is not, so there is not a
                // conflict. One is more specific than the other.
                return false;
            }
        }

        // At this point, the two patterns are identical. If the segment
        // length is different, then one pattern is more specific than the
        // other, disambiguating them.
        return levels.size() == other.levels.size();
    }

    @Override
    public String toString() {
        return topic;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof Topic)) {
            return false;
        }

        return topic.equals(((Topic) o).topic);
    }

    @Override
    public int hashCode() {
        return topic.hashCode();
    }

    /**
     * Represents a level in a topic.
     */
    public static final class Level {
        private String value;
        private boolean isLabel;

        /**
         * @param value The value of the topic.
         * @param isLabel True if the value is a label.
         */
        public Level(String value, boolean isLabel) {
            this.isLabel = isLabel;
            this.value = value;
        }

        /**
         * @param value The value of the topic.
         */
        public Level(String value) {
            this(value, false);
        }

        /**
         * Gets the content of the topic.
         *
         * <p>Label levels do not contain the wrapping "{" and "}"
         * characters.
         *
         * @return Returns the level value text.
         */
        public String getContent() {
            return value;
        }

        /**
         * Check if the level is a label.
         *
         * @return Returns true if the level is a label.
         */
        public boolean isLabel() {
            return isLabel;
        }

        @Override
        public String toString() {
            return isLabel ? ("{" + value + "}") : value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            } else if (!(o instanceof Level)) {
                return false;
            }
            Level level = (Level) o;
            return isLabel == level.isLabel && value.equals(level.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(isLabel, value);
        }
    }
}
