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

import java.security.Principal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.saml2.core.AuthnContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;
import net.shibboleth.idp.attribute.context.AttributeContext;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.RequestedPrincipalContext;
import net.shibboleth.idp.authn.impl.DefaultPrincipalDeterminationStrategy;
import net.shibboleth.idp.plugin.oidc.op.messaging.context.OIDCAuthenticationResponseContext;
import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.saml.authn.principal.AuthnContextClassRefPrincipal;
import net.shibboleth.idp.saml.authn.principal.AuthnContextDeclRefPrincipal;
import net.shibboleth.idp.ui.context.RelyingPartyUIContext;
import net.shibboleth.profile.context.RelyingPartyContext;
import net.shibboleth.shared.annotation.constraint.NonnullAfterInit;
import net.shibboleth.shared.annotation.constraint.NonnullBeforeExec;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.servlet.HttpServletSupport;

public class AddEvent extends AbstractProfileAction {

    /** Class logger. */
    @Nonnull
    private final Logger log = LoggerFactory.getLogger(AddEvent.class);

    /**
     * Strategy used to locate the {@link RelyingPartyContext}.
     */
    @NonnullBeforeExec
    private BiFunction<AttributeContext, Map<String, Object>, String> eventContentLookupStrategy;

    /**
     * Strategy used to locate the {@link RelyingPartyContext}.
     */
    @NonnullBeforeExec
    private Function<ProfileRequestContext, RelyingPartyContext> relyingPartyContextLookupStrategy;

    /** Relying party Identifier. */
    @NonnullBeforeExec
    private String rpId;

    /**
     * Strategy used to locate the {@link RelyingPartyUIContext} associated with a
     * given {@link ProfileRequestContext}.
     */
    @NonnullBeforeExec
    private Function<ProfileRequestContext, RelyingPartyUIContext> relyingPartyUIContextLookupStrategy;

    /** Event buffer. */
    @NonnullBeforeExec
    private EventStore eventStore;

    /** Strategy used to obtain the client Address. */
    @NonnullAfterInit
    private Function<ProfileRequestContext, String> addressLookupStrategy;

    /** Relying party ui context. */
    @NonnullBeforeExec
    private RelyingPartyUIContext rpUIContext;

    /** ACR. */
    @NonnullBeforeExec
    private String acr;

    /** Attribute context to resolve clamis for event store item. */
    @NonnullBeforeExec
    private AttributeContext attributeCtx;

    /**
     * Strategy used to extract, and create if necessary, the
     * {@link AuthenticationContext} from the {@link ProfileRequestContext}.
     */
    @Nonnull
    private Function<ProfileRequestContext, AuthenticationContext> authnCtxLookupStrategy;

    /** Strategy used to determine the AuthnContextClassRef. */
    @NonnullAfterInit
    private Function<ProfileRequestContext, AuthnContextClassRefPrincipal> classRefLookupStrategy;

    /**
     * Strategy used to locate or create the {@link AttributeContext} to populate.
     */
    @Nonnull
    private Function<ProfileRequestContext, AttributeContext> attributeContextCreationStrategy;

    /** Constructor. */
    protected AddEvent() {
        super();
        eventContentLookupStrategy = new DefaultCSCIdMEventContentLookupStrategy();
        relyingPartyContextLookupStrategy = new ChildContextLookup<>(RelyingPartyContext.class);
        attributeContextCreationStrategy = new ChildContextLookup<>(AttributeContext.class, true)
                .compose(new ChildContextLookup<>(RelyingPartyContext.class));
        relyingPartyUIContextLookupStrategy = new ChildContextLookup<>(RelyingPartyUIContext.class)
                .compose(new ChildContextLookup<>(AuthenticationContext.class));
        authnCtxLookupStrategy = new ChildContextLookup<>(AuthenticationContext.class);
    }

    /**
     * Set the strategy used to return {@link RelyingPartyContext} .
     * 
     * @param strategy lookup strategy
     */
    public void setEventContentLookupStrategy(
            @Nonnull final BiFunction<AttributeContext, Map<String, Object>, String> strategy) {
        checkSetterPreconditions();
        eventContentLookupStrategy = Constraint.isNotNull(strategy, "EventContent lookup strategy cannot be null");
    }

    /**
     * Set the strategy used to return {@link RelyingPartyContext} .
     * 
     * @param strategy lookup strategy
     */
    public void setRelyingPartyContextLookup(
            @Nonnull final Function<ProfileRequestContext, RelyingPartyContext> strategy) {
        checkSetterPreconditions();
        relyingPartyContextLookupStrategy = Constraint.isNotNull(strategy,
                "RelyingPartyContext lookup strategy cannot be null");
    }

    /**
     * Set the strategy used to return {@link RelyingPartyContext} .
     * 
     * @param strategy lookup strategy
     */
    public void setRelyingPartyUIContextLookup(
            @Nonnull final Function<ProfileRequestContext, RelyingPartyUIContext> strategy) {
        checkSetterPreconditions();
        relyingPartyUIContextLookupStrategy = Constraint.isNotNull(strategy,
                "RelyingPartyUIContext lookup strategy cannot be null");
    }

    /**
     * Set the strategy used to locate or create the {@link AttributeContext} to
     * populate.
     * 
     * @param strategy lookup/creation strategy
     */
    public void setAttributeContextCreationStrategy(
            @Nonnull final Function<ProfileRequestContext, AttributeContext> strategy) {
        checkSetterPreconditions();
        attributeContextCreationStrategy = Constraint.isNotNull(strategy,
                "AttributeContext creation strategy cannot be null");
    }

    /**
     * Set event store.
     * 
     * @param store Event store.
     */
    public void setEventStore(@Nonnull EventStore store) {
        checkSetterPreconditions();
        eventStore = Constraint.isNotNull(store, "Event Store cannot be null");
    }

    /**
     * Set the strategy used to obtain the client IP address.
     * 
     * @param strategy lookup strategy
     * 
     */
    public void setAddressLookupStrategy(@Nonnull final Function<ProfileRequestContext, String> strategy) {
        checkSetterPreconditions();
        addressLookupStrategy = Constraint.isNotNull(strategy, "Strategy cannot be null");
    }

    /**
     * Set the context lookup strategy.
     * 
     * @param strategy lookup strategy function for {@link AuthenticationContext}.
     */
    public void setAuthenticationContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext, AuthenticationContext> strategy) {
        checkSetterPreconditions();

        authnCtxLookupStrategy = Constraint.isNotNull(strategy, "Strategy cannot be null");

    }

    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        if (eventStore == null) {
            throw new ComponentInitializationException("Event store cannot be null");
        }
        if (addressLookupStrategy == null) {
            addressLookupStrategy = new RemoteAddressStrategy();
        }
        if (classRefLookupStrategy == null) {
            classRefLookupStrategy = new DefaultPrincipalDeterminationStrategy<>(AuthnContextClassRefPrincipal.class,
                    new AuthnContextClassRefPrincipal(AuthnContext.UNSPECIFIED_AUTHN_CTX));
        }
    }

    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext) {

        if (!super.doPreExecute(profileRequestContext)) {
            return false;
        }
        RelyingPartyContext rpCtx = relyingPartyContextLookupStrategy.apply(profileRequestContext);
        if (rpCtx == null) {
            log.warn("{} No RelyingPartyContext available. Nothing to do.", getLogPrefix());
            return false;
        }
        rpId = rpCtx.getRelyingPartyId();
        if (rpId == null) {
            log.warn("{} No Relying Party Identifier available. Nothing to do.", getLogPrefix());
            return false;
        }
        rpUIContext = relyingPartyUIContextLookupStrategy.apply(profileRequestContext);
        if (rpUIContext == null) {
            log.warn("{} Unable to locate relying party ui context", getLogPrefix());
            return false;
        }
        final MessageContext outboundMessageCtx = profileRequestContext.getOutboundMessageContext();
        if (outboundMessageCtx == null) {
            log.warn("{} No outbound message context", getLogPrefix());
            return false;
        }
        OIDCAuthenticationResponseContext oidcResponseContext = outboundMessageCtx
                .getSubcontext(OIDCAuthenticationResponseContext.class);
        if (oidcResponseContext != null) {
            acr = oidcResponseContext.getAcr() != null ? oidcResponseContext.getAcr().getValue() : null;
        } else {
            AuthenticationContext authnContext = authnCtxLookupStrategy.apply(profileRequestContext);
            if (authnContext == null) {
                log.warn("{} No authentication context class", getLogPrefix());
                return false;
            }
            RequestedPrincipalContext requestedPrincipalContext = authnContext
                    .getSubcontext(RequestedPrincipalContext.class);
            if (requestedPrincipalContext != null && requestedPrincipalContext.getMatchingPrincipal() != null) {
                final Principal matchingPrincipal = requestedPrincipalContext.getMatchingPrincipal();
                if (matchingPrincipal instanceof AuthnContextClassRefPrincipal) {
                    acr = ((AuthnContextClassRefPrincipal) matchingPrincipal).getAuthnContextClassRef().getURI();
                } else if (matchingPrincipal instanceof AuthnContextDeclRefPrincipal) {
                    acr = ((AuthnContextDeclRefPrincipal) matchingPrincipal).getAuthnContextDeclRef().getURI();
                } else {
                    acr = classRefLookupStrategy.apply(profileRequestContext).getAuthnContextClassRef().getURI();
                }
            } else {
                acr = classRefLookupStrategy.apply(profileRequestContext).getAuthnContextClassRef().getURI();
            }
        }
        if (acr == null) {
            log.warn("{} No ACR resolved", getLogPrefix());
            return false;
        }
        attributeCtx = attributeContextCreationStrategy.apply(profileRequestContext);
        if (null == attributeCtx) {
            log.warn("{} Unable to locate/create AttributeContext. Nothing to do.", getLogPrefix());
            return false;
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {

        final Map<String, Object> payload = new HashMap<>();
        payload.put("acr", acr);
        payload.put("timestampSeconds", Instant.now().getEpochSecond());
        payload.put("serviceId", rpId);
        payload.put("serviceName", rpUIContext.getServiceName());
        payload.put("remoteAddress", addressLookupStrategy.apply(profileRequestContext));
        String eventContent = eventContentLookupStrategy.apply(attributeCtx, payload);
        if (eventContent != null) {
            log.debug("{} Adding item '{}' to buffer", getLogPrefix(), eventContent);
            eventStore.addItemToBuffer(eventContent);
        }
    }

    /**
     * Default strategy for obtaining client address from servlet layer.
     */
    private class RemoteAddressStrategy implements Function<ProfileRequestContext, String> {

        /** {@inheritDoc} */
        @Nullable
        public String apply(@Nullable final ProfileRequestContext t) {
            final HttpServletRequest req = getHttpServletRequest();
            if (req != null) {
                return HttpServletSupport.getRemoteAddr(req);
            }

            return null;
        }
    }

}