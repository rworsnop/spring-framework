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
	 * Expect that the header with the given name is present.
	 */
	public RestTestClient.ResponseSpec exists(String name) {
		if (!this.exchangeResult.getHeaders().containsHeader(name)) {
			String message = getMessage(name) + " does not exist";
			this.exchangeResult.assertWithDiagnostics(() -> fail(message));
		}
		return this.responseSpec;
	}

	private static String getMessage(String headerName) {
		return "Response header '" + headerName + "'";
	}
}
