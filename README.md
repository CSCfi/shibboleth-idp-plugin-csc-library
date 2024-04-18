# Shibboleth Idp Plugin CSC Library



## Intro

Bean library for CSC IdPs.

## Beans

### csclib.OIDC.AuthenticationContextClassReferenceTranslationStrategyLookupStrategy

Mimics shibboleth project 'ProxyAwareDefaultOIDCAuthenticationContextClassResponseLookupFunction' functionality with the modification that you may have empty key in 'shibboleth.oidc.PrincipalProxyResponseMappings'. If response from upstream OIDC OP contains no ACR claim it is mapped to value of empty key if such exists in.

