/*
 * Copyright 2015-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.analytics.rest.domain;

/**
 * The REST representation of a Counter.
 *
 * @author Eric Bottard
 */
public class CounterResource extends MetricResource {

	/**
	 * The value for the counter.
	 */
	private long value;


	/**
	 * No-arg constructor for serialization frameworks.
	 */
	protected CounterResource() {

	}

	public CounterResource(String name, long value) {
		super(name);
		this.value = value;
	}

	/**
	 * Return the value for the counter.
	 *
	 * @return counter value
	 */
	public long getValue() {
		return value;
	}

}
