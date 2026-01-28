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

import org.springframework.test.context.TestPropertySource;
import org.testng.Assert;
import org.testng.annotations.Test;

@TestPropertySource(properties = {
		"csclib.oidc.upstream.scopes={\"default\":\"openid\",\"https://issuer1\":\" openid    email \", \"https://issuer2\":\"\"}" })
public class PropertyScopesLookupFunctionTest
		extends AbstractPropertyLookupFunctionTest<PropertyScopesLookupFunction> {

	@Test
	public void testMatchingSuccess() {
		Assert.assertTrue(function.apply(nestedPrc).size() == 2);
		Assert.assertTrue(function.apply(nestedPrc).contains("openid"));
		Assert.assertTrue(function.apply(nestedPrc).contains("email"));
	}

	@Test
	public void testDefaultSuccess() {
		rpCtx.setRelyingPartyId("https://some");
		Assert.assertTrue(function.apply(nestedPrc).size() == 1);
		Assert.assertTrue(function.apply(nestedPrc).contains("openid"));
	}

	@Test
	public void testEmptySuccess() {
		rpCtx.setRelyingPartyId("https://issuer2");
		Assert.assertTrue(function.apply(nestedPrc).size() == 1);
		Assert.assertTrue(function.apply(nestedPrc).contains(""));
	}
}
