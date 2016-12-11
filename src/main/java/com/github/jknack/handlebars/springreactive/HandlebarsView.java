/**
 * Copyright (c) 2012-2015 Edgar Espina
 * <p>
 * This file is part of Handlebars.java.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.jknack.handlebars.springreactive;

import static org.apache.commons.lang3.Validate.notEmpty;
import static org.apache.commons.lang3.Validate.notNull;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.result.view.AbstractUrlBasedView;

import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.ValueResolver;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * A Handlebars Spring Web Reactive view implementation.
 *
 * @author sdeleuze
 * @author edgar.espina
 */
public class HandlebarsView extends AbstractUrlBasedView {

    /**
     * The compiled template.
     */
    private Template template;

    /**
     * The value's resolvers.
     */
    private ValueResolver[] valueResolvers;

    /**
     * Merge model into the view. {@inheritDoc}
     */
    @Override
    protected Mono<Void> renderInternal(Map<String, Object> model, MediaType contentType,
                                        ServerWebExchange exchange) {
        Context context = Context.newBuilder(model)
                .resolver(valueResolvers)
                .combine("locale", exchange.getAttribute("locale").get().toString())
                .build();
        DataBuffer dataBuffer = exchange.getResponse().bufferFactory().allocateBuffer();
        try {
            Charset charset = getCharset(contentType).orElse(getDefaultCharset());
            Writer writer = new OutputStreamWriter(dataBuffer.asOutputStream(), charset);
            template.apply(context, writer);
            writer.flush();
        } catch (IOException ex) {
            String message = "Could not load Handlebars template for URL [" + getUrl() + "]";
            return Mono.error(new IllegalStateException(message, ex));
        } catch (Throwable ex) {
            return Mono.error(ex);
        } finally {
            context.destroy();
        }
        return exchange.getResponse().writeWith(Flux.just(dataBuffer));
    }

    private static Optional<Charset> getCharset(MediaType mediaType) {
        return mediaType != null ? Optional.ofNullable(mediaType.getCharset()) : Optional.empty();
    }

    /**
     * @return The underlying template for this view.
     */
    public Template getTemplate() {
        return template;
    }

    @Override
    public boolean checkResourceExists(Locale locale) throws Exception {
        return template != null;
    }

    /**
     * Set the compiled template.
     *
     * @param template The compiled template. Required.
     */
    void setTemplate(final Template template) {
        this.template = notNull(template, "A handlebars template is required.");
    }

    /**
     * Set the value resolvers.
     *
     * @param valueResolvers The value resolvers. Required.
     */
    void setValueResolver(final ValueResolver... valueResolvers) {
        this.valueResolvers = notEmpty(valueResolvers,
                "At least one value-resolver must be present.");
    }

}
