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

import java.net.URI;

import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestClient;

/**
 * @author Rob Worsnop
 */
public class DefaultRestTestClient implements RestTestClient {

	private final RestClient restClient;

	public DefaultRestTestClient(RestClient.Builder restClientBuilder) {
		this.restClient = restClientBuilder.build();
	}

	@Override
	public RequestHeadersUriSpec<?> get() {
		return methodInternal(HttpMethod.GET);
	}

	private RequestBodyUriSpec methodInternal(HttpMethod httpMethod) {
		return new DefaultRequestBodyUriSpec(restClient.method(httpMethod));
	}


	private class DefaultRequestBodyUriSpec implements RequestBodyUriSpec {

		private RestClient.RequestBodyUriSpec requestHeadersUriSpec;
		private RestClient.RequestBodySpec requestBodySpec;


		public DefaultRequestBodyUriSpec(RestClient.RequestBodyUriSpec spec) {
			this.requestHeadersUriSpec = spec;
			this.requestBodySpec = spec;
		}

		@Override
		public RequestBodySpec uri(URI uri) {
			this.requestBodySpec = requestHeadersUriSpec.uri(uri);
			return this;
		}

		@Override
		public RequestBodySpec header(String headerName, String... headerValues) {
			this.requestBodySpec = requestHeadersUriSpec.header(headerName, headerValues);
			return this;
		}

		@Override
		public ResponseSpec exchange() {
			ExchangeResult exchangeResult = requestBodySpec.exchange(
					(clientRequest, clientResponse) -> new ExchangeResult(clientResponse));
			return new DefaultResponseSpec(exchangeResult);
		}
	}

	private static class DefaultResponseSpec implements ResponseSpec {

		private final @Nullable ExchangeResult exchangeResult;

		public DefaultResponseSpec(@Nullable ExchangeResult exchangeResult) {
			this.exchangeResult = exchangeResult;
		}

		@Override
		public StatusAssertions expectStatus() {
			return new StatusAssertions(exchangeResult, this);
		}

		@Override
		public BodyContentSpec expectBody() {
			return new DefaultBodyContentSpec();
		}
	}

	private static class DefaultBodyContentSpec implements BodyContentSpec {

	}
}
