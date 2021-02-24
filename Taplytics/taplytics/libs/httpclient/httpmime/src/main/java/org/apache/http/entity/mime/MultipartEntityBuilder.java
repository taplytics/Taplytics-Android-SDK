/*
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

package org.apache.http.entity.mime;

import org.apache.http.HttpEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.util.Args;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Builder for multipart {@link HttpEntity}s.
 *
 * @since 4.3
 */
public class MultipartEntityBuilder {

	/**
	 * The pool of ASCII chars to be used for generating a multipart boundary.
	 */
	private final static char[] MULTIPART_CHARS = "-_1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

	private final static String DEFAULT_SUBTYPE = "form-data";

	private String subType = DEFAULT_SUBTYPE;
	private HttpMultipartMode mode = HttpMultipartMode.STRICT;
	private String boundary = null;
	private Charset charset = null;
	private List<FormBodyPart> bodyParts = null;

	public static MultipartEntityBuilder create() {
		return new MultipartEntityBuilder();
	}

	MultipartEntityBuilder() {
		super();
	}

	public MultipartEntityBuilder setMode(final HttpMultipartMode mode) {
		this.mode = mode;
		return this;
	}

	public MultipartEntityBuilder setLaxMode() {
		this.mode = HttpMultipartMode.BROWSER_COMPATIBLE;
		return this;
	}

	public MultipartEntityBuilder setStrictMode() {
		this.mode = HttpMultipartMode.STRICT;
		return this;
	}

	public MultipartEntityBuilder setBoundary(final String boundary) {
		this.boundary = boundary;
		return this;
	}

	/**
	 * @since 4.4
	 */
	public MultipartEntityBuilder setMimeSubtype(final String subType) {
		Args.notBlank(subType, "MIME subtype");
		this.subType = subType;
		return this;
	}

	public MultipartEntityBuilder setCharset(final Charset charset) {
		this.charset = charset;
		return this;
	}

	/**
	 * @since 4.4
	 */
	public MultipartEntityBuilder addPart(final FormBodyPart bodyPart) {
		if (bodyPart == null) {
			return this;
		}
		if (this.bodyParts == null) {
			this.bodyParts = new ArrayList<FormBodyPart>();
		}
		this.bodyParts.add(bodyPart);
		return this;
	}

	public MultipartEntityBuilder addPart(final String name, final ContentBody contentBody) {
		Args.notNull(name, "Name");
		Args.notNull(contentBody, "Content body");
		return addPart(new FormBodyPart(name, contentBody));
	}

	private String generateContentType(final String boundary, final String subType, final Charset charset) {
		final StringBuilder buffer = new StringBuilder();
		buffer.append("multipart/").append(subType).append("; boundary=");
		buffer.append(boundary);
		if (charset != null) {
			buffer.append("; charset=");
			buffer.append(charset.name());
		}
		return buffer.toString();
	}

	private String generateBoundary() {
		final StringBuilder buffer = new StringBuilder();
		final Random rand = new Random();
		final int count = rand.nextInt(11) + 30; // a random size from 30 to 40
		for (int i = 0; i < count; i++) {
			buffer.append(MULTIPART_CHARS[rand.nextInt(MULTIPART_CHARS.length)]);
		}
		return buffer.toString();
	}

	MultipartFormEntity buildEntity() {
		final String st = subType != null ? subType : DEFAULT_SUBTYPE;
		final Charset cs = charset;
		final String b = boundary != null ? boundary : generateBoundary();
		final List<FormBodyPart> bps = bodyParts != null ? new ArrayList<FormBodyPart>(bodyParts) : Collections.<FormBodyPart> emptyList();
		final HttpMultipartMode m = mode != null ? mode : HttpMultipartMode.STRICT;
		final AbstractMultipartForm form;
		switch (m) {
		case BROWSER_COMPATIBLE:
			form = new HttpBrowserCompatibleMultipart(st, cs, b, bps);
			break;
		case RFC6532:
			form = new HttpRFC6532Multipart(st, cs, b, bps);
			break;
		default:
			form = new HttpStrictMultipart(st, cs, b, bps);
		}
		return new MultipartFormEntity(form, generateContentType(b, st, cs), form.getTotalLength());
	}

	public HttpEntity build() {
		return buildEntity();
	}

}
