# Shibboleth Idp Plugin CSC Library



## Intro

Bean library for CSC IdPs.

## Beans

### csclib.OIDC.AuthenticationContextClassReferenceTranslationStrategyLookupStrategy

Mimics shibboleth project 'ProxyAwareDefaultOIDCAuthenticationContextClassResponseLookupFunction' functionality with the enhancement that you may map also non existent ACR values to values having empty key in 'shibboleth.oidc.PrincipalProxyResponseMappings'. 

