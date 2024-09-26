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

import java.util.HashMap;
import java.util.Map;
import org.opensaml.profile.context.ProfileRequestContext;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.profile.context.RelyingPartyContext;

public class JSONPropertyClientCredentialsLookupFunctionTest {

    private JSONPropertyClientCredentialsLookupFunction function;

    private ProfileRequestContext prc;

    private ProfileRequestContext nestedPrc;

    private AuthenticationContext ac;

    private RelyingPartyContext rpCtx;

    @BeforeMethod
    public void setup() {
        prc = new ProfileRequestContext();
        ac = prc.ensureSubcontext(AuthenticationContext.class);
        nestedPrc = ac.ensureSubcontext(ProfileRequestContext.class);
        rpCtx = nestedPrc.ensureSubcontext(RelyingPartyContext.class);
        rpCtx.setRelyingPartyId("https://issuer1");
        function = new JSONPropertyClientCredentialsLookupFunction();
        Map<String, String> ids = new HashMap<String, String>();
        ids.put("default", "default_secret");
        ids.put("https://issuer1", "issuer1_secret");
        ids.put("https://issuer2", "issuer2_secret");
        function.setClientCredentials(ids);
    }

    @Test
    public void testMatchingSuccess() {
        Assert.assertEquals(function.apply(nestedPrc).getSecret(), "issuer1_secret");
    }

    @Test
    public void testDefaultSuccess() {
        rpCtx.setRelyingPartyId("https://some");
        Assert.assertEquals(function.apply(nestedPrc).getSecret(), "default_secret");
    }

    @Test
    public void testNullMap() {
        function.setClientCredentials(null);
        Assert.assertNull(function.apply(nestedPrc));
    }

}
