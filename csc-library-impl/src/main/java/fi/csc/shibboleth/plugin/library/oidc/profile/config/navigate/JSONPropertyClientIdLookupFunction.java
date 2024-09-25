/*
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
package fi.csc.shibboleth.plugin.library.oidc.profile.config.navigate;

import java.util.Map;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import net.shibboleth.profile.context.RelyingPartyContext;
import net.shibboleth.shared.primitive.LoggerFactory;

import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;

/**
 * Returns client id to be used with current upstream OP.
 */
@ThreadSafe
public class JSONPropertyClientIdLookupFunction implements Function<ProfileRequestContext, String> {

    /** Class logger. */
    @Nonnull
    private final Logger log = LoggerFactory.getLogger(JSONPropertyClientIdLookupFunction.class);

    /** Lookup strategy to locate the nested ProfileRequestContext. */
    @Nonnull
    private Function<ProfileRequestContext, ProfileRequestContext> profileRequestContextLookupStrategy;

    /** Map of client ids per upstream OP. */
    @Value("#{%{csclib.oidc.upstream.clientIds:null}}")
    private Map<String, String> clientIds;

    @Override
    @Nullable
    public String apply(@Nonnull ProfileRequestContext prc) {

        assert prc != null;
        if (clientIds == null) {
            log.error("Client Id map is null");
            return null;
        }
        RelyingPartyContext rpCtx = prc.getSubcontext(RelyingPartyContext.class);
        if (rpCtx == null) {
            log.error("RelyingPartyContext of nested ProfileRequestContext not found");
            return null;
        }
        String rpId = rpCtx.getRelyingPartyId();
        if (rpId == null || rpId.isBlank()) {
            log.error("relyingPartyId in RelyingPartyContext of nested ProfileRequestContext not blank or null");
            return null;
        }
        String clientId = clientIds.get(rpId);
        return clientId != null ? clientId : clientIds.get("default");
    }

}
