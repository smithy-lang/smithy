/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.model.knowledge;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.traits.Trait;

/**
 * Defines an HTTP message member binding.
 */
public final class HttpBinding {

    private final MemberShape member;
    private final Location location;
    private final String locationName;
    private final Trait bindingTrait;

    HttpBinding(MemberShape member, Location location, String locationName, Trait bindingTrait) {
        this.member = Objects.requireNonNull(member);
        this.location = Objects.requireNonNull(location);
        this.locationName = Objects.requireNonNull(locationName);
        this.bindingTrait = bindingTrait;
    }

    /** HTTP binding types. */
    public enum Location { LABEL, DOCUMENT, PAYLOAD, HEADER, PREFIX_HEADERS, QUERY, QUERY_PARAMS, RESPONSE_CODE,
        UNBOUND }

    public MemberShape getMember() {
        return member;
    }

    public String getMemberName() {
        return member.getMemberName();
    }

    public Location getLocation() {
        return location;
    }

    public String getLocationName() {
        return locationName;
    }

    public Optional<Trait> getBindingTrait() {
        return Optional.ofNullable(bindingTrait);
    }

    @Override
    public String toString() {
        return member.getId() + " @ " + location.toString().toLowerCase(Locale.US) + " (" + locationName + ")";
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof HttpBinding)) {
            return false;
        } else {
            HttpBinding otherBinding = (HttpBinding) other;
            return getMember().equals(otherBinding.getMember())
                   && getLocation() == otherBinding.getLocation()
                   && getLocationName().equals(otherBinding.getLocationName());
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(location, member, locationName);
    }
}
