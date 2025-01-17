package fi.csc.shibboleth.plugin.library.filter;

import java.io.IOException;

import javax.annotation.Nonnull;

import org.slf4j.MDC;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.servlet.AbstractConditionalFilter;
import net.shibboleth.shared.spring.servlet.ChainableFilter;

/**
 * Servlet filter that sets traceparent MDC attributes as the request comes in
 * and clears the MDC as the response is returned. Values are parsed either from
 * request parameter or header. Supports only traceparent version "00".
 */

public class SLF4JMDCTraceParentServletFilter extends AbstractConditionalFilter implements ChainableFilter {

    /** Traceparent header and parameter name. */
    @Nonnull
    @NotEmpty
    public static final String TRACEPARENT_FIELDNAME = "traceparent";

    /** MDC attribute name for traceparent trace id. */
    @Nonnull
    @NotEmpty
    public static final String TRACEPARENT_TRACEID_MDC_ATTRIBUTE = "csclib.traceparent_traceid";

    /** MDC attribute name for traceparent parent id. */
    @Nonnull
    @NotEmpty
    public static final String TRACEPARENT_PARENTID_MDC_ATTRIBUTE = "csclib.traceparent_parentid";

    /** MDC attribute name for traceparent trace flags. */
    @Nonnull
    @NotEmpty
    public static final String TRACEPARENT_TRACEFLAGS_MDC_ATTRIBUTE = "csclib.traceparent_traceflags";

    /** {@inheritDoc} */
    @Override
    protected void runFilter(final @Nonnull ServletRequest request, final @Nonnull ServletResponse response,
            final @Nonnull FilterChain chain) throws IOException, ServletException {
        try {
            if (request instanceof HttpServletRequest httpRequest) {
                String value = httpRequest.getHeader(TRACEPARENT_FIELDNAME);
                if (value == null) {
                    value = httpRequest.getParameter(TRACEPARENT_FIELDNAME);
                }
                if (value != null) {
                    String[] field = value.split("-");
                    if ("00".equals(field[0]) && field.length == 4) {
                        MDC.put(TRACEPARENT_TRACEID_MDC_ATTRIBUTE, field[1]);
                        MDC.put(TRACEPARENT_PARENTID_MDC_ATTRIBUTE, field[2]);
                        MDC.put(TRACEPARENT_TRACEFLAGS_MDC_ATTRIBUTE, field[3]);
                    }
                }
            }
            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
