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
package com.googlecode.jmxtrans.model.output;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.googlecode.jmxtrans.ConfigurationParser;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.ValidationException;
import org.apache.commons.pool.impl.GenericKeyedObjectPool;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.collect.ImmutableList.of;

public class GraphiteWriterTests {

	@Test(expected = NullPointerException.class)
	public void hostIsRequired() throws ValidationException {
		try {
			GraphiteWriter.builder()
					.setPort(123)
					.build();
		} catch (NullPointerException npe) {
			Assertions.assertThat(npe).hasMessage("Host cannot be null.");
			throw npe;
		}
	}

	@Test(expected = NullPointerException.class)
	public void portIsRequired() throws ValidationException {
		try {
			GraphiteWriter.builder()
					.setHost("localhost")
					.build();
		} catch (NullPointerException npe) {
			Assertions.assertThat(npe).hasMessage("Port cannot be null.");
			throw npe;
		}
	}
	
	private static GraphiteWriter getGraphiteWriter(OutputStream out) throws Exception {
		return getGraphiteWriter(out, new ArrayList<String>());
	}

	private static GraphiteWriter getGraphiteWriter(OutputStream out, List<String> typeNames) throws Exception {
		GenericKeyedObjectPool<InetSocketAddress, Socket> pool = Mockito.mock(GenericKeyedObjectPool.class);
		Socket socket = Mockito.mock(Socket.class);
		Mockito.when(pool.borrowObject(Matchers.any(InetSocketAddress.class))).thenReturn(socket);

		Mockito.when(socket.getOutputStream()).thenReturn(out);

		GraphiteWriter writer = GraphiteWriter.builder()
				.setHost("localhost")
				.setPort(2003)
				.addTypeNames(typeNames)
				.build();
		writer.setPool(pool);

		return writer;
	}

	private static String getOutput(Server server, Query query, Result result) throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		GraphiteWriter writer = getGraphiteWriter(out);
		writer.doWrite(server, query, of(result));
		return out.toString();
	}

	@Test
	public void writeSingleResult() throws Exception {
		Server server = Server.builder().setHost("host").setPort("123").build();
		Query query = Query.builder().build();
		Result result = new Result(System.currentTimeMillis(), "attributeName", "className", "objDomain", "classNameAlias", "typeName", ImmutableMap.of("key", (Object)1));

		// check that Graphite format is respected
		Assertions.assertThat(getOutput(server, query, result))
				.startsWith("servers.host_123.classNameAlias.attributeName_key 1 ");
	}

	@Test
	public void useObjDomainWorks() throws Exception {
		Server server = Server.builder().setHost("host").setPort("123").build();
		// Set useObjDomain to true
		Query query = Query.builder().setUseObjDomainAsKey(true).build();
		Result result = new Result(System.currentTimeMillis(), "attributeName", "className", "objDomain", null, "typeName", ImmutableMap.of("key", (Object)1));

		// check that Graphite format is respected
		Assertions.assertThat(getOutput(server, query, result))
				.startsWith("servers.host_123.objDomain.attributeName_key 1 ");
	}
	
	@Test
	public void allowDottedWorks() throws Exception {
		Server server = Server.builder().setHost("host").setPort("123").setAlias("host").build();
		// Set allowDottedKeys to true
		Query query = Query.builder().setAllowDottedKeys(true).build();
		Result result = new Result(System.currentTimeMillis(), "attributeName", "className", "objDomain", null, "typeName", ImmutableMap.of("key", (Object)1));

		// check that Graphite format is respected
		Assertions.assertThat(getOutput(server, query, result))
				.startsWith("servers.host.className.attributeName.key 1 ");
	}

	@Test
	public void useAllTypeNamesWorks() throws Exception {
		Server server = Server.builder().setHost("host").setPort("123").setAlias("host").build();
		// Set useAllTypeNames to true
		Query query = Query.builder().setUseAllTypeNames(true).build();
		String typeName = "typeName,typeNameKey1=typeNameValue1,typeNameKey2=typeNameValue2";
		String typeNameReordered = "typeNameKey2=typeNameValue2,typeName,typeNameKey1=typeNameValue1";
		Result result = new Result(System.currentTimeMillis(), "attributeName", "className", "objDomain", null,
				typeName, ImmutableMap.of("key", (Object)1));
		Result resultWithTypeNameReordered = new Result(System.currentTimeMillis(), "attributeName", "className", "objDomain", null,
				typeNameReordered, ImmutableMap.of("key", (Object)1));

		// check that Graphite format is respected
		Assertions.assertThat(getOutput(server, query, result))
				.startsWith("servers.host.className.typeNameValue1_typeNameValue2.attributeName_key 1 ");
		Assertions.assertThat(getOutput(server, query, resultWithTypeNameReordered))
				.startsWith("servers.host.className.typeNameValue2_typeNameValue1.attributeName_key 1 ");
	}

	@Test
	public void booleanAsNumberWorks() throws Exception {
		File testInput = new File(GraphiteWriterTests.class.getResource("/booleanTest.json").toURI());

		boolean continueOnJsonError = true;

		ImmutableList servers = new ConfigurationParser().parseServers(of(testInput), continueOnJsonError);

		Result result = new Result(System.currentTimeMillis(), "attributeName", "className", "objDomain", null, "typeName", ImmutableMap.of("key", (Object)true));

		ByteArrayOutputStream out = new ByteArrayOutputStream();

		GenericKeyedObjectPool<InetSocketAddress, Socket> pool = Mockito.mock(GenericKeyedObjectPool.class);
		Socket socket = Mockito.mock(Socket.class);
		Mockito.when(pool.borrowObject(Matchers.any(InetSocketAddress.class))).thenReturn(socket);

		Mockito.when(socket.getOutputStream()).thenReturn(out);

		Server server = ((Server)servers.get(0));
		Query query = server.getQueries().asList().get(0);
		GraphiteWriter writer = (GraphiteWriter) (query.getOutputWriters().get(0));
		writer.setPool(pool);

		writer.doWrite(server, query, of(result));

		// check that the booleanAsNumber property was picked up from the JSON
		Assertions.assertThat(out.toString()).startsWith("servers.host_123.objDomain.attributeName.key 1");
	}
	
	@Test
	public void checkEmptyTypeNamesAreIgnored() throws Exception {
		Server server = Server.builder().setHost("host").setPort("123").build();
		// Set useObjDomain to true
		Query query = Query.builder()
				.setUseObjDomainAsKey(true)
				.setAllowDottedKeys(true)
				.setObj("\"yammer.metrics\":name=\"uniqueName\",type=\"\"").build();

		Result result = new Result(System.currentTimeMillis(),
				"Attribute",
				"com.yammer.metrics.reporting.JmxReporter$Counter",
				"yammer.metrics",
				null,
				"name=\"uniqueName\",type=\"\"",
				ImmutableMap.of("Attribute", (Object)0));

		ByteArrayOutputStream out = new ByteArrayOutputStream();

		ArrayList<String> typeNames = new ArrayList<String>();
		typeNames.add("name");
		typeNames.add("type");
		GraphiteWriter writer = getGraphiteWriter(out, typeNames);

		writer.doWrite(server, query, of(result));

		// check that the empty type "type" is ignored when allowDottedKeys is true
		Assertions.assertThat(out.toString()).startsWith("servers.host_123.yammer.metrics.uniqueName.Attribute 0 ");
		
		// check that this also works when literal " characters aren't included in the JMX ObjectName
		query = Query.builder()
				.setUseObjDomainAsKey(true)
				.setAllowDottedKeys(true)
				.setObj("yammer.metrics:name=uniqueName,type=").build();
		
		out = new ByteArrayOutputStream();
		writer = getGraphiteWriter(out, typeNames);
		
		writer.doWrite(server, query, of(result));
		Assertions.assertThat(out.toString()).startsWith("servers.host_123.yammer.metrics.uniqueName.Attribute 0 ");
		
		// check that the empty type "type" is ignored when allowDottedKeys is false
		query = Query.builder()
				.setUseObjDomainAsKey(true)
				.setAllowDottedKeys(false)
				.setObj("\"yammer.metrics\":name=\"uniqueName\",type=\"\"").build();
		
		out = new ByteArrayOutputStream();
		writer = getGraphiteWriter(out, typeNames);
		
		writer.doWrite(server, query, of(result));
		Assertions.assertThat(out.toString()).startsWith("servers.host_123.yammer_metrics.uniqueName.Attribute 0 ");
	}

	@Test
	public void socketInvalidatedWhenError() throws Exception {
		// a lot of setup for not much of a test ...
		Server server = Server.builder().setHost("host").setPort("123").build();
		Query query = Query.builder().build();
		Result result = new Result(System.currentTimeMillis(), "attributeName", "className", "objDomain", "classNameAlias", "typeName", ImmutableMap.of("key", (Object)1));

		GenericKeyedObjectPool<InetSocketAddress, Socket> pool = Mockito.mock(GenericKeyedObjectPool.class);
		Socket socket = Mockito.mock(Socket.class);
		Mockito.when(pool.borrowObject(Matchers.any(InetSocketAddress.class))).thenReturn(socket);
		UnflushableByteArrayOutputStream out = new UnflushableByteArrayOutputStream();
		Mockito.when(socket.getOutputStream()).thenReturn(out);

		GraphiteWriter writer = GraphiteWriter.builder()
				.setHost("localhost")
				.setPort(2003)
				.build();
		writer.setPool(pool);

		writer.doWrite(server, query, of(result));
		Mockito.verify(pool).invalidateObject(Matchers.any(InetSocketAddress.class), Matchers.eq(socket));
		Mockito.verify(pool, Mockito.never()).returnObject(Matchers.any(InetSocketAddress.class), Matchers.eq(socket));
	}

	private static class UnflushableByteArrayOutputStream extends ByteArrayOutputStream {
		@Override
		public void flush() throws IOException {
			throw new IOException();
		}
	}

}
