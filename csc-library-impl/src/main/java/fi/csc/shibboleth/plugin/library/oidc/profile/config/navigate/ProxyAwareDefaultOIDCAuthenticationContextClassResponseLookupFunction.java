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


import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import org.opensaml.profile.context.ProfileRequestContext;

import net.shibboleth.shared.annotation.ParameterName;
import net.shibboleth.shared.annotation.constraint.NonnullElements;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.logic.Constraint;

/**
 * Implements a set of logic for determining the principals to use from OIDC 'amr' or 'acr' claims.
 * 
 * <p>This operates for the OIDC to SAML proxy use case, in effect the reverse of the 
 * {@link ProxyAwareDefaultOIDCAuthenticationContextClassRequestLookupFunction} function. 
 * All input values are either OIDC ACRs or AMRs, and all output values Java {@link Principal}s.
 * The values are mapped by the given principal mappings. If a mapping does not exist, the AMR or ACR is ignored.
 * </p>
 */
/**
 * Difference to Shibboleth project implementation is that also empty or null input is matched to empty mapping. 
 */
@ThreadSafe
public class ProxyAwareDefaultOIDCAuthenticationContextClassResponseLookupFunction 
    implements Function<Collection<String>,Collection<Principal>> {
    
    /** Mappings to transform proxied Principals. */
    @Nonnull @NonnullElements private final Map<String,Collection<Principal>> principalMappings;
    
    /**
     * 
     * Constructor.
     *
     * @param mappings the AMR/ACR value to Principal mappings
     */
    public ProxyAwareDefaultOIDCAuthenticationContextClassResponseLookupFunction(
            @Nullable @NonnullElements @ParameterName(name="mappings") 
            final Map<String,Collection<Principal>> mappings) {
        
        if (mappings == null || mappings.isEmpty()) {
            principalMappings = CollectionSupport.emptyMap();
        } else {        
            principalMappings = new HashMap<>(mappings.size());
            mappings.forEach((k, v) -> principalMappings.put(k, List.copyOf(v)));
        }
    }

    /** {@inheritDoc} */
    @Nullable public Collection<Principal> apply(final Collection<String> amrOrAcrs) {
        
        if (amrOrAcrs != null && !amrOrAcrs.isEmpty()) {                
                final List<Principal> principals = new ArrayList<>();                
                for (final String amrOrAcr : amrOrAcrs) {                    
                    if (principalMappings.containsKey(amrOrAcr)) {
                        final Collection<Principal> mappedPrincipals = principalMappings.get(amrOrAcr);
                        if (!mappedPrincipals.isEmpty()) {
                            principals.addAll(mappedPrincipals);
                        }
                    }                 
                }
                return principals;            
        } else {
            if (principalMappings.containsKey("")) {
                final List<Principal> principals = new ArrayList<>();
                final Collection<Principal> mappedPrincipals = principalMappings.get("");
                if (!mappedPrincipals.isEmpty()) {
                    principals.addAll(mappedPrincipals);
                }
                return principals;
            }
        }
        return CollectionSupport.emptyList();
    }   
    
    /** A simple lookup function that returns a singleton function.*/
    public static class LookupFunctionWrapper 
                implements Function<ProfileRequestContext, Function<Collection<String>,Collection<Principal>>> {

        /** A function used to map OIDC ACR/AMRs to Principals. A single instance is supplied.*/
        @Nonnull private final Function<Collection<String>,Collection<Principal>> function; 
        
        /**
         * 
         * Constructor.
         *
         * @param wrappedFunction the function to return when requested.          
         */
        public LookupFunctionWrapper(@ParameterName(name="wrappedFunction") 
                final Function<Collection<String>,Collection<Principal>> wrappedFunction) {
            function = Constraint.isNotNull(wrappedFunction, "Lookup function can not be null");
        }
        
        /** {@inheritDoc} */
        @Nullable public Function<Collection<String>, Collection<Principal>> apply(
                @Nullable final ProfileRequestContext prc) {
            return function;
        }
    }

}
