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

import org.opensaml.profile.context.ProfileRequestContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeMethod;

import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.profile.context.RelyingPartyContext;

@ContextConfiguration(locations = { "classpath*:/META-INF/net.shibboleth.idp/JSONLookupPostconfig.xml", })
public abstract class AbstractPropertyLookupFunctionTest<T> extends AbstractTestNGSpringContextTests {

	@Autowired
	protected T function;

	protected ProfileRequestContext prc;

	protected ProfileRequestContext nestedPrc;

	protected AuthenticationContext ac;

	protected RelyingPartyContext rpCtx;

	@BeforeMethod
	public void setup() {
		prc = new ProfileRequestContext();
		ac = prc.ensureSubcontext(AuthenticationContext.class);
		nestedPrc = ac.ensureSubcontext(ProfileRequestContext.class);
		rpCtx = nestedPrc.ensureSubcontext(RelyingPartyContext.class);
		rpCtx.setRelyingPartyId("https://issuer1");
	}

}
