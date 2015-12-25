/*******************************************************************************
 * Copyright 2011-2014 Sergey Tarasevich
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
package com.nostra13.universalimageloader.cache.memory;

import android.graphics.Bitmap;

import java.lang.ref.Reference;
import java.util.*;

/**
 * 基于MemoryCache实现抽象类，实现了内存缓存器中的相关公共方法
 * Base memory cache. Implements common functionality for memory cache. Provides object references (
 * {@linkplain Reference not strong}) storing.
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @since 1.0.0
 */
public abstract class BaseMemoryCache implements MemoryCache {

	/**
	 * Stores not strong references to objects
	 * 进行维护图片信息的软引用map对象
	 */
	private final Map<String, Reference<Bitmap>> softMap = Collections.synchronizedMap(new HashMap<String, Reference<Bitmap>>());

	/**
	 * get(key)的具体方法实现，从软引用中根据key来获取图片
	 * @param key
	 * @return
	 */
	@Override
	public Bitmap get(String key) {
		Bitmap result = null;
		Reference<Bitmap> reference = softMap.get(key);
		if (reference != null) {
			result = reference.get();
		}
		return result;
	}

	/**
	 * 根据key 和图片  把数据加入到软引用中
	 * @param key
	 * @param value
	 * @return
	 */
	@Override
	public boolean put(String key, Bitmap value) {
		softMap.put(key, createReference(value));
		return true;
	}

	/**
	 * 根据key，从map对象中删除该对应的图片对象，并且返回该删除的图片
	 * @param key
	 * @return
	 */
	@Override
	public Bitmap remove(String key) {
		Reference<Bitmap> bmpRef = softMap.remove(key);
		return bmpRef == null ? null : bmpRef.get();
	}

	@Override
	public Collection<String> keys() {
		synchronized (softMap) {
			return new HashSet<String>(softMap.keySet());
		}
	}

	/**
	 * 清除对象软引用map中的数据
	 */
	@Override
	public void clear() {
		softMap.clear();
	}

	/**
	 * Creates {@linkplain Reference not strong} reference of value
	 * 图片对象包装的引用对象  这边采用抽象方法，具体进行扩展，让具体的内存缓存器分别实现
	 */
	protected abstract Reference<Bitmap> createReference(Bitmap value);
}
