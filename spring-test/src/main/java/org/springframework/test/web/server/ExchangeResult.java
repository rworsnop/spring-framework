/*
 * Copyright 2002-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test.web.server;

import java.io.IOException;
import java.net.HttpCookie;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseCookie;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse;

/**
 * Container for request and response details for exchanges performed through
 * {@link RestTestClient}.
 * @author Rob Worsnop
 */
public class ExchangeResult {
	private static final Pattern SAME_SITE_PATTERN = Pattern.compile("(?i).*SameSite=(Strict|Lax|None).*");


	private static final Log logger = LogFactory.getLog(ExchangeResult.class);

	/** Ensure single logging; for example, for expectAll. */
	private boolean diagnosticsLogged;

	private final ConvertibleClientHttpResponse clientResponse;

	public ExchangeResult(@Nullable ConvertibleClientHttpResponse clientResponse) {
		this.clientResponse = Objects.requireNonNull(clientResponse, "clientResponse must be non-null");
	}

	public HttpStatusCode getStatus() {
		try {
			return this.clientResponse.getStatusCode();
		}
		catch (IOException ex) {
			throw new AssertionError(ex);
		}
	}

	public HttpHeaders getHeaders() {
		return this.clientResponse.getHeaders();
	}

	@Nullable
	public <T> T getBody(Class<T> bodyType) {
		return this.clientResponse.bodyTo(bodyType);
	}

	/**
	 * Execute the given Runnable, catch any {@link AssertionError}, log details
	 * about the request and response at ERROR level under the class log
	 * category, and after that re-throw the error.
	 */
	public void assertWithDiagnostics(Runnable assertion) {
		try {
			assertion.run();
		}
		catch (AssertionError ex) {
			if (!this.diagnosticsLogged && logger.isErrorEnabled()) {
				this.diagnosticsLogged = true;
				logger.error("Request details for assertion failure:\n" + this);
			}
			throw ex;
		}
	}

	/**
	 * Return response cookies received from the server.
	 */
	public MultiValueMap<String, ResponseCookie> getResponseCookies() {
		return Optional.ofNullable(this.clientResponse.getHeaders().get(HttpHeaders.SET_COOKIE)).orElse(List.of()).stream()
				.flatMap(header -> {
					Matcher matcher = SAME_SITE_PATTERN.matcher(header);
					String sameSite = (matcher.matches() ? matcher.group(1) : null);
					return HttpCookie.parse(header).stream().map(cookie -> toResponseCookie(cookie, sameSite));
				})
				.collect(LinkedMultiValueMap::new,
						(cookies, cookie) -> cookies.add(cookie.getName(), cookie),
						LinkedMultiValueMap::addAll);
	}

	private static ResponseCookie toResponseCookie(HttpCookie cookie, @Nullable String sameSite) {
		return ResponseCookie.from(cookie.getName(), cookie.getValue())
				.domain(cookie.getDomain())
				.httpOnly(cookie.isHttpOnly())
				.maxAge(cookie.getMaxAge())
				.path(cookie.getPath())
				.secure(cookie.getSecure())
				.sameSite(sameSite)
				.build();
	}
}
