/**
 * The MIT License
 * Copyright (c) 2010 JmxTrans team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.googlecode.jmxtrans.model.output.support;

import com.google.common.collect.ImmutableList;
import com.googlecode.jmxtrans.exceptions.LifecycleException;
import com.googlecode.jmxtrans.model.OutputWriter;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.ValidationException;
import com.googlecode.jmxtrans.model.results.BooleanAsNumberValueTransformer;
import com.googlecode.jmxtrans.model.results.IdentityValueTransformer;
import com.googlecode.jmxtrans.model.results.ResultValuesTransformer;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Map;

import static com.google.common.collect.FluentIterable.from;

public class ResultTransformerOutputWriter<T extends OutputWriter> implements OutputWriter {

	@Nonnull private final ResultValuesTransformer resultValuesTransformer;
	@Nonnull private final T target;

	public ResultTransformerOutputWriter(@Nonnull ResultValuesTransformer resultValuesTransformer, @Nonnull T target) {
		this.resultValuesTransformer = resultValuesTransformer;
		this.target = target;
	}

	@Override
	public void start() throws LifecycleException {
	}

	@Override
	public void stop() throws LifecycleException {
	}

	@Override
	public void doWrite(Server server, Query query, ImmutableList<Result> results) throws Exception {
		target.doWrite(
				server,
				query,
				from(results).transform(resultValuesTransformer).toList());
	}

	@Override
	public Map<String, Object> getSettings() {
		return Collections.emptyMap();
	}

	@Override
	public void setSettings(Map<String, Object> settings) {
	}

	@Override
	public void validateSetup(Server server, Query query) throws ValidationException {
	}

	public static <T extends OutputWriter> ResultTransformerOutputWriter<T> booleanToNumber(boolean booleanToNumber, T target) {
		if (booleanToNumber) return booleanToNumber(target);
		return identity(target);
	}

	public static <T extends OutputWriter> ResultTransformerOutputWriter<T> booleanToNumber(T target) {
		return new ResultTransformerOutputWriter<T>(new ResultValuesTransformer(new BooleanAsNumberValueTransformer(1, 0)), target);
	}

	public static <T extends OutputWriter> ResultTransformerOutputWriter<T> identity(T target) {
		return new ResultTransformerOutputWriter<T>(new ResultValuesTransformer(new IdentityValueTransformer()), target);
	}


}
