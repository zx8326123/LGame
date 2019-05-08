/**
 * Copyright 2008 - 2015 The Loon Game Engine Authors
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * 
 * @project loon
 * @author cping
 * @email：javachenpeng@yahoo.com
 * @version 0.5
 */
package loon;

/**
 * 一个ID计数器
 */
public class IDGenerator {

	private static IDGenerator instance;

	private final Counter _counter = new Counter();
	
	public final static IDGenerator make() {
		return new IDGenerator();
	}

	public final static IDGenerator get() {
		if (instance != null) {
			return instance;
		}
		synchronized (IDGenerator.class) {
			if (instance == null) {
				instance = make();
			}
			return instance;
		}
	}

	private IDGenerator() {
	}

	public final int generate() {
		return _counter.increment();
	}

	public final int getID() {
		return _counter.getValue();
	}

	public final void clear() {
		_counter.clear();
	}
}