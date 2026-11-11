package com.controltower.backend.observability;

import java.util.UUID;
import java.util.regex.Pattern;

import org.slf4j.MDC;

public final class RequestCorrelation {
	public static final String HEADER_NAME = "X-Request-ID";
	public static final String MDC_KEY = "requestId";
	private static final Pattern SAFE_REQUEST_ID = Pattern.compile("[A-Za-z0-9._-]{1,36}");

	private RequestCorrelation() {
	}

	public static String resolve(String candidate) {
		if (candidate != null && SAFE_REQUEST_ID.matcher(candidate).matches()) {
			return candidate;
		}
		return UUID.randomUUID().toString();
	}

	public static String currentRequestId() {
		String requestId = MDC.get(MDC_KEY);
		return requestId == null ? UUID.randomUUID().toString() : requestId;
	}
}
