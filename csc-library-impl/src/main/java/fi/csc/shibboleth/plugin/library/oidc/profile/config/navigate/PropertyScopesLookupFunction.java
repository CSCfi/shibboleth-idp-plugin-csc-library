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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import net.shibboleth.shared.primitive.LoggerFactory;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;

/**
 * Returns scopes to be used with current upstream OP.
 */
@ThreadSafe
public class PropertyScopesLookupFunction extends AbstractPropertyLookupFunction<Set<String>> {

	/** Class logger. */
	@Nonnull
	private final Logger log = LoggerFactory.getLogger(PropertyScopesLookupFunction.class);

	/** Map of scopes per upstream OP. */
	@Value("#{%{csclib.oidc.upstream.scopes:null}}")
	private Map<String, String> scopes;

	@Override
	protected Set<String> doApply(String rpId) {
		if (scopes == null) {
			return null;
		}
		String rpScopes = scopes.get(rpId);
		rpScopes = rpScopes != null ? rpScopes : scopes.get("default");
		return rpScopes != null ? new HashSet<String>(Arrays.asList(rpScopes.trim().split("\\s+"))) : null;
	}

}
