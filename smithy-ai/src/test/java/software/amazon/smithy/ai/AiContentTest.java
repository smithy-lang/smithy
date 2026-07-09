/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.ai;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

public class AiContentTest {

    private ClassLoader loader() {
        return AiContentTest.class.getClassLoader();
    }

    @Test
    public void discoversBundledSmithyDocsNavigator() {
        List<AiSkill> skills = AiContent.skills(loader());
        AiSkill nav = skills.stream()
                .filter(s -> s.getName().equals("smithy-docs-navigator"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("smithy-docs-navigator not discovered; got: " + skills));
        assertThat(nav.getFiles(), hasItem("SKILL.md"));
    }

    @Test
    public void readsSkillFileContent() {
        AiSkill nav = AiContent.skill(loader(), "smithy-docs-navigator").orElseThrow(AssertionError::new);
        String body = nav.readFile("SKILL.md");
        // Front-matter is authoritative content and is unlikely to change; if it does, this test
        // is a canary for the move being wired wrong.
        assertThat(body, containsString("name: smithy-docs-navigator"));
    }

    @Test
    public void readFilesReturnsEveryEntry() {
        AiSkill nav = AiContent.skill(loader(), "smithy-docs-navigator").orElseThrow(AssertionError::new);
        assertThat(nav.readFiles().keySet(), equalTo(new java.util.LinkedHashSet<>(nav.getFiles())));
    }

    @Test
    public void unknownRelativeFileFailsFast() {
        AiSkill nav = AiContent.skill(loader(), "smithy-docs-navigator").orElseThrow(AssertionError::new);
        AiContentException ex = assertThrows(AiContentException.class, () -> nav.readFile("nope.md"));
        assertThat(ex.getMessage(), containsString("nope.md"));
    }

    @Test
    public void unknownSkillIsEmpty() {
        Optional<AiSkill> missing = AiContent.skill(loader(), "does-not-exist");
        assertTrue(missing.isEmpty());
    }

    @Test
    public void resourcePathPointsUnderMetaInfPrefix() {
        AiSkill nav = AiContent.skill(loader(), "smithy-docs-navigator").orElseThrow(AssertionError::new);
        assertThat(nav.resourcePath("SKILL.md"),
                equalTo("META-INF/smithy-ai/skills/smithy-docs-navigator/SKILL.md"));
    }
}
