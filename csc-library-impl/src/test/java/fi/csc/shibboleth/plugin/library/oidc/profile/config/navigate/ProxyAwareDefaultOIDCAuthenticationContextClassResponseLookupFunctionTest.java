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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opensaml.profile.context.ProfileRequestContext;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.RequestedPrincipalContext;
import net.shibboleth.idp.saml.authn.principal.AuthenticationMethodPrincipal;
import net.shibboleth.shared.collection.CollectionSupport;

/** Tests for ProxyAwareDefaultOIDCAuthenticationContextClassLookupFunction.*/
@SuppressWarnings("javadoc")
public class ProxyAwareDefaultOIDCAuthenticationContextClassResponseLookupFunctionTest {
    
    private ProxyAwareDefaultOIDCAuthenticationContextClassResponseLookupFunction function;
    
    private ProfileRequestContext prc;
    
    private ProfileRequestContext nestedPrc;
    
    private AuthenticationContext ac;
    
    private RequestedPrincipalContext rpc;
    
    @BeforeMethod
    public void setup() {        
        prc = new ProfileRequestContext();
        ac = prc.ensureSubcontext(AuthenticationContext.class);
        nestedPrc = ac.ensureSubcontext(ProfileRequestContext.class);
        rpc = ac.ensureSubcontext(RequestedPrincipalContext.class);
    }
    
    @Test
    public void testSingleMappingSuccess() {
        
        final Map<String,Collection<Principal>> mappings = new HashMap<>();
        mappings.put(
                "pwd", 
                CollectionSupport.listOf(new AuthenticationMethodPrincipal
                        ("urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport")));

        function = new ProxyAwareDefaultOIDCAuthenticationContextClassResponseLookupFunction(mappings);
        
        final Collection<Principal> mapped = function.apply(CollectionSupport.singletonList("pwd"));
        assertTrue(mapped != null && mapped.contains(
                        new AuthenticationMethodPrincipal
                        ("urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport")));
        
    }
    
    @Test
    public void testSingleMappingSuccess_NotMappedIgnored() {
        
        
        final Map<String,Collection<Principal>> mappings = new HashMap<>();
        mappings.put(
                "pwd", 
                List.of(new AuthenticationMethodPrincipal
                        ("urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport")));
        function = new ProxyAwareDefaultOIDCAuthenticationContextClassResponseLookupFunction(mappings);;
        
        // OTP should not be in the result
        final Collection<Principal> mapped = function.apply(CollectionSupport.listOf("pwd", "otp"));
        assert mapped!= null;
        assertEquals(mapped.size(), 1);
        assertTrue(mapped.contains(new AuthenticationMethodPrincipal
                        ("urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport")));
       
    }
    
    @Test
    public void testSingleMappingSuccess_EmptyInput() {
        
        // OTP should not be in the result
        final Collection<Principal> mapped = function.apply(CollectionSupport.emptyList());
        assert mapped!= null;
        assertEquals(mapped.size(), 0);
    }
    
    @Test
    public void testSingleMappingSuccess_NullMappings() {
        
        function = new ProxyAwareDefaultOIDCAuthenticationContextClassResponseLookupFunction(null);
        
        // OTP should not be in the result
        final Collection<Principal> mapped = function.apply(CollectionSupport.emptyList());
        assert mapped!= null;
        assertEquals(mapped.size(), 0);
    }
    
    @Test
    public void testSingleMappingSuccess_EmptyMapping() {
        
        final Map<String,Collection<Principal>> mappings = new HashMap<>();
        mappings.put("pwd", Collections.emptyList());
        function = new ProxyAwareDefaultOIDCAuthenticationContextClassResponseLookupFunction(mappings);
        
        // OTP should not be in the result
        final Collection<Principal> mapped = function.apply(CollectionSupport.emptyList());
        assert mapped!= null;
        assertEquals(mapped.size(), 0);
    }
    
    /**
     * Difference to Shibboleth project implementation is that also empty or null input is matched to empty mapping. 
     */
    
    @Test
    public void testSingleMappingSuccess_NullKeyedMapping() {
        
        final Map<String,Collection<Principal>> mappings = new HashMap<>();
        mappings.put(
                "", 
                List.of(new AuthenticationMethodPrincipal
                        ("urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport")));
        function = new ProxyAwareDefaultOIDCAuthenticationContextClassResponseLookupFunction(mappings);
        
        // Passport should be in the result
        final Collection<Principal> mapped = function.apply(CollectionSupport.emptyList());
        assert mapped!= null;
        assertEquals(mapped.size(), 1);
        assertTrue(mapped.contains(new AuthenticationMethodPrincipal
                        ("urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport")));
    }


}
