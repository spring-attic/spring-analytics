/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.analytics.test.support;

import org.springframework.analytics.metrics.redis.RedisMetricRepository;
import org.springframework.analytics.rest.domain.Delta;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of {@link CounterService}.
 *
 * @author Dave Syer
 */
public class DefaultCounterService implements CounterService {

	private final RedisMetricRepository writer;

	private final ConcurrentHashMap<String, String> names = new ConcurrentHashMap<String, String>();

	/**
	 * Create a {@link DefaultCounterService} instance.
	 * @param writer the underlying writer used to manage metrics
	 */
	public DefaultCounterService(RedisMetricRepository writer) {
		this.writer = writer;
	}

	@Override
	public void increment(String metricName) {
		this.writer.increment(new Delta<Long>(wrap(metricName), 1L));
	}

	@Override
	public void decrement(String metricName) {
		this.writer.increment(new Delta<Long>(wrap(metricName), -1L));
	}

	@Override
	public void reset(String metricName) {
		this.writer.reset(wrap(metricName));
	}

	private String wrap(String metricName) {
		String cached = this.names.get(metricName);
		if (cached != null) {
			return cached;
		}
		if (metricName.startsWith("counter.") || metricName.startsWith("meter.")) {
			return metricName;
		}
		String name = "counter." + metricName;
		this.names.put(metricName, name);
		return name;
	}

}
