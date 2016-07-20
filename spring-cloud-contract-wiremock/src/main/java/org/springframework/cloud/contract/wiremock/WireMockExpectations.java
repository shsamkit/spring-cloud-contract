/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.contract.wiremock;

import java.io.IOException;
import java.nio.charset.Charset;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;

import com.github.tomakehurst.wiremock.common.Json;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * @author Dave Syer
 *
 */
public class WireMockExpectations {

	private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

	private String prefix = "classpath:/stubs/";

	private String suffix = ".json";

	private String baseUrl = "";

	private MockRestServiceServer server;

	private WireMockExpectations(RestTemplate restTemplate) {
		this.server = MockRestServiceServer.bindTo(restTemplate).build();
	}

	public WireMockExpectations baseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
		return this;
	}

	public WireMockExpectations prefix(String prefix) {
		this.prefix = prefix;
		return this;
	}
	public WireMockExpectations suffix(String suffix) {
		this.suffix = suffix;
		return this;
	}

	public MockRestServiceServer expect(String... locations) {
		for (String location : locations) {
			try {
				for (Resource resource : this.resolver.getResources(this.prefix + location + this.suffix)) {
					StubMapping mapping;
					mapping = Json.read(StreamUtils.copyToString(resource.getInputStream(), Charset.defaultCharset()),
							StubMapping.class);
					this.server.expect(requestTo(this.baseUrl + mapping.getRequest().getUrlPath()))
							.andRespond(withSuccess(mapping.getResponse().getBody(), MediaType.TEXT_PLAIN));
				}
			}
			catch (IOException e) {
				throw new IllegalStateException("Cannot load resources for: " + location, e);
			}
		}
		return this.server;
	}

	public static WireMockExpectations with(RestTemplate restTemplate) {
		return new WireMockExpectations(restTemplate);
	}

}
