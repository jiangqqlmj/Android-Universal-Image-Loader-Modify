/*******************************************************************************
 * Copyright 2013-2014 Sergey Tarasevich
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.nostra13.universalimageloader.core.assist;

import java.io.IOException;
import java.io.InputStream;

/**
 * 对InputStream流进行包装，给其提供读取等相关方法
 * Decorator for {@link java.io.InputStream InputStream}. Provides possibility to return defined stream length by
 * {@link #available()} method.
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com), Mariotaku
 * @since 1.9.1
 */
public class ContentLengthInputStream extends InputStream {

	private final InputStream stream;
	private final int length;

	/**
	 * 包装器构造器
	 * @param stream
	 * @param length
	 */
	public ContentLengthInputStream(InputStream stream, int length) {
		this.stream = stream;
		this.length = length;
	}

	/**
	 * 返回流的长度
	 * @return
	 */
	@Override
	public int available() {
		return length;
	}

	/**
	 * 进行关闭流
	 * @throws IOException
	 */
	@Override
	public void close() throws IOException {
		stream.close();
	}

	/**
	 * 进行给流标记位置
	 * @param readLimit
	 */
	@Override
	public void mark(int readLimit) {
		stream.mark(readLimit);
	}

	/**
	 * 进行读取
	 * @return
	 * @throws IOException
	 */
	@Override
	public int read() throws IOException {
		return stream.read();
	}

	/**
	 * 采用一个缓冲区数组来读取流
	 * @param buffer
	 * @return
	 * @throws IOException
	 */
	@Override
	public int read(byte[] buffer) throws IOException {
		return stream.read(buffer);
	}

	/**
	 * 采用一个缓冲区数组，从指定的位置，来读取指定长度的数据
	 * @param buffer       缓冲区
	 * @param byteOffset   流指定的位置
	 * @param byteCount    读取指定的长度
	 * @return
	 * @throws IOException
	 */
	@Override
	public int read(byte[] buffer, int byteOffset, int byteCount) throws IOException {
		return stream.read(buffer, byteOffset, byteCount);
	}

	/**
	 * 重置流的位置
	 * @throws IOException
	 */
	@Override
	public void reset() throws IOException {
		stream.reset();
	}

	/**
	 * 流 跳过指定的长度
	 * @param byteCount
	 * @return
	 * @throws IOException
	 */
	@Override
	public long skip(long byteCount) throws IOException {
		return stream.skip(byteCount);
	}

	@Override
	public boolean markSupported() {
		return stream.markSupported();
	}
}