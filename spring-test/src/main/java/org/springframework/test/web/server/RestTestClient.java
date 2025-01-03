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
import java.util.Map;
import java.util.function.Consumer;

import jakarta.servlet.Filter;

import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.test.web.servlet.DispatcherServletCustomizer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.setup.ConfigurableMockMvcBuilder;
import org.springframework.test.web.servlet.setup.MockMvcConfigurer;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.util.UriBuilderFactory;

/**
 * Client for testing web servers.
 *
 * @author Rob Worsnop
 */
public interface RestTestClient {

	/**
	 * The name of a request header used to assign a unique id to every request
	 * performed through the {@code RestTestClient}. This can be useful for
	 * storing contextual information at all phases of request processing (for example,
	 * from a server-side component) under that id and later to look up
	 * that information once an {@link ExchangeResult} is available.
	 */
	String RESTTESTCLIENT_REQUEST_ID = "RestTestClient-Request-Id";

	static MockMvcServerSpec<?> bindToApplicationContext(WebApplicationContext context) {
		return new ApplicationContextMockMvcSpec(context);
	}

	/**
	 * Prepare an HTTP GET request.
	 * @return a spec for specifying the target URL
	 */
	RequestHeadersUriSpec<?> get();

	/**
	 * This server setup option allows you to connect to a live server through
	 * a client connector.
	 * <p><pre class="code">
	 * RestTestClient client = RestTestClient.bindToServer()
	 *         .baseUrl("http://localhost:8080")
	 *         .build();
	 * </pre>
	 * @return chained API to customize client config
	 */
	static Builder bindToServer() {
		return new DefaultRestTestClientBuilder();
	}

	/**
	 * A variant of {@link #bindToServer()} with a pre-configured request factory.
	 * @return chained API to customize client config
	 */
	static Builder bindToServer(ClientHttpRequestFactory requestFactory) {
		return new DefaultRestTestClientBuilder(RestClient.builder().requestFactory(requestFactory));
	}


	/**
	 * Base specification for configuring {@link MockMvc}, and a simple facade
	 * around {@link ConfigurableMockMvcBuilder}.
	 *
	 * @param <B> a self reference to the builder type
	 */
	interface MockMvcServerSpec<B extends MockMvcServerSpec<B>> {
		/**
		 * Add a global filter.
		 * <p>This is delegated to
		 * {@link ConfigurableMockMvcBuilder#addFilters(Filter...)}.
		 */
		<T extends B> T filters(Filter... filters);

		/**
		 * Add a filter for specific URL patterns.
		 * <p>This is delegated to
		 * {@link ConfigurableMockMvcBuilder#addFilter(Filter, String...)}.
		 */
		<T extends B> T filter(Filter filter, String... urlPatterns);

		/**
		 * Define default request properties that should be merged into all
		 * performed requests such that input from the client request override
		 * the default properties defined here.
		 * <p>This is delegated to
		 * {@link ConfigurableMockMvcBuilder#defaultRequest(RequestBuilder)}.
		 */
		<T extends B> T defaultRequest(RequestBuilder requestBuilder);

		/**
		 * Define a global expectation that should <em>always</em> be applied to
		 * every response.
		 * <p>This is delegated to
		 * {@link ConfigurableMockMvcBuilder#alwaysExpect(ResultMatcher)}.
		 */
		<T extends B> T alwaysExpect(ResultMatcher resultMatcher);

		/**
		 * Whether to handle HTTP OPTIONS requests.
		 * <p>This is delegated to
		 * {@link ConfigurableMockMvcBuilder#dispatchOptions(boolean)}.
		 */
		<T extends B> T dispatchOptions(boolean dispatchOptions);

		/**
		 * Allow customization of {@code DispatcherServlet}.
		 * <p>This is delegated to
		 * {@link ConfigurableMockMvcBuilder#addDispatcherServletCustomizer(DispatcherServletCustomizer)}.
		 */
		<T extends B> T dispatcherServletCustomizer(DispatcherServletCustomizer customizer);

		/**
		 * Add a {@code MockMvcConfigurer} that automates MockMvc setup.
		 * <p>This is delegated to
		 * {@link ConfigurableMockMvcBuilder#apply(MockMvcConfigurer)}.
		 */
		<T extends B> T apply(MockMvcConfigurer configurer);


		/**
		 * Proceed to configure and build the test client.
		 */
		Builder configureClient();

		/**
		 * Shortcut to build the test client.
		 */
		RestTestClient build();
	}

	/**
	 * Specification for providing request headers and the URI of a request.
	 *
	 * @param <S> a self reference to the spec type
	 */
	interface RequestHeadersUriSpec<S extends RequestHeadersSpec<S>> extends UriSpec<S>, RequestHeadersSpec<S> {
	}

	/**
	 * Specification for providing the body and the URI of a request.
	 */
	interface RequestBodyUriSpec extends RequestBodySpec, RequestHeadersUriSpec<RequestBodySpec> {
	}

	/**
	 * Chained API for applying assertions to a response.
	 */
	interface ResponseSpec {
		/**
		 * Assertions on the response status.
		 */
		StatusAssertions expectStatus();

		/**
		 * Consume and decode the response body to {@code byte[]} and then apply
		 * assertions on the raw content (for example, isEmpty, JSONPath, etc.).
		 */
		BodyContentSpec expectBody();

		/**
		 * Consume and decode the response body to a single object of type
		 * {@code <B>} and then apply assertions.
		 * @param bodyType the expected body type
		 */
		<B> BodySpec<B, ?> expectBody(Class<B> bodyType);


		/**
		 * Assertions on the cookies of the response.
		 */
		CookieAssertions expectCookie();

		/**
		 * Assertions on the headers of the response.
		 */
		HeaderAssertions expectHeader();

		/**
		 * Apply multiple assertions to a response with the given
		 * {@linkplain RestTestClient.ResponseSpec.ResponseSpecConsumer consumers}, with the guarantee that
		 * all assertions will be applied even if one or more assertions fails
		 * with an exception.
		 * <p>If a single {@link Error} or {@link RuntimeException} is thrown,
		 * it will be rethrown.
		 * <p>If multiple exceptions are thrown, this method will throw an
		 * {@link AssertionError} whose error message is a summary of all the
		 * exceptions. In addition, each exception will be added as a
		 * {@linkplain Throwable#addSuppressed(Throwable) suppressed exception} to
		 * the {@code AssertionError}.
		 * <p>This feature is similar to the {@code SoftAssertions} support in
		 * AssertJ and the {@code assertAll()} support in JUnit Jupiter.
		 *
		 * <h4>Example</h4>
		 * <pre class="code">
		 * restTestClient.get().uri("/hello").exchange()
		 *     .expectAll(
		 *         responseSpec -&gt; responseSpec.expectStatus().isOk(),
		 *         responseSpec -&gt; responseSpec.expectBody(String.class).isEqualTo("Hello, World!")
		 *     );
		 * </pre>
		 * @param consumers the list of {@code ResponseSpec} consumers
		 * @since 5.3.10
		 */
		ResponseSpec expectAll(ResponseSpecConsumer... consumers);

		/**
		 * {@link Consumer} of a {@link RestTestClient.ResponseSpec}.
		 * @see RestTestClient.ResponseSpec#expectAll(RestTestClient.ResponseSpec.ResponseSpecConsumer...)
		 */
		@FunctionalInterface
		interface ResponseSpecConsumer extends Consumer<ResponseSpec> {
		}
	}

	/**
	 * Spec for expectations on the response body content.
	 */
	interface BodyContentSpec {

	}

	/**
	 * Spec for expectations on the response body decoded to a single Object.
	 *
	 * @param <S> a self reference to the spec type
	 * @param <B> the body type
	 */
	interface BodySpec<B, S extends BodySpec<B, S>> {
		/**
		 * Exit the chained API and return an {@code ResponseEntity} with the
		 * decoded response content.
		 */
		ResponseEntity<B> returnResult();
	}

	/**
	 * Specification for providing the URI of a request.
	 *
	 * @param <S> a self reference to the spec type
	 */
	interface UriSpec<S extends RequestHeadersSpec<?>> {
		/**
		 * Specify the URI using an absolute, fully constructed {@link java.net.URI}.
		 * <p>If a {@link UriBuilderFactory} was configured for the client with
		 * a base URI, that base URI will <strong>not</strong> be applied to the
		 * supplied {@code java.net.URI}. If you wish to have a base URI applied to a
		 * {@code java.net.URI} you must invoke either {@link #uri(String, Object...)}
		 * or {@link #uri(String, Map)} &mdash; for example, {@code uri(myUri.toString())}.
		 * @return spec to add headers or perform the exchange
		 */
		S uri(URI uri);
	}




	/**
	 * Specification for adding request headers and performing an exchange.
	 *
	 * @param <S> a self reference to the spec type
	 */
	interface RequestHeadersSpec<S extends RequestHeadersSpec<S>> {

		/**
		 * Add the given, single header value under the given name.
		 * @param headerName  the header name
		 * @param headerValues the header value(s)
		 * @return the same instance
		 */
		S header(String headerName, String... headerValues);

		/**
		 * Perform the exchange without a request body.
		 * @return spec for decoding the response
		 */
		ResponseSpec exchange();
	}

	/**
	 * Specification for providing body of a request.
	 */
	interface RequestBodySpec extends RequestHeadersSpec<RequestBodySpec> {
	}

		interface Builder {
		/**
		 * Build the {@link RestTestClient} instance.
		 */
		RestTestClient build();
	}
}
