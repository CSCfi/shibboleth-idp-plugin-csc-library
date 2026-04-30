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
### csclib.OIDC.UpstreamScopesLookupStrategy

Bean that resolves scope value per upstream issuer from property `csclib.oidc.upstream.scopes`.


Activation of functionality in relying party:

```
<bean parent="OIDC.SSO" p:scopesLookupStrategy-ref="csclib.OIDC.UpstreamScopesLookupStrategy"..
```

Property value example:

```
csclib.oidc.upstream.scopes = {"default": "openid", "https://upstreamOP1.com", "openid mail", "https://upstreamOP2.com", "openid mail profile"}
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
## Interceptors

### http-inform

Fire and forget type of http informer of authentication events. Sends a post request to a defined endpoint without stalling authentication.

#### Activation

Http Informer is activated in relying party configuration by setting it is as outbound interceptor flow.

```
<bean parent="OIDC.SSO" p:outboundInterceptorFlows="#{{'http-inform'}}"/>
<bean parent="SAML2.SSO" p:outboundInterceptorFlows="#{{'http-inform'}}"/>
```
Service that sends the actual requests is activated by setting the http endpoint for post requests.

```
csclib.httpInformer.endpoint = https://my-api.com/lastlogin
``` 

#### Configuration
The content of post request is by default something that you most likely want to override. You need to define your own BiFunction bean for your content and the contract for making it is as follows.

#### input1
input1 is AttributeContext
#### input2
input2 is a map with following items

| key            | value                                                                |
|----------------|----------------------------------------------------------------------|
| acr            | authentication context class value                                   |
| serviceId      | Relying Party Identifier of the service, i.e. client_id or entityId  |
| serviceName    | Name of the service                                                  |
| remoteAddress  | IP address of the user                                               |

#### result
Result is expected to be either a json object as string or json array of two objects as a string. Single object or the first object of array are considered to be the payload of the post request. The second optional object in array can be used to set headers. See following example:
```
<bean id="my.LastLoginEvent" parent="shibboleth.BiFunctions.Scripted" factory-method="inlineScript">
           <constructor-arg>
           <value><![CDATA[
           logger = Java.type("org.slf4j.LoggerFactory").getLogger("my.LastLoginEvent");
           logger.debug("Forming last login Event");

           result = null;
		   // Get relying party identifier from input2. We call it applicationId.
           var applicationId = input2.get("serviceId");
		   // Get attribute userId from attribute context.
           var attribute = input1.getUnfilteredIdPAttributes().get("userId");
           var userId = (attribute != null) ? attribute.getValues().get(0).getDisplayValue() : null;
		   // Get attribute accesToken from attribute context. This is a case of requiring dynamic header value
           attribute = input1.getUnfilteredIdPAttributes().get("accessToken");
           var accessToken = (attribute != null) ? attribute.getValues().get(0).getDisplayValue() : null;

           if (applicationId != null && userId != null && accessToken != null){
               var ObjectMapper = Java.type("com.fasterxml.jackson.databind.ObjectMapper");
               var Map = Java.type("java.util.Map");
               var List = Java.type("java.util.List");
			   // Post body consists of who has logged in to which service
               var body = Map.of("applicationId", applicationId,"userId", userId);
			   // The API requires us to access token resolved in attribute resolution (for instance client credentials grant or similar).
               var headers = Map.of("Authorization", "Bearer " + accessToken);
               result = new ObjectMapper().writeValueAsString(List.of(body, headers));
           }
           logger.debug("Last login Event result '{}'", result);
           result;
           ]]>
           </value>
           </constructor-arg>
       </bean>
```
You need to tell http-informer to use your bean by setting it as value of property.
```
csclib.eventContentLookupStrategy = my.LastLoginEvent
```
#### Static Headers
Previous example resolved headers runtime for the request but that is not always needed. You may set additional headers to request simply by defining a property as json string.
```
csclib.httpInformer.headers = {"Header1":"Header1Value", "Header2":"Header2Value"}
```
#### Buffer Size and buffer
All events are stored in buffer for processing. By default buffer has space for 1000 events. The size of the buffer can be set with java property.
```
# one more item!
csclib.eventStore.maxBufferItems = 1001
```
If buffer get's full i.e. the receiving side API is down for instance the oldest item will be discarded.
#### Receiving API error codes and Buffer
If http request fails and http informer interpretes the failure is on API side the item is returned to buffer. The decision is based on http status code. The codes that suggest http informer the reason is not on the API side and item should not be placed back to buffer can be set with java property.
```
# The default set of codes that mean I should give up
csclib.httpInformer.finalFailureCodes = 400,401,403,404,405,409,410,415,422
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

  