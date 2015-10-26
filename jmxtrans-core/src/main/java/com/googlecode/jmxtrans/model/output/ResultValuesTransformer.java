package com.googlecode.jmxtrans.model.output;

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.results.ValueTransformer;

import javax.annotation.Nullable;

final class ResultValuesTransformer implements Function<Result, Result> {

	private final ValueTransformer valueTransformer;

	ResultValuesTransformer(ValueTransformer valueTransformer) {
		this.valueTransformer = valueTransformer;
	}

	@Nullable
	@Override
	public Result apply(@Nullable Result input) {
		if (input == null) {
			return null;
		}
		return new Result(
				input.getEpoch(),
				input.getAttributeName(),
				input.getClassName(),
				input.getObjDomain(),
				input.getKeyAlias(),
				input.getTypeName(),
				Maps.transformValues(input.getValues(), valueTransformer)
		);
	}
}
