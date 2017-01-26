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
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URLEncoder;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.MustacheException;
import com.samskivert.mustache.Template;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.result.view.AbstractUrlBasedView;
import org.springframework.web.reactive.result.view.View;
import org.springframework.web.server.ServerWebExchange;

/**
 * Spring MVC {@link View} using the Mustache template engine.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Sebastien Deleuze
 */
public class MustacheView extends AbstractUrlBasedView {

    private Template template;

    public MustacheView() {
        setRequestContextAttribute("context");
    }

    public MustacheView(Template template) {
        this.template = template;
    }

    @Override
    protected Mono<Void> renderInternal(Map<String, Object> model, MediaType contentType, ServerWebExchange exchange) {
        if (this.template != null) {

                DataBuffer dataBuffer = exchange.getResponse().bufferFactory().allocateBuffer();
                Writer writer = new OutputStreamWriter(dataBuffer.asOutputStream(), getDefaultCharset());
                Locale locale = exchange.getRequest().getHeaders().getAcceptLanguageAsLocale();
                Optional<String> username = exchange.getSession().block().getAttribute("username");
                if (username.isPresent()) {
                    model.put("username", username.get());
                }
                if (locale != null) {
                    model.put("locale", locale.toString());
                    model.put("localePrefix", locale.getLanguage().equals("en") ? "/en" : "");
                    model.put("en", locale.getLanguage().equals("en"));
                    model.put("fr", locale.getLanguage().equals("fr"));
                }
                model.put("i18n", (Mustache.Lambda) (frag, out) -> {
                    String key = frag.execute();
                    out.write(getApplicationContext().getMessage(key, null, locale));
                });
                model.put("urlEncode", (Mustache.Lambda) (frag, out) -> {
                    out.write(URLEncoder.encode(frag.execute(), "UTF-8"));
                });
                try {
                    this.template.execute(model, writer);
                } catch (MustacheException ex) {
                    return Mono.error(new MustacheException("Error while rendering " + getUrl() + ": " + ex.getMessage()));
                }
                return exchange.getResponse().writeWith(Flux.just(dataBuffer)).doOnSubscribe(s -> {
                    try {
                        writer.flush();
                    } catch (IOException ex) {
                        String message = "Could not load Mustache template for URL [" + getUrl() + "]";
                        throw new IllegalStateException(message, ex);
                    }
                });
        }
        return Mono.empty();
    }

    public void setTemplate(Template template) {
        this.template = template;
    }

    @Override
    public boolean checkResourceExists(Locale locale) throws Exception {
        return getApplicationContext().getResource(getUrl()).exists();
    }

}
