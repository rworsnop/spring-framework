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

import org.springframework.http.ResponseCookie;

import static org.springframework.test.util.AssertionErrors.fail;

/**
 * Assertions on cookies of the response.
 *
 * @author Rob Worsnop
 */
public class CookieAssertions {

	private final ExchangeResult exchangeResult;

	private final RestTestClient.ResponseSpec responseSpec;

	public CookieAssertions(ExchangeResult exchangeResult, RestTestClient.ResponseSpec responseSpec) {
		this.exchangeResult = exchangeResult;
		this.responseSpec = responseSpec;
	}

	/**
	 * Expect that the cookie with the given name is present.
	 */
	public RestTestClient.ResponseSpec exists(String name) {
		getCookie(name);
		return this.responseSpec;
	}


	private ResponseCookie getCookie(String name) {
		ResponseCookie cookie = this.exchangeResult.getResponseCookies().getFirst(name);
		if (cookie != null) {
			return cookie;
		}
		else {
			this.exchangeResult.assertWithDiagnostics(() -> fail("No cookie with name '" + name + "'"));
		}
		throw new IllegalStateException("This code path should not be reachable");
	}
}
