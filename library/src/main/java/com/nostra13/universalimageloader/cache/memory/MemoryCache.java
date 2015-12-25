/*******************************************************************************
 * Copyright 2014 Sergey Tarasevich
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

import java.util.Collection;

/**
 * 内存缓存统一规范接口
 * Interface for memory cache
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @since 1.9.2
 */
public interface MemoryCache {
	/**
	 * 通过key(bitmap)把value加入到缓存中
	 * Puts value into cache by key
	 *
	 * @return <b>true</b> - if value was put into cache successfully, <b>false</b> - if value was <b>not</b> put into
	 * cache
	 */
	boolean put(String key, Bitmap value);

	/**
	 * Returns value by key. If there is no value for key then null will be returned.
	 * 通过key，从缓存中获取图片
	 */
	Bitmap get(String key);

	/** Removes item by key
	 *  根据key,从缓存中删除图片
	 */
	Bitmap remove(String key);

	/**
	 * Returns all keys of cache
	 * 返回缓存中所有的key
	 */
	Collection<String> keys();

	/** Remove all items from cache
	 * 清除缓存中所有的数据
	 */
	void clear();
}
