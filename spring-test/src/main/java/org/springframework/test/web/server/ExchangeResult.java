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

import java.io.IOException;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.web.client.RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse;
import static org.springframework.test.util.AssertionErrors.*;

/**
 * @author Rob Worsnop
 */
public class ExchangeResult {
	private static final Log logger = LogFactory.getLog(ExchangeResult.class);

	/** Ensure single logging; for example, for expectAll. */
	private boolean diagnosticsLogged;

	private static final List<MediaType> PRINTABLE_MEDIA_TYPES = List.of(
			MediaType.parseMediaType("application/*+json"), MediaType.APPLICATION_XML,
			MediaType.parseMediaType("text/*"), MediaType.APPLICATION_FORM_URLENCODED);

	private final @Nullable ConvertibleClientHttpResponse clientResponse;

	public ExchangeResult(@Nullable ConvertibleClientHttpResponse clientResponse) {
		this.clientResponse = clientResponse;
	}

	public HttpStatusCode getStatus() {
		try {
			assertNotNull("clientResponse unexpectedly null", clientResponse);
			return clientResponse.getStatusCode();
		} catch (IOException e) {
			throw new AssertionError(e);
		}
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
}
