package fi.csc.shibboleth.plugin.library.filter;

import java.io.IOException;

import javax.annotation.Nonnull;

import org.slf4j.MDC;

import fi.csc.shibboleth.plugin.library.util.TraceparentHeader;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.servlet.AbstractConditionalFilter;
import net.shibboleth.shared.spring.servlet.ChainableFilter;

/**
 * Servlet filter that sets traceparent MDC attributes as the request comes in
 * and clears the MDC as the response is returned. Values are parsed either from
 * request parameter or header in the case execution is not inside webflow yet.
 * If we are inside webflow value is read from session. If no value can be found
 * traceparent information is generatd. Supports only traceparent version "00".
 * 
 */

public class SLF4JMDCTraceParentServletFilter extends AbstractConditionalFilter implements ChainableFilter {

    /** Traceparent header and parameter name. */
    @Nonnull
    @NotEmpty
    public static final String TRACEPARENT_FIELDNAME = "traceparent";

    /** Sessio attribute name for traceparent. */
    @Nonnull
    @NotEmpty
    public static final String TRACEPARENT_ATTRIBUTE = "csclib.traceparent";

    /** Traceparent default flags. */
    @Nonnull
    @NotEmpty
    public static final String TRACEPARENT_DEFAULT_TRACEFLAGS = "01";

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
        assert request != null;
        assert response != null;
        assert chain != null;
        try {
            if (request instanceof HttpServletRequest httpRequest) {
                HttpSession session = ((HttpServletRequest) request).getSession();
                String value = null;
                if (httpRequest.getParameter("execution") == null) {
                    session.removeAttribute(TRACEPARENT_ATTRIBUTE);
                } else {
                    value = (String) session.getAttribute(TRACEPARENT_ATTRIBUTE);
                }
                value = value == null ? httpRequest.getHeader(TRACEPARENT_FIELDNAME) : value;
                value = value == null ? httpRequest.getParameter(TRACEPARENT_FIELDNAME) : value;
                TraceparentHeader header = TraceparentHeader.parse(value);
                header = header == null ? TraceparentHeader.generateTraceheader() : header;
                session.setAttribute(TRACEPARENT_ATTRIBUTE, header.getValue());
                MDC.put(TRACEPARENT_TRACEID_MDC_ATTRIBUTE, header.getTraceId());
                MDC.put(TRACEPARENT_PARENTID_MDC_ATTRIBUTE, header.getParentId());
                MDC.put(TRACEPARENT_TRACEFLAGS_MDC_ATTRIBUTE, header.getTraceFlags());
            }
            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
