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
package com.nostra13.universalimageloader.cache.memory.impl;

import android.graphics.Bitmap;
import com.nostra13.universalimageloader.cache.memory.LimitedMemoryCache;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * 有限制的缓存
 * Limited {@link Bitmap bitmap} cache. Provides {@link Bitmap bitmaps} storing. Size of all stored bitmaps will not to
 * exceed size limit. When cache reaches limit size then the bitmap which has the largest size is deleted from
 * cache.<br />
 * <br />
 * <b>NOTE:</b> This cache uses strong and weak references for stored Bitmaps. Strong references - for limited count of
 * Bitmaps (depends on cache size), weak references - for all other cached Bitmaps.
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @since 1.0.0
 */
public class LargestLimitedMemoryCache extends LimitedMemoryCache {
	/**
	 * 保存存储对象强引用集合，如果硬缓存大小超过限制，那么最大的缓存会被删除。
	 * (但是该对象还是会存在软引用中，随时可能被GC回收)
	 * Contains strong references to stored objects (keys) and sizes of the objects. If hard cache
	 * size will exceed limit then object with the largest size is deleted (but it continue exist at
	 * {@link #softMap} and can be collected by GC at any time)
	 */
	private final Map<Bitmap, Integer> valueSizes = Collections.synchronizedMap(new HashMap<Bitmap, Integer>());

	/**
	 * 构造器
	 * @param sizeLimit
	 */
	public LargestLimitedMemoryCache(int sizeLimit) {
		super(sizeLimit);
	}

	/**
	 * 进行文件缓存
	 * @param key
	 * @param value
	 * @return
	 */
	@Override
	public boolean put(String key, Bitmap value) {
		if (super.put(key, value)) {
			valueSizes.put(value, getSize(value));
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 进行缓存文件删除
	 * @param key
	 * @return
	 */
	@Override
	public Bitmap remove(String key) {
		Bitmap value = super.get(key);
		if (value != null) {
			valueSizes.remove(value);
		}
		return super.remove(key);
	}

	/**
	 * 进行内存缓存数据清除
	 */
	@Override
	public void clear() {
		valueSizes.clear();
		super.clear();
	}

	@Override
	protected int getSize(Bitmap value) {
		return value.getRowBytes() * value.getHeight();
	}

	@Override
	protected Bitmap removeNext() {
		Integer maxSize = null;
		Bitmap largestValue = null;
		Set<Entry<Bitmap, Integer>> entries = valueSizes.entrySet();
		synchronized (valueSizes) {
			for (Entry<Bitmap, Integer> entry : entries) {
				if (largestValue == null) {
					largestValue = entry.getKey();
					maxSize = entry.getValue();
				} else {
					Integer size = entry.getValue();
					if (size > maxSize) {
						maxSize = size;
						largestValue = entry.getKey();
					}
				}
			}
		}
		valueSizes.remove(largestValue);
		return largestValue;
	}

	@Override
	protected Reference<Bitmap> createReference(Bitmap value) {
		return new WeakReference<Bitmap>(value);
	}
}
