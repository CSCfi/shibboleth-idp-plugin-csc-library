/*
 * Copyright (c) 2026 CSC- IT Center for Science, www.csc.fi
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fi.csc.shibboleth.plugin.library.inform.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.context.AttributeContext;

/**
 * Default event content lookup to serve our specific needs. Each service should
 * create their own lookup.
 */
public class DefaultCSCIdMEventContentLookupStrategy
        implements BiFunction<AttributeContext, Map<String, Object>, String> {

    /** Class logger. */
    @Nonnull
    private final Logger log = LoggerFactory.getLogger(DefaultCSCIdMEventContentLookupStrategy.class);

    /** Object mapper for parsing. */
    private static ObjectMapper objectMapper = new ObjectMapper();

    /** Attribute ids matching the fieds IdM requires. */
    @Nullable
    private Map<String, String> attributeNames;

    @Value("%{csclib.idmEventAttributes:}")
    public void setAttributeNames(String input) {
        if (input == null || input.isBlank()) {
            return;
        }
        try {
            attributeNames = objectMapper.readValue(input, new TypeReference<Map<String, String>>() {
            });
        } catch (JsonProcessingException e) {
            log.error("DefaultCSCIdMEventContentLookupStrategy failed parsing 'csclib.idmEventAttributes' - map");
        }
    }

    @Override
    public String apply(@Nonnull AttributeContext attributeContext, @Nonnull Map<String, Object> baseEvent) {

        assert attributeContext != null;
        assert baseEvent != null;
        final Map<String, Object> cscIdmEvent = new HashMap<>();

        if (attributeNames == null || attributeNames.isEmpty()) {
            log.error(
                    "DefaultCSCIdMEventContentLookupStrategy requires you to define 'csclib.idmEventAttributes' - map");
            return null;
        }

        if (baseEvent.get("acr") instanceof String acr) {
            cscIdmEvent.put("acr", acr);
        } else {
            log.warn("Not able to inform authentication event as mandatory 'acr' is not available");
            return null;
        }

        if (baseEvent.get("timestampSeconds") instanceof Long time) {
            cscIdmEvent.put("time", time);
        } else {
            log.warn("Not able to inform authentication event as mandatory 'time' is not available");
            return null;
        }

        if (baseEvent.get("serviceId") instanceof String cscServiceId) {
            cscIdmEvent.put("cscServiceId", cscServiceId);
        } else {
            log.warn("Not able to inform authentication event as mandatory 'cscServiceId' is not available");
            return null;
        }

        if (baseEvent.get("serviceName") instanceof String cscServiceName) {
            cscIdmEvent.put("cscServiceName", cscServiceName);
        } else {
            log.warn("Not able to inform authentication event as mandatory 'cscServiceName' is not available");
            return null;
        }

        if (baseEvent.get("remoteAddress") instanceof String remoteUserIPAddress) {
            cscIdmEvent.put("remoteUserIPAddress", remoteUserIPAddress);
        }

        Map<String, IdPAttribute> attributes = attributeContext.getUnfilteredIdPAttributes();
        IdPAttribute attribute = attributes.get(attributeNames.get("cscUserName"));
        if (attribute != null) {
            cscIdmEvent.put("cscUserName", attribute.getValues().get(0).getDisplayValue());
        }

        attribute = attributes.get(attributeNames.get("remoteUserIdentifier"));
        if (attribute != null) {
            cscIdmEvent.put("remoteUserIdentifier", attribute.getValues().get(0).getDisplayValue());
        }

        attribute = attributes.get(attributeNames.get("eduPersonScopedAffiliation"));
        if (attribute != null) {
            List<String> values = new ArrayList<>();
            attribute.getValues().forEach(value -> values.add(value.getDisplayValue()));
            cscIdmEvent.put("eduPersonScopedAffiliation", values);
        }

        attribute = attributes.get(attributeNames.get("schacCountryOfCitizenship"));
        if (attribute != null) {
            cscIdmEvent.put("schacCountryOfCitizenship", attribute.getValues().get(0).getDisplayValue());
        }

        attribute = attributes.get(attributeNames.get("givenName"));
        if (attribute != null) {
            cscIdmEvent.put("givenName", attribute.getValues().get(0).getDisplayValue());
        }

        attribute = attributes.get(attributeNames.get("surname"));
        if (attribute != null) {
            cscIdmEvent.put("surname", attribute.getValues().get(0).getDisplayValue());
        }

        attribute = attributes.get(attributeNames.get("mail"));
        if (attribute != null) {
            cscIdmEvent.put("mail", attribute.getValues().get(0).getDisplayValue());
        }

        try {
            return objectMapper.writeValueAsString(cscIdmEvent);
        } catch (JsonProcessingException e) {
            log.error("Failed writing the event as json string", e);
            return null;
        }
    }
}
