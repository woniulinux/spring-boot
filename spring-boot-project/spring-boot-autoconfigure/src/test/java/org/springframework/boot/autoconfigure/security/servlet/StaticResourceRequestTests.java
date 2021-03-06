/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.autoconfigure.security.servlet;

import javax.servlet.http.HttpServletRequest;

import org.assertj.core.api.AssertDelegateTarget;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.boot.autoconfigure.security.StaticResourceLocation;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.StaticWebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link StaticResourceRequest}.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 */
public class StaticResourceRequestTests {

	private StaticResourceRequest resourceRequest = StaticResourceRequest.get();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void atCommonLocationsShouldMatchCommonLocations() {
		RequestMatcher matcher = this.resourceRequest.atCommonLocations();
		assertMatcher(matcher).matches("/css/file.css");
		assertMatcher(matcher).matches("/js/file.js");
		assertMatcher(matcher).matches("/images/file.css");
		assertMatcher(matcher).matches("/webjars/file.css");
		assertMatcher(matcher).matches("/foo/favicon.ico");
		assertMatcher(matcher).doesNotMatch("/bar");
	}

	@Test
	public void atCommonLocationsWithExcludeShouldNotMatchExcluded() {
		RequestMatcher matcher = this.resourceRequest.atCommonLocations()
				.excluding(StaticResourceLocation.CSS);
		assertMatcher(matcher).doesNotMatch("/css/file.css");
		assertMatcher(matcher).matches("/js/file.js");
	}

	@Test
	public void atLocationShouldMatchLocation() {
		RequestMatcher matcher = this.resourceRequest.at(StaticResourceLocation.CSS);
		assertMatcher(matcher).matches("/css/file.css");
		assertMatcher(matcher).doesNotMatch("/js/file.js");
	}

	@Test
	public void atLocationWhenHasServletPathShouldMatchLocation() {
		ServerProperties serverProperties = new ServerProperties();
		serverProperties.getServlet().setPath("/foo");
		RequestMatcher matcher = this.resourceRequest.at(StaticResourceLocation.CSS);
		assertMatcher(matcher, serverProperties).matches("/foo", "/css/file.css");
		assertMatcher(matcher, serverProperties).doesNotMatch("/foo", "/js/file.js");
	}

	@Test
	public void atLocationsFromSetWhenSetIsNullShouldThrowException() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Locations must not be null");
		this.resourceRequest.at(null);
	}

	@Test
	public void excludeFromSetWhenSetIsNullShouldThrowException() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Locations must not be null");
		this.resourceRequest.atCommonLocations()
				.excluding(null);
	}

	private RequestMatcherAssert assertMatcher(RequestMatcher matcher) {
		StaticWebApplicationContext context = new StaticWebApplicationContext();
		context.registerBean(ServerProperties.class);
		return assertThat(new RequestMatcherAssert(context, matcher));
	}

	private RequestMatcherAssert assertMatcher(RequestMatcher matcher,
			ServerProperties serverProperties) {
		StaticWebApplicationContext context = new StaticWebApplicationContext();
		context.registerBean(ServerProperties.class, () -> serverProperties);
		return assertThat(new RequestMatcherAssert(context, matcher));
	}

	private static class RequestMatcherAssert implements AssertDelegateTarget {

		private final WebApplicationContext context;

		private final RequestMatcher matcher;

		RequestMatcherAssert(WebApplicationContext context, RequestMatcher matcher) {
			this.context = context;
			this.matcher = matcher;
		}

		public void matches(String path) {
			matches(mockRequest(path));
		}

		public void matches(String servletPath, String path) {
			matches(mockRequest(servletPath, path));
		}

		private void matches(HttpServletRequest request) {
			assertThat(this.matcher.matches(request))
					.as("Matches " + getRequestPath(request)).isTrue();
		}

		public void doesNotMatch(String path) {
			doesNotMatch(mockRequest(path));
		}

		public void doesNotMatch(String servletPath, String path) {
			doesNotMatch(mockRequest(servletPath, path));
		}

		private void doesNotMatch(HttpServletRequest request) {
			assertThat(this.matcher.matches(request))
					.as("Does not match " + getRequestPath(request)).isFalse();
		}

		private MockHttpServletRequest mockRequest(String path) {
			return mockRequest(null, path);
		}

		private MockHttpServletRequest mockRequest(String servletPath, String path) {
			MockServletContext servletContext = new MockServletContext();
			servletContext.setAttribute(
					WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE,
					this.context);
			MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
			if (servletPath != null) {
				request.setServletPath(servletPath);
			}
			request.setPathInfo(path);
			return request;
		}

		private String getRequestPath(HttpServletRequest request) {
			String url = request.getServletPath();
			if (request.getPathInfo() != null) {
				url += request.getPathInfo();
			}
			return url;
		}

	}

}
