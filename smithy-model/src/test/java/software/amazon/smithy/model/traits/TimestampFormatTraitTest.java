/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

public class TimestampFormatTraitTest {
    @Test
    public void createsFromString() {
        assertThat(TimestampFormatTrait.Format.fromString("date-time"),
                equalTo(TimestampFormatTrait.Format.DATE_TIME));
        assertThat(TimestampFormatTrait.Format.fromString("http-date"),
                equalTo(TimestampFormatTrait.Format.HTTP_DATE));
        assertThat(TimestampFormatTrait.Format.fromString("epoch-seconds"),
                equalTo(TimestampFormatTrait.Format.EPOCH_SECONDS));
        assertThat(TimestampFormatTrait.Format.fromString("foo-baz"),
                equalTo(TimestampFormatTrait.Format.UNKNOWN));
    }

    @Test
    public void convertsFormatToString() {
        assertThat(TimestampFormatTrait.Format.fromString("date-time").toString(),
                equalTo("date-time"));
    }

    @Test
    public void createsFormatFromTrait() {
        TimestampFormatTrait trait = new TimestampFormatTrait("date-time");

        assertThat(trait.getFormat(), equalTo(TimestampFormatTrait.Format.DATE_TIME));
    }
}
