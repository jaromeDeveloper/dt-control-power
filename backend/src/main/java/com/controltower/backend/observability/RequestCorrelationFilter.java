package com.controltower.backend.observability;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestCorrelationFilter extends OncePerRequestFilter {

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		String requestId = RequestCorrelation.resolve(request.getHeader(RequestCorrelation.HEADER_NAME));
		response.setHeader(RequestCorrelation.HEADER_NAME, requestId);
		try (MDC.MDCCloseable ignored = MDC.putCloseable(RequestCorrelation.MDC_KEY, requestId)) {
			filterChain.doFilter(request, response);
		}
	}
}
