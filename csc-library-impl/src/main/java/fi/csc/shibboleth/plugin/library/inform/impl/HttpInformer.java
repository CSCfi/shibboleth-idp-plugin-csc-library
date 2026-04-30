package fi.csc.shibboleth.plugin.library.inform.impl;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.opensaml.security.httpclient.HttpClientSecurityParameters;
import org.opensaml.security.httpclient.HttpClientSecuritySupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import net.shibboleth.shared.annotation.constraint.NonnullAfterInit;
import net.shibboleth.shared.annotation.constraint.NonnullBeforeExec;
import net.shibboleth.shared.httpclient.HttpClientSupport;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.logic.ConstraintViolationException;

/**
 * Class sending HTTP Post requests in fire and forget - way. The payload for
 * post is read from {@link EventStore}.
 */
public class HttpInformer implements SmartLifecycle {

    /** Class logger. */
    @Nonnull
    private final Logger log = LoggerFactory.getLogger(HttpInformer.class);

    /** HTTP client for sending Post requests. */
    @NonnullAfterInit
    private HttpClient httpClient;

    /** HTTP security parameters. */
    @Nullable
    private HttpClientSecurityParameters httpClientSecurityParameters;

    /** Post Endpoint. */
    @Nonnull
    @Value("%{csclib.httpInformer.endpoint:}")
    private String endpoint;

    /** Failure codes that suggest resending event will not be successfull. */
    @Nonnull
    @Value("%{csclib.httpInformer.finalFailureCodes:400,401,403,404,405,409,410,415,422}")
    private List<Integer> finalFailureCodes;

    /** Event Store we look for content. */
    @NonnullBeforeExec
    private EventStore eventStore;

    /** Object mapper for parsing json. */
    @Nonnull
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /** Additional headers for Post request. */
    @Nullable
    private Map<String, String> headers;

    /** Set additional headers for Post request. */
    @Value("%{csclib.httpInformer.headers:}")
    public void setHeaders(String input) {
        if (input == null || input.isBlank()) {
            return;
        }
        try {
            headers = objectMapper.readValue(input, new TypeReference<Map<String, String>>() {
            });
        } catch (JsonProcessingException e) {
            log.error("HttpInformer failed parsing 'csclib.httpInformer.headers' - map");
        }
    }

    /** Number of workers sending Post requests. */
    @Nonnull
    @Value("%{csclib.httpInformer.workers:2}")
    private int workers;

    /** Executor that sends the HTTP requests. */
    @NonnullBeforeExec
    private ExecutorService executor;

    /**
     * Set HTTP client for sending Post requests.
     * 
     * @param client HTTP client for sending Post requests
     */
    public void setHttpClient(@Nonnull final HttpClient client) {
        httpClient = Constraint.isNotNull(client, "HttpClient cannot be null");
    }

    /**
     * Set HTTP security parameters.
     * 
     * @param params HTTP security parameters
     */
    public void setHttpClientSecurityParameters(@Nullable final HttpClientSecurityParameters params) {
        httpClientSecurityParameters = params;
    }

    /**
     * Set Event Store we look for content.
     * 
     * @param store Event Store we look for content
     */
    public void setEventStore(@Nonnull EventStore store) {
        eventStore = Constraint.isNotNull(store, "Event Store cannot be null");
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void start() {
        if (executor != null && !executor.isShutdown()) {
            return;
        }
        try {
            Constraint.isNotNull(httpClient, "Http client must not be null");
            Constraint.isNotNull(eventStore, "Eventstore must not be null");
            Constraint.isNotEmpty(endpoint, "Endpoint must not be null or empty");
        } catch (ConstraintViolationException e) {
            log.info("HttpInformer not started due to missing configuration: {}", e.getMessage());
            return;
        }
        executor = Executors.newFixedThreadPool(workers);
        executeTasks();
    }

    /** {@inheritDoc} */
    @Override
    public void stop() {
        if (executor == null) {
            return;
        }
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isRunning() {
        return executor != null && !executor.isTerminated();
    }

    /**
     * Starts worker threads that look for content in {@link EventStore}, forms Post
     * request by adding optional additional headers either from event or
     * properties, then finally posts the request. In case of failure the event is
     * returned back to {@link EventStore} if failure code suggests we are not in
     * fault.
     */
    private void executeTasks() {

        for (int i = 0; i < workers; i++) {
            executor.submit(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        String event = eventStore.takeItemFromBuffer();
                        List<Map<String, Object>> events = eventToMap(event);
                        if (events.isEmpty()) {
                            continue;
                        }
                        ClassicRequestBuilder builder = ClassicRequestBuilder.post(endpoint)
                                .setHeader("accept", "application/json").setHeader("Content-Type", "application/json");
                        // Set optional statically defined headers.
                        if (headers != null) {
                            for (Map.Entry<String, String> entry : headers.entrySet()) {
                                builder.setHeader(entry.getKey(), entry.getValue());
                            }
                        }
                        // Set optional dynamically defined headers.
                        if (events.size() > 1 && events.get(1) != null) {
                            for (String key : events.get(1).keySet()) {
                                builder.setHeader(key, String.valueOf(events.get(1).get(key)));
                            }
                        }
                        String payload;
                        try {
                            payload = objectMapper.writeValueAsString(events.get(0));
                        } catch (JsonProcessingException e) {
                            log.error("Payload is corrupt");
                            continue;
                        }
                        builder.setEntity(new StringEntity(payload));
                        ClassicHttpRequest request = builder.build();
                        InformerResponse response = executeHttpRequest(request);
                        if (response != null
                                && (response.indicateSuccess() || finalFailureCodes.contains(response.getCode()))) {
                            if (!response.indicateSuccess()) {
                                log.error("HttpInformer received error code {} and response '{}'", response.getCode(),
                                        response.getPayload());
                            }
                            continue;
                        }
                        if (response != null) {
                            log.error("HttpInformer received error code {} and response '{}'. Returning item to buffer",
                                    response.getCode(), response.getPayload());
                        } else {
                            log.error("HttpInformer io failed. Returning item to buffer");
                        }
                        Thread.sleep(10000);
                        eventStore.returnItemToBuffer(event);
                    } catch (InterruptedException e) {
                        log.info("HttpInformer interrupted.");
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
        }

    }

    /**
     * Deserializes event to list of maps. First map is content and second if it
     * exists is headers.
     * 
     * @param json either one json object or two json objects in array.
     * @return Deserialized event as list of maps. First map is content and second
     *         if it exists is headers.
     */
    @Nonnull
    private List<Map<String, Object>> eventToMap(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            try {
                Map<String, Object> single = objectMapper.readValue(json, new TypeReference<>() {
                });
                return List.of(single);
            } catch (JsonProcessingException ex) {
                log.error("Failed to parse event: {}", json, ex);
                return List.of();
            }
        }

    }

    /**
     * Executes HTTP Request
     * 
     * @param request Request to execute.
     * @return
     */
    @Nullable
    protected InformerResponse executeHttpRequest(@Nonnull final ClassicHttpRequest request) {
        final HttpClientContext clientContext = HttpClientContext.create();
        HttpClientSecuritySupport.marshalSecurityParameters(clientContext, httpClientSecurityParameters, true);
        HttpClientSecuritySupport.addDefaultTLSTrustEngineCriteria(clientContext, request);
        try {
            InformerResponse response = httpClient.execute(request, clientContext, new InformerResponseHandler());
            HttpClientSecuritySupport.checkTLSCredentialEvaluated(clientContext, request.getScheme());
            return response;

        } catch (IOException e) {
            log.error("HttpInformer io failure.", e);
            return null;
        }
    }

    /**
     * Class that what wraps HTTP response payload and status code.
     */
    public class InformerResponse {

        /** HTTP status code . */
        private final int code;

        /** HTTP response payload . */
        @Nullable
        private final String payload;

        /**
         * Constructor.
         * 
         * @param code    HTTP status code
         * @param payload HTTP response payload
         */
        public InformerResponse(int code, @Nullable String payload) {
            this.code = code;
            this.payload = payload;
        }

        /**
         * Get HTTP status code.
         * 
         * @return HTTP response payload
         */
        public int getCode() {
            return code;
        }

        /**
         * Get HTTP response payload.
         * 
         * @return HTTP response payload
         */
        @Nullable
        public String getPayload() {
            return payload;
        }

        /**
         * Whether status code indicates success.
         * 
         * @return
         */
        public boolean indicateSuccess() {
            return code >= 200 && code < 300;
        }
    }

    /**
     * Response handler to parse {@InformerResponse}.
     */
    public class InformerResponseHandler implements HttpClientResponseHandler<InformerResponse> {

        @Override
        public InformerResponse handleResponse(ClassicHttpResponse response) throws HttpException, IOException {
            return new InformerResponse(response.getCode(),
                    response.getEntity() != null ? HttpClientSupport.toString(response.getEntity(), "UTF-8", 65536)
                            : null);
        }
    }

}