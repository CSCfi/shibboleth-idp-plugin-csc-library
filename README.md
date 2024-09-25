# Shibboleth Idp Plugin CSC Library



## Intro

Bean library for CSC IdPs.

## Beans

### csclib.OIDC.AuthenticationContextClassReferenceTranslationStrategyLookupStrategy

Mimics shibboleth project 'ProxyAwareDefaultOIDCAuthenticationContextClassResponseLookupFunction' functionality with the enhancement that you may map also non existent ACR values to values having empty key in 'shibboleth.oidc.PrincipalProxyResponseMappings'. 

### csclib.OIDC.UpstreamClientIdLookupStrategy

Bean that resolves OIDC RP client id per upstream issuer from property `csclib.oidc.upstream.clientIds`.

`csclib.oidc.upstream.clientIds = {"default": "default_clientId", "https://upstreamOP1.com", "upstreamOP1_clientId", "https://upstreamOP2.com", "upstreamOP2_clientId"}`

Activation of functionality in relying party:

`<bean parent="OIDC.SSO" p:clientIdLookupStrategy-ref="csclib.OIDC.UpstreamClientIdLookupStrategy"..`

### csclib.OIDC.UpstreamClientCredentialsLookupStrategy

Bean that resolves OIDC RP client secret per upstream issuer from property `csclib.oidc.upstream.clientCredentials`.

`{"default": "default_clientSecret", "https://upstreamOP1.com", "upstreamOP1_Secret", "https://upstreamOP2.com", "upstreamOP2_Secret"}`

Activation of functionality in relying party:

`<bean parent="OIDC.SSO" p:clientCredentialLookupStrategy-ref="csclib.OIDC.UpstreamClientCredentialsLookupStrategy"..`

