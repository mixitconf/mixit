/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.reactive.result.view.mustache;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Mustache.Compiler;

import com.samskivert.mustache.Template;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Mono;

import org.springframework.core.io.Resource;
import org.springframework.web.reactive.result.view.UrlBasedViewResolver;
import org.springframework.web.reactive.result.view.View;
import org.springframework.web.reactive.result.view.ViewResolver;

/**
 * Spring MVC {@link ViewResolver} for Mustache.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Sebastien Deleuze
 */
public class MustacheViewResolver extends UrlBasedViewResolver {

	private Compiler compiler = Mustache.compiler().escapeHTML(false);

	private Charset charset = StandardCharsets.UTF_8;

	protected static final Log logger = LogFactory.getLog(MustacheViewResolver.class);


	public MustacheViewResolver() {
		setViewClass(requiredViewClass());
	}

	@Override
	protected Class<?> requiredViewClass() {
		return MustacheView.class;
	}

	/**
	 * Set the compiler.
	 * @param compiler the compiler
	 */
	public void setCompiler(Compiler compiler) {
		this.compiler = compiler;
	}

	/**
	 * Set the charset.
	 * @param charset the charset
	 */
	public void setCharset(Charset charset) {
		this.charset = charset;
	}


	@Override
	public Mono<View> resolveViewName(String viewName, Locale locale) {
		Resource resource = getApplicationContext().getResource(getPrefix() + viewName + getSuffix());
		return super.resolveViewName(viewName, locale).map(view -> {
			((MustacheView)view).setTemplate(createTemplate(resource));
			return view;
		});
	}

	private Template createTemplate(Resource resource) {

		try (Reader reader = new InputStreamReader(resource.getInputStream(), this.charset)) {
			return this.compiler.compile(reader);
		}
		catch(IOException ioe) {
			throw new IllegalStateException(ioe);
		}
	}

}
