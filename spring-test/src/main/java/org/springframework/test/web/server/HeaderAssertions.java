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

import java.util.Arrays;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpHeaders;
import org.springframework.util.CollectionUtils;

import static org.springframework.test.util.AssertionErrors.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertTrue;
import static org.springframework.test.util.AssertionErrors.fail;

/**
 * Assertions on headers of the response.
 *
 * @author Rob Worsnop
 * @see RestTestClient.ResponseSpec#expectHeader()
 */
public class HeaderAssertions {

	private final ExchangeResult exchangeResult;

	private final RestTestClient.ResponseSpec responseSpec;

	public HeaderAssertions(ExchangeResult exchangeResult, RestTestClient.ResponseSpec responseSpec) {
		this.exchangeResult = exchangeResult;
		this.responseSpec = responseSpec;
	}

	/**
	 * Expect a header with the given name to match the specified values.
	 */
	public RestTestClient.ResponseSpec valueEquals(String headerName, String... values) {
		return assertHeader(headerName, Arrays.asList(values), getHeaders().getOrEmpty(headerName));
	}

	/**
	 * Match the first value of the response header with a regex.
	 * @param name the header name
	 * @param pattern the regex pattern
	 */
	public RestTestClient.ResponseSpec valueMatches(String name, String pattern) {
		String value = getRequiredValue(name);
		String message = getMessage(name) + "=[" + value + "] does not match [" + pattern + "]";
		this.exchangeResult.assertWithDiagnostics(() -> assertTrue(message, value.matches(pattern)));
		return this.responseSpec;
	}

	/**
	 * Expect that the header with the given name is present.
	 */
	public RestTestClient.ResponseSpec exists(String name) {
		if (!this.exchangeResult.getResponseHeaders().containsHeader(name)) {
			String message = getMessage(name) + " does not exist";
			this.exchangeResult.assertWithDiagnostics(() -> fail(message));
		}
		return this.responseSpec;
	}

	private HttpHeaders getHeaders() {
		return this.exchangeResult.getResponseHeaders();
	}

	private String getRequiredValue(String name) {
		return getRequiredValues(name).get(0);
	}

	private List<String> getRequiredValues(String name) {
		List<String> values = getHeaders().get(name);
		if (!CollectionUtils.isEmpty(values)) {
			return values;
		}
		else {
			this.exchangeResult.assertWithDiagnostics(() -> fail(getMessage(name) + " not found"));
		}
		throw new IllegalStateException("This code path should not be reachable");
	}

	private RestTestClient.ResponseSpec assertHeader(String name, @Nullable Object expected, @Nullable Object actual) {
		this.exchangeResult.assertWithDiagnostics(() -> {
			String message = getMessage(name);
			assertEquals(message, expected, actual);
		});
		return this.responseSpec;
	}

	private static String getMessage(String headerName) {
		return "Response header '" + headerName + "'";
	}
}
