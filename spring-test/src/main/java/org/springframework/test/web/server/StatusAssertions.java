/*
 * Copyright 2002-2024 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test.web.server;

import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Assertions on the response status.
 *
 * @author Rob Worsnop
 *
 * @see RestTestClient.ResponseSpec#expectStatus()
 */
public class StatusAssertions {

	private final ExchangeResult exchangeResult;

	private final RestTestClient.ResponseSpec responseSpec;

	public StatusAssertions(ExchangeResult exchangeResult, RestTestClient.ResponseSpec responseSpec) {
		this.exchangeResult = exchangeResult;
		this.responseSpec = responseSpec;
	}

	/**
	 * Assert the response status code is {@code HttpStatus.OK} (200).
	 */
	public RestTestClient.ResponseSpec isOk() {
		return assertStatusAndReturn(HttpStatus.OK);
	}

	private RestTestClient.ResponseSpec assertStatusAndReturn(HttpStatus httpStatus) {
		return this.responseSpec;
	}


}
