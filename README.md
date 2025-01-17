# Shibboleth Idp Plugin CSC Library

## Intro

Bean library for CSC IdPs.

## Beans

### csclib.OIDC.AuthenticationContextClassReferenceTranslationStrategyLookupStrategy

Has enhancements to shibboleth projects standard OIDC response translation strategy. You may have default translations and translate nonexistent ACR responses.  

Activation of functionality in relying party:  

```
<bean parent="OIDC.SSO" p:clientIdLookupStrategy-ref="csclib.OIDC.UpstreamClientIdLookupStrategy"... 
```

New translations key `csclib.UpstreamACR.Default` is applied to upstream ACRs that do not have translation key.   
```
    <util:map id="shibboleth.oidc.PrincipalProxyResponseMappings">. 
        <entry key="csclib.UpstreamACR.Default">. 
            <list>. 
                <bean parent="shibboleth.OIDCAuthnContextClassReference". 
                   c:classRef="https://downstream.com/FederationX" />. 
                <bean parent="shibboleth.SAML2AuthnContextClassRef". 
                   c:classRef="https://downstream.com/FederationX" />. 
              </list>. 
            </entry>. 
    </util:map>. 
``` 

Empty translation key `""` is applied to when upstream does not include ACR claim in Id Token.  
```
    <util:map id="shibboleth.oidc.PrincipalProxyResponseMappings">. 
        <entry key="">. 
            <list>. 
                <bean parent="shibboleth.OIDCAuthnContextClassReference". 
                   c:classRef="https://downstream.com/FederationX" />. 
                <bean parent="shibboleth.SAML2AuthnContextClassRef". 
                   c:classRef="https://downstream.com/FederationX" />. 
              </list>. 
            </entry>. 
    </util:map>. 
```
### csclib.OIDC.UpstreamClientIdLookupStrategy

Bean that resolves OIDC RP client id per upstream issuer from property `csclib.oidc.upstream.clientIds`.

Activation of functionality in relying party:

```
<bean parent="OIDC.SSO" p:clientIdLookupStrategy-ref="csclib.OIDC.UpstreamClientIdLookupStrategy"..
```

Property value example:

```
csclib.oidc.upstream.clientIds = {"default": "default_clientId", "https://upstreamOP1.com", "upstreamOP1_clientId", "https://upstreamOP2.com", "upstreamOP2_clientId"}
```

### csclib.OIDC.UpstreamClientCredentialsLookupStrategy

Bean that resolves OIDC RP client secret per upstream issuer from property `csclib.oidc.upstream.clientCredentials`. Only string type of credentials are supported.


Activation of functionality in relying party:

```
<bean parent="OIDC.SSO" p:clientCredentialLookupStrategy-ref="csclib.OIDC.UpstreamClientCredentialsLookupStrategy"..
```

Property value example:

```
csclib.oidc.upstream.clientCredentials = {"default": "default_clientSecret", "https://upstreamOP1.com", "upstreamOP1_Secret", "https://upstreamOP2.com", "upstreamOP2_Secret"}
```

### csclib.SAML.MapDrivenAuthnContextTranslationStrategy

Has enhancement to shibboleth projects standard SAML2 translation strategy. You may have default translations.

Activation of functionality in relying party:

```
<bean parent="SAML2.SSO" p:authnContextTranslationStrategy-ref="csclib.SAML.MapDrivenAuthnContextTranslationStrategy"..
```

New translations key `<bean parent="shibboleth.SAML2AuthnContextClassRef" c:classRef="csclib.UpstreamACR.Default" />` is applied to upstream ACRs that do not have translation key. 

```
    <util:map id="shibboleth.PrincipalProxyResponseMappings">. 
        <entry>. 
            <key>. 
                <bean parent="shibboleth.SAML2AuthnContextClassRef". 
                   c:classRef="csclib.UpstreamACR.Default" />. 
            </key>. 
            <list>. 
                <bean parent="shibboleth.OIDCAuthnContextClassReference". 
                   c:classRef="https://downstream.com/FederationX" />. 
                <bean parent="shibboleth.SAML2AuthnContextClassRef". 
                   c:classRef="https://downstream.com/FederationX" />. 
              </list>. 
            </entry>. 
    </util:map>. 
```

## Filters

### SLF4JMDCTraceParentServletFilter

Filter that populates MDC keys csclib.traceparent_traceid, csclib.traceparent_parentid and csclib.traceparent_traceflags per traceparent header or request parameter. Filter can be disabled in  csc-library.properties.
```
csclib.logging.MDC.enabled = true
```

## 3rd party libraries

### logstash-logback-encoder version 8.0

We provide the library via this plugin until we run into too much inconvenience.

  