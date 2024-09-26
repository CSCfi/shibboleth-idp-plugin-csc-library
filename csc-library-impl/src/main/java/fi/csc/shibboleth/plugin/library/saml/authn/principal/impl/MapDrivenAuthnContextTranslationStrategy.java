package fi.csc.shibboleth.plugin.library.saml.authn.principal.impl;

import java.security.Principal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.saml.saml2.core.AuthnContext;
import org.opensaml.saml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml.saml2.core.AuthnContextDeclRef;
import org.slf4j.Logger;

import net.shibboleth.idp.saml.authn.principal.AuthnContextClassRefPrincipal;
import net.shibboleth.idp.saml.authn.principal.AuthnContextDeclRefPrincipal;
import net.shibboleth.shared.annotation.constraint.NotLive;
import net.shibboleth.shared.annotation.constraint.Unmodifiable;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.primitive.LoggerFactory;

/**
 * Implements a set of default logic for mapping an {@link AuthnContext}'s content into a set of
 * custom Principals based on a set of static mapping rules.
 * 
 * @since 4.0.0
 */
/**
 * Difference to Shibboleth project implementation is that to input without mappings a default mapping is applied to.
 */
public class MapDrivenAuthnContextTranslationStrategy implements Function<AuthnContext,Collection<Principal>> {
    
    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(MapDrivenAuthnContextTranslationStrategy.class);
    
    /** Key for response mapping to be used for unmapped ACRs. */
    @Nonnull public final static AuthnContextClassRefPrincipal DEFAULT = new AuthnContextClassRefPrincipal("csclib.UpstreamACR.Default");
    
    /** Mappings to transform proxied Principals. */
    @Nonnull private Map<Principal,Collection<Principal>> principalMappings;
    
    /** Constructor. */
    public MapDrivenAuthnContextTranslationStrategy() {
        principalMappings = CollectionSupport.emptyMap();
    }
    
    /**
     * Sets the mappings from input/proxied Principals to zero or more equivalent values to use.
     * 
     * <p>Any values not mapped will be assumed to be passed through.</p>
     * 
     * @param mappings {@link Principal} mappings
     */
    public void setMappings(@Nullable final Map<Principal,Collection<Principal>> mappings) {
        if (mappings == null || mappings.isEmpty()) {
            principalMappings = CollectionSupport.emptyMap();
            return;
        }
        
        principalMappings = new HashMap<>(mappings.size());
        mappings.forEach((k, v) -> principalMappings.put(k, List.copyOf(v)));
    }
    
    /** {@inheritDoc} */
    @Nullable @Unmodifiable @NotLive public Collection<Principal> apply(@Nullable final AuthnContext input) {
        
        if (input != null) {
            final Principal principal;
            final AuthnContextClassRef classRef = input.getAuthnContextClassRef();
            final String classRefURI = classRef == null ? null : classRef.getURI();
            final AuthnContextDeclRef declRef = input.getAuthnContextDeclRef();
            final String declRefURI = declRef == null ? null : declRef.getURI();
            
            if (classRefURI != null) {
                principal = new AuthnContextClassRefPrincipal(classRefURI);
            } else if (declRefURI != null) {
                principal = new AuthnContextDeclRefPrincipal(declRefURI);
            } else {
                log.trace("Input AuthnContext did not contain a class or decl reference, returning nothing");
                return null;
            }
            
            if (principalMappings.containsKey(principal)) {
                final Collection<Principal> mapped = principalMappings.get(principal);
                if (log.isTraceEnabled()) {
                    log.trace("Mapped '{}' to ", principal.getName(),
                            mapped.stream().map(Principal::getName).collect(Collectors.toUnmodifiableList()));
                }
                return mapped;
            } else if (principalMappings.containsKey(DEFAULT)) {
                final Collection<Principal> mapped = principalMappings.get(principal);
                if (log.isTraceEnabled()) {
                    log.trace("Mapped '{}' to ", principal.getName(),
                            mapped.stream().map(Principal::getName).collect(Collectors.toUnmodifiableList()));
                }
                return mapped;
            }

            log.trace("Passing unmapped value '{}' through", principal.getName());
            return CollectionSupport.singletonList(principal);
        }
        
        log.trace("Input AuthnContext was null, returning nothing");
        return null;
    }

}