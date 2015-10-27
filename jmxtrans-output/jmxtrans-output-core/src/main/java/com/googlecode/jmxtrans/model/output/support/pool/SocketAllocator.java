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
package com.googlecode.jmxtrans.model.output.support.pool;

import com.google.common.io.Closer;
import stormpot.Allocator;
import stormpot.Slot;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.charset.Charset;

public class SocketAllocator implements Allocator<SocketPoolable> {

	private final InetSocketAddress server;
	private final int socketTimeoutMillis;
	private final Charset charset;

	public SocketAllocator(InetSocketAddress server, int socketTimeoutMillis, Charset charset) {
		this.server = server;
		this.socketTimeoutMillis = socketTimeoutMillis;
		this.charset = charset;
	}

	@Override
	public SocketPoolable allocate(Slot slot) throws Exception {
		// create new InetSocketAddress to ensure name resolution is done again
		SocketAddress serverAddress = new InetSocketAddress(server.getHostName(), server.getPort());
		Socket socket = new Socket();
		socket.setKeepAlive(false);
		socket.connect(serverAddress, socketTimeoutMillis);

		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), charset));

		return new SocketPoolable(slot, socket, writer);
	}

	@Override
	public void deallocate(SocketPoolable poolable) throws Exception {
		Closer closer = Closer.create();
		try {
			closer.register(poolable.getSocket());
			closer.register(poolable.getWriter());
		} catch (Throwable t) {
			closer.rethrow(t);
		} finally {
			closer.close();
		}
	}
}
