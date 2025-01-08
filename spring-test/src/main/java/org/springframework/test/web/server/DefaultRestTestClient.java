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
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.AssertionErrors;
import org.springframework.test.util.ExceptionCollector;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * Default implementation of {@link RestTestClient}.
 *
 * @author Rob Worsnop
 */
class DefaultRestTestClient implements RestTestClient {

	private final RestClient restClient;

	private final AtomicLong requestIndex = new AtomicLong();

	private final RestClient.Builder restClientBuilder;

	DefaultRestTestClient(RestClient.Builder restClientBuilder) {
		this.restClient = restClientBuilder.build();
		this.restClientBuilder = restClientBuilder;
	}

	@Override
	public RequestHeadersUriSpec<?> get() {
		return methodInternal(HttpMethod.GET);
	}

	@Override
	public RequestBodyUriSpec post() {
		return methodInternal(HttpMethod.POST);
	}

	@Override
	public Builder mutate() {
		return new DefaultRestTestClientBuilder(this.restClientBuilder);
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
		public RequestBodySpec uri(String uriTemplate, Object... uriVariables) {
			this.requestBodySpec = this.requestHeadersUriSpec.uri(uriTemplate, uriVariables);
			return this;
		}

		@Override
		public RequestBodySpec cookie(String name, String value) {
			this.requestBodySpec = this.requestHeadersUriSpec.cookie(name, value);
			return this;
		}

		@Override
		public RequestBodySpec cookies(Consumer<MultiValueMap<String, String>> cookiesConsumer) {
			this.requestBodySpec = this.requestHeadersUriSpec.cookies(cookiesConsumer);
			return this;
		}

		@Override
		public RequestBodySpec header(String headerName, String... headerValues) {
			this.requestBodySpec = this.requestHeadersUriSpec.header(headerName, headerValues);
			return this;
		}

		@Override
		public RequestBodySpec contentType(MediaType contentType) {
			this.requestBodySpec = this.requestHeadersUriSpec.contentType(contentType);
			return this;
		}

		@Override
		public RequestHeadersSpec<?> bodyValue(Object body) {
			this.requestHeadersUriSpec.body(body);
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
			return new DefaultBodyContentSpec(this.exchangeResult);
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
		private final ExchangeResult result;

		public DefaultBodyContentSpec(ExchangeResult result) {
			this.result = result;
		}

		@Override
		public ExchangeResult isEmpty() {
			this.result.assertWithDiagnostics(() ->
					AssertionErrors.assertTrue("Expected empty body",
							this.result.getBody(byte[].class) == null));
			return this.result;
		}
	}

	private static class DefaultBodySpec<B, S extends BodySpec<B, S>> implements BodySpec<B, S> {

		private final ExchangeResult result;
		private final Class<B> bodyType;

		public DefaultBodySpec(@Nullable ExchangeResult result, Class<B> bodyType) {
			this.result = Objects.requireNonNull(result, "exchangeResult must be non-null");
			this.bodyType = bodyType;
		}

		@Override
		public ResponseEntity<B> returnResult() {
			return ResponseEntity.status(this.result.getStatus())
					.headers(this.result.getResponseHeaders())
					.body(this.result.getBody(this.bodyType));
		}

		@Override
		public <T extends S> T isEqualTo(B expected) {
			this.result.assertWithDiagnostics(() ->
					AssertionErrors.assertEquals("Response body", expected, this.result.getBody(this.bodyType)));
			return self();
		}

		@SuppressWarnings("unchecked")
		private <T extends S> T self() {
			return (T) this;
		}
	}
}
