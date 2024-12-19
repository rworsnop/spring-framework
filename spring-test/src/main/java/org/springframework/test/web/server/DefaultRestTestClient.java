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

import org.springframework.http.HttpMethod;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse;

/**
 * @author Rob Worsnop
 */
public class DefaultRestTestClient implements RestTestClient {

	private final RestClient.Builder restClientBuilder;

	public DefaultRestTestClient(RestClient.Builder restClientBuilder) {
		this.restClientBuilder = restClientBuilder;
	}

	@Override
	public RequestHeadersUriSpec<?> get() {
		return methodInternal(HttpMethod.GET);
	}

	private RequestBodyUriSpec methodInternal(HttpMethod httpMethod) {
		return new DefaultRequestBodyUriSpec(httpMethod);
	}


	private class DefaultRequestBodyUriSpec implements RequestBodyUriSpec {

		private final HttpMethod httpMethod;

		@Nullable
		private URI uri;

		public DefaultRequestBodyUriSpec(HttpMethod httpMethod) {
			this.httpMethod = httpMethod;
		}

		@Override
		public RequestBodySpec uri(URI uri) {
			this.uri = uri;
			return this;
		}

		@Override
		public ResponseSpec exchange() {
			RestClient.RequestBodyUriSpec bodyUriSpec = restClientBuilder.build().method(httpMethod);
			RestClient.RequestBodySpec bodySpec = uri != null ? bodyUriSpec.uri(uri) : bodyUriSpec;

			ExchangeResult exchangeResult = bodySpec.exchange(
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
