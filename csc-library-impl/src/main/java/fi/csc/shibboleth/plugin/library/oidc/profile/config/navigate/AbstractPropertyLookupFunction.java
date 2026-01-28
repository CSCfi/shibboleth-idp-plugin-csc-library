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

import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.profile.context.RelyingPartyContext;
import net.shibboleth.shared.primitive.LoggerFactory;

import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;

/**
 * Abstract class for lookups that are based on relying party identifier.
 */
abstract class AbstractPropertyLookupFunction<T> implements Function<ProfileRequestContext, T> {

	/** Class logger. */
	@Nonnull
	private final Logger log = LoggerFactory.getLogger(AbstractPropertyLookupFunction.class);

	/**
	 * Perform lookup for relying party.
	 * 
	 * @param rpId relying party identifier
	 * @return lookup result
	 */
	abstract protected T doApply(String rpId);

	@Override
	@Nullable
	public T apply(@Nonnull ProfileRequestContext prc) {
		assert prc != null;
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
		return doApply(rpId);
	}

}
