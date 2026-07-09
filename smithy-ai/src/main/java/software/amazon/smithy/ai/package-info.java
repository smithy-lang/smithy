/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * Discovery API for Smithy AI agent skills bundled on the classpath.
 *
 * <p>Content lives under {@code META-INF/smithy-ai/skills/} in each contributing JAR, with a
 * companion {@code skills.index} listing every file leaf. {@link
 * software.amazon.smithy.ai.AiContent} enumerates the indexes across all JARs and returns immutable
 * {@link software.amazon.smithy.ai.AiSkill} handles.
 *
 * <p>The mechanism is deliberately identical to {@code ModelDiscovery}'s treatment of
 * {@code META-INF/smithy/manifest}: a JAR cannot list a directory at runtime, so the set of
 * bundled things and the files inside each one are captured at build time.
 */
package software.amazon.smithy.ai;
