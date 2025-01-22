package fi.csc.shibboleth.plugin.library.util;

import java.util.Random;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bouncycastle.util.encoders.DecoderException;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.MDC;

import fi.csc.shibboleth.plugin.library.filter.SLF4JMDCTraceParentServletFilter;
import net.shibboleth.shared.primitive.LoggerFactory;

/**
 * Utility class both to verify traceparent header
 * {@link https://www.w3.org/TR/trace-context/#traceparent-header} value and
 * also to form one. Relies on {@link SLF4JMDCTraceParentServletFilter} to
 * create MDC keys.
 */
public class TraceparentHeader {

    /** Class logger. */
    @Nonnull
    private final static Logger log = LoggerFactory.getLogger(TraceparentHeader.class);

    /**
     * https://www.w3.org/TR/trace-context/#version.
     */
    @Nonnull
    private final static String version = "00";

    /**
     * https://www.w3.org/TR/trace-context/#trace-id.
     */
    @Nonnull
    private final String traceId;

    /**
     * https://www.w3.org/TR/trace-context/#parent-id.
     */
    @Nonnull
    private final String parentId;

    /**
     * https://www.w3.org/TR/trace-context/#trace-flags.
     */
    @Nonnull
    private final String traceFlags;

    /**
     * Get https://www.w3.org/TR/trace-context/#version.
     * 
     * @return https://www.w3.org/TR/trace-context/#version
     */
    @Nonnull
    public static String getVersion() {
        return version;
    }

    /**
     * Get https://www.w3.org/TR/trace-context/#trace-id.
     * 
     * @return https://www.w3.org/TR/trace-context/#trace-id
     */
    @Nonnull
    public String getTraceId() {
        return traceId;
    }

    /**
     * Get https://www.w3.org/TR/trace-context/#parent-id.
     * 
     * @return https://www.w3.org/TR/trace-context/#version
     */
    @Nonnull
    public String getParentId() {
        return parentId;
    }

    /**
     * Get https://www.w3.org/TR/trace-context/#trace-flags,
     * 
     * @return https://www.w3.org/TR/trace-context/#trace-flags
     */
    @Nonnull
    public String getTraceFlags() {
        return traceFlags;
    }

    /**
     * Get https://www.w3.org/TR/trace-context/#traceparent-header value.
     * 
     * @return https://www.w3.org/TR/trace-context/#traceparent-header value
     */
    @Nonnull
    public String getValue() {
        return version + "-" + traceId + "-" + parentId + "-" + traceFlags;
    }

    /**
     * Constructor.
     * 
     * @param traceId    https://www.w3.org/TR/trace-context/#trace-id
     * @param parentId   https://www.w3.org/TR/trace-context/#parent-id
     * @param traceFlags https://www.w3.org/TR/trace-context/#trace-flags
     */
    private TraceparentHeader(@Nonnull String traceId, @Nonnull String parentId, @Nonnull String traceFlags) {
        assert traceId != null;
        assert parentId != null;
        assert traceFlags != null;
        this.traceId = traceId;
        this.parentId = parentId;
        this.traceFlags = traceFlags;
    }

    /**
     * Parse https://www.w3.org/TR/trace-context/#traceparent-header value
     * 
     * @param value https://www.w3.org/TR/trace-context/#traceparent-header value
     * @return TraceparentHeader instance
     */
    @Nullable
    public static TraceparentHeader parse(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String[] field = value.split("-");
        if (version.equals(field[0])) {
            try {
                if (field[1].equals(field[1].toLowerCase()) && field[2].equals(field[2].toLowerCase())
                        && Hex.decodeStrict(field[1]).length == 16 && Hex.decodeStrict(field[2]).length == 8
                        && Hex.decodeStrict(field[3]).length == 1) {
                    return new TraceparentHeader(field[1], field[2], field[3]);
                } else {
                    log.error("Traceparent header value {} is invalid", value);
                    return null;
                }

            } catch (DecoderException e) {
                log.error("Traceparent header value {} is invalid", value, e);
                return null;
            }
        }
        log.error("Traceparent header version not supported {}", value);
        return null;
    }

    /**
     * Spans new traceparent header value for current trace-id.
     * 
     * @param traceFlags https://www.w3.org/TR/trace-context/#trace-flags
     * @return TraceparentHeader instance
     */
    @Nonnull
    public static TraceparentHeader spanTraceheader(String traceFlags) {
        return new TraceparentHeader(MDC.get(SLF4JMDCTraceParentServletFilter.TRACEPARENT_TRACEID_MDC_ATTRIBUTE),
                generateParentId(), traceFlags);
    }

    /**
     * Spans new traceparent header value for current trace-id.
     * 
     * @return TraceparentHeader instance
     */
    @Nonnull
    public static TraceparentHeader spanTraceheader() {
        return new TraceparentHeader(MDC.get(SLF4JMDCTraceParentServletFilter.TRACEPARENT_TRACEID_MDC_ATTRIBUTE),
                generateParentId(), SLF4JMDCTraceParentServletFilter.TRACEPARENT_TRACEFLAGS_MDC_ATTRIBUTE);
    }

    /**
     * Generates new traceparent header value.
     * 
     * @param traceFlags https://www.w3.org/TR/trace-context/#trace-flags
     * @return TraceparentHeader instance
     */
    @Nonnull
    public static TraceparentHeader generateTraceheader(String traceFlags) {
        return new TraceparentHeader(generateTraceId(), generateParentId(), traceFlags);
    }

    /**
     * Generates new traceparent header value. trace-flag is set to "01".
     * 
     * @param traceFlags https://www.w3.org/TR/trace-context/#trace-flags
     * @return TraceparentHeader instance
     */
    @Nonnull
    public static TraceparentHeader generateTraceheader() {
        return new TraceparentHeader(generateTraceId(), generateParentId(), "01");
    }

    /**
     * Generate https://www.w3.org/TR/trace-context/#trace-id.
     * 
     * @return https://www.w3.org/TR/trace-context/#trace-id
     */
    @Nonnull
    public static String generateTraceId() {
        Random random = new Random();
        byte traceId[] = new byte[16];
        random.nextBytes(traceId);
        return Hex.toHexString(traceId);
    }

    /**
     * Generate https://www.w3.org/TR/trace-context/#parent-id.
     * 
     * @return https://www.w3.org/TR/trace-context/#trace-id
     */
    @Nonnull
    public final static String generateParentId() {
        Random random = new Random();
        byte parentId[] = new byte[8];
        random.nextBytes(parentId);
        return Hex.toHexString(parentId);
    }
}
