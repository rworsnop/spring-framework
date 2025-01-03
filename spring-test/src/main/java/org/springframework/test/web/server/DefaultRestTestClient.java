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

import java.net.URI;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ExceptionCollector;
import org.springframework.web.client.RestClient;

/**
 * Default implementation of {@link RestTestClient}.
 *
 * @author Rob Worsnop
 */
public class DefaultRestTestClient implements RestTestClient {

	private final RestClient restClient;

	private final AtomicLong requestIndex = new AtomicLong();

	public DefaultRestTestClient(RestClient.Builder restClientBuilder) {
		this.restClient = restClientBuilder.build();

	}

	@Override
	public RequestHeadersUriSpec<?> get() {
		return methodInternal(HttpMethod.GET);
	}

	private RequestBodyUriSpec methodInternal(HttpMethod httpMethod) {
		return new DefaultRequestBodyUriSpec(this.restClient.method(httpMethod));
	}


	private class DefaultRequestBodyUriSpec implements RequestBodyUriSpec {

		private RestClient.RequestBodyUriSpec requestHeadersUriSpec;
		private RestClient.RequestBodySpec requestBodySpec;
		private final String requestId;


		public DefaultRequestBodyUriSpec(RestClient.RequestBodyUriSpec spec) {
			this.requestHeadersUriSpec = spec;
			this.requestBodySpec = spec;
			this.requestId = String.valueOf(requestIndex.incrementAndGet());
		}

		@Override
		public RequestBodySpec uri(URI uri) {
			this.requestBodySpec = this.requestHeadersUriSpec.uri(uri);
			return this;
		}

		@Override
		public RequestBodySpec header(String headerName, String... headerValues) {
			this.requestBodySpec = this.requestHeadersUriSpec.header(headerName, headerValues);
			return this;
		}

		@Override
		public ResponseSpec exchange() {
			this.requestBodySpec = this.requestBodySpec.header(RESTTESTCLIENT_REQUEST_ID, this.requestId);
			ExchangeResult exchangeResult = this.requestBodySpec.exchange(
					(clientRequest, clientResponse) -> new ExchangeResult(clientResponse),
					false);
			return new DefaultResponseSpec(Objects.requireNonNull(exchangeResult));
		}
	}

	private static class DefaultResponseSpec implements ResponseSpec {

		private final ExchangeResult exchangeResult;

		public DefaultResponseSpec(ExchangeResult exchangeResult) {
			this.exchangeResult = exchangeResult;
		}

		@Override
		public StatusAssertions expectStatus() {
			return new StatusAssertions(this.exchangeResult, this);
		}

		@Override
		public BodyContentSpec expectBody() {
			return new DefaultBodyContentSpec();
		}

		@Override
		public <B> BodySpec<B, ?> expectBody(Class<B> bodyType) {
			return new DefaultBodySpec<>(this.exchangeResult, bodyType);
		}

		@Override
		public CookieAssertions expectCookie() {
			return new CookieAssertions(this.exchangeResult, this);
		}

		@Override
		public HeaderAssertions expectHeader() {
			return new HeaderAssertions(this.exchangeResult, this);
		}

		@Override
		public ResponseSpec expectAll(ResponseSpecConsumer... consumers) {
			ExceptionCollector exceptionCollector = new ExceptionCollector();
			for (ResponseSpecConsumer consumer : consumers) {
				exceptionCollector.execute(() -> consumer.accept(this));
			}
			try {
				exceptionCollector.assertEmpty();
			}
			catch (RuntimeException ex) {
				throw ex;
			}
			catch (Exception ex) {
				// In theory, a ResponseSpecConsumer should never throw an Exception
				// that is not a RuntimeException, but since ExceptionCollector may
				// throw a checked Exception, we handle this to appease the compiler
				// and in case someone uses a "sneaky throws" technique.
				throw new AssertionError(ex.getMessage(), ex);
			}
			return this;
		}
	}

	private static class DefaultBodyContentSpec implements BodyContentSpec {

	}

	private static class DefaultBodySpec<B, S extends BodySpec<B, S>> implements BodySpec<B, S> {

		private final ExchangeResult exchangeResult;
		private final Class<B> bodyType;

		public DefaultBodySpec(@Nullable ExchangeResult exchangeResult, Class<B> bodyType) {
			this.exchangeResult = Objects.requireNonNull(exchangeResult, "exchangeResult must be non-null");
			this.bodyType = bodyType;
		}

		@Override
		public ResponseEntity<B> returnResult() {
			return ResponseEntity.status(this.exchangeResult.getStatus())
					.headers(this.exchangeResult.getHeaders())
					.body(this.exchangeResult.getBody(this.bodyType));
		}
	}
}
