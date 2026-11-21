package io.github.hichambenhsine.strobe.service;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/**
 * Pure byte-level payload sanitizer preventing unsafe Java deserialization and OOM crashes.
 * Detects JDK serialization magic headers, GZIP archives, arbitrary binary blobs, and UTF-8/JSON text.
 */
public class RedisPayloadSanitizer {

    public static final int MAX_PAYLOAD_BYTES = 64 * 1024;
    private static final int BINARY_PREVIEW_BYTES = 64;
    private static final int TEXT_PROBE_BYTES = 4096;

    /**
     * Encapsulates the decoded representation and metadata of a raw byte payload.
     */
    public record SanitizedPayload(
            String encoding,
            long sizeBytes,
            boolean truncated,
            String content
    ) {}

    /**
     * Safely decodes raw bytes without instantiating arbitrary domain classes.
     *
     * @param rawBytes Raw byte array retrieved from datastore.
     * @return SanitizedPayload containing safe printable output and encoding metadata.
     */
    public SanitizedPayload sanitize(byte[] rawBytes) {
        if (rawBytes == null || rawBytes.length == 0) {
            return new SanitizedPayload("EMPTY", 0L, false, "");
        }

        long totalSize = rawBytes.length;

        // 1. Detect JDK ObjectInputStream magic bytes (0xAC 0xED) - never deserialize
        if (rawBytes.length >= 2 && rawBytes[0] == (byte) 0xAC && rawBytes[1] == (byte) 0xED) {
            return buildBinaryPayload("JDK_SERIALIZED", rawBytes, totalSize);
        }

        // 2. Detect GZIP magic bytes (0x1F 0x8B)
        if (rawBytes.length >= 2 && rawBytes[0] == (byte) 0x1F && rawBytes[1] == (byte) 0x8B) {
            return buildBinaryPayload("GZIP_BINARY", rawBytes, totalSize);
        }

        // 3. Probe initial segment to verify strict UTF-8 printable text
        if (!isPrintableUtf8(rawBytes)) {
            return buildBinaryPayload("BINARY", rawBytes, totalSize);
        }

        // 4. Truncate safely at byte limit before creating String to protect JVM heap
        boolean truncated = totalSize > MAX_PAYLOAD_BYTES;
        int sliceLength = truncated ? MAX_PAYLOAD_BYTES : rawBytes.length;

        String decoded = new String(rawBytes, 0, sliceLength, StandardCharsets.UTF_8);
        String encoding = detectTextFormat(decoded, truncated);

        return new SanitizedPayload(encoding, totalSize, truncated, decoded);
    }

    private SanitizedPayload buildBinaryPayload(String encoding, byte[] rawBytes, long totalSize) {
        int previewLength = Math.min(rawBytes.length, BINARY_PREVIEW_BYTES);
        String hexPreview = HexFormat.ofDelimiter(" ").formatHex(rawBytes, 0, previewLength);
        boolean truncated = totalSize > BINARY_PREVIEW_BYTES;
        String content = "[" + encoding + " - " + totalSize + " bytes] Hex preview: " + hexPreview;
        return new SanitizedPayload(encoding, totalSize, truncated, content);
    }

    private boolean isPrintableUtf8(byte[] rawBytes) {
        int probeLength = Math.min(rawBytes.length, TEXT_PROBE_BYTES);
        CharsetDecoder strictDecoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);

        try {
            int adjustedLength = probeLength;
            if (rawBytes.length > TEXT_PROBE_BYTES) {
                while (adjustedLength > 0 && (rawBytes[adjustedLength - 1] & 0xC0) == 0x80) {
                    adjustedLength--;
                }
                if (adjustedLength > 0 && (rawBytes[adjustedLength - 1] & 0x80) != 0) {
                    adjustedLength--;
                }
            }

            CharBuffer chars = strictDecoder.decode(ByteBuffer.wrap(rawBytes, 0, adjustedLength));
            for (int i = 0; i < chars.length(); i++) {
                char c = chars.get(i);
                if (c < 0x20 && c != '\t' && c != '\n' && c != '\r') {
                    return false;
                }
            }
            return true;
        } catch (CharacterCodingException e) {
            return false;
        }
    }

    private String detectTextFormat(String text, boolean truncated) {
        String trimmed = text.trim();
        if (trimmed.startsWith("{") && (truncated || trimmed.endsWith("}"))) {
            return "JSON";
        }
        if (trimmed.startsWith("[") && (truncated || trimmed.endsWith("]"))) {
            return "JSON";
        }
        return "UTF8_TEXT";
    }
}