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

import static org.apache.commons.lang3.Validate.notNull;

import java.io.IOException;
import java.util.Locale;

import org.springframework.context.MessageSource;

import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;

/**
 * <p>
 * A helper that delegates to a {@link MessageSource} instance.
 * </p>
 * Usage:
 *
 * <pre>
 *  {{message "code" args* [default="default message"] }}
 * </pre>
 *
 * Where:
 * <ul>
 * <li>code: String literal. Required.</li>
 * <li>args: Object. Optional</li>
 * <li>default: A default message. Optional.</li>
 * </ul>
 * This helper have resolve locale from a "locale" context attribute
 *
 * @author sdeleuze
 * @author edgar.espina
 */
public class MessageSourceHelper implements Helper<String> {

    /**
     * A message source. Required.
     */
    private MessageSource messageSource;

    /**
     * Creates a new {@link MessageSourceHelper}.
     *
     * @param messageSource The message source. Required.
     */
    public MessageSourceHelper(final MessageSource messageSource) {
        this.messageSource = notNull(messageSource, "A message source is required.");
    }

    @Override
    public Object apply(final String code, final Options options)
            throws IOException {
        Object[] args = options.params;
        String defaultMessage = options.hash("default");
        return messageSource.getMessage(code, args, defaultMessage, currentLocale(options));
    }

    /**
     * Resolve the current user locale.
     *
     * @return The current user locale.
     */
    protected Locale currentLocale(Options options) {
        return Locale.forLanguageTag((String)options.context.get("locale"));
    }
}
