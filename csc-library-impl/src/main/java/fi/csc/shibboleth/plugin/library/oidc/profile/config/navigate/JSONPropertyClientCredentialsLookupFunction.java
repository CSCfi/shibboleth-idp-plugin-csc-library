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
import javax.annotation.concurrent.ThreadSafe;

import net.shibboleth.oidc.security.credential.ClientSecretCredential;
import net.shibboleth.oidc.security.credential.DefaultClientSecretCredential;
import net.shibboleth.profile.context.RelyingPartyContext;
import net.shibboleth.shared.primitive.LoggerFactory;

import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;

/**
 * Returns client secret to be used with current upstream OP.
 */
@ThreadSafe
public class JSONPropertyClientCredentialsLookupFunction
        implements Function<ProfileRequestContext, ClientSecretCredential> {

    /** Class logger. */
    @Nonnull
    private final Logger log = LoggerFactory.getLogger(JSONPropertyClientCredentialsLookupFunction.class);

    /** Map of client credentials per upstream OP. */
    @Value("#{%{csclib.oidc.upstream.clientCredentials:null}}")
    private Map<String, String> clientCredentials;

    /**
     * Set map of client credentials per upstream OP.
     * 
     * @param credentials Map of client credentials per upstream OP
     */
    public void setClientCredentials(Map<String, String> credentials) {
        clientCredentials = credentials;
    }

    @Override
    public ClientSecretCredential apply(@Nonnull ProfileRequestContext prc) {

        assert prc != null;
        if (clientCredentials == null) {
            log.error("Client Credentials map is null");
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
        String clientCredential = clientCredentials.get(rpId);
        return clientCredential != null ? new DefaultClientSecretCredential(clientCredential)
                : new DefaultClientSecretCredential(clientCredentials.get("default"));
    }

}
