package com.googlecode.jmxtrans.model.output;

import com.google.common.collect.ImmutableList;
import com.googlecode.jmxtrans.exceptions.LifecycleException;
import com.googlecode.jmxtrans.model.OutputWriter;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.ValidationException;

import java.util.Map;

import static java.util.Collections.emptyMap;

public class BooleanAsNumberOutputWriter implements OutputWriter {
	@Override
	public void start() throws LifecycleException {
	}

	@Override
	public void stop() throws LifecycleException {
	}

	@Override
	public void doWrite(Server server, Query query, ImmutableList<Result> results) throws Exception {

	}

	@Override
	public Map<String, Object> getSettings() {
		return emptyMap();
	}

	@Override
	public void setSettings(Map<String, Object> settings) {
	}

	@Override
	public void validateSetup(Server server, Query query) throws ValidationException {
	}
}
