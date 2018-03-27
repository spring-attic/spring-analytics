/*
 * Copyright 2015-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.analytics.rest.controller;

import org.springframework.analytics.rest.domain.Metric;
import org.springframework.analytics.metrics.redis.RedisMetricRepository;
import org.springframework.analytics.rest.domain.CounterResource;
import org.springframework.analytics.rest.domain.MetricResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.hateoas.mvc.ResourceAssemblerSupport;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.toIntExact;

/**
 * Allows interaction with Counters.
 *
 * @author Eric Bottard
 * @author Mark Fisher
 * @author Ilayaperumal Gopinathan
 */
@RestController
@RequestMapping("/metrics/counters")
@ExposesResourceFor(CounterResource.class)
public class CounterController {

	public static final String COUNTER_PREFIX = "counter.";

	private final RedisMetricRepository metricRepository;

	private final ResourceAssembler<Metric<Double>, CounterResource> counterResourceAssembler =
			new DeepCounterResourceAssembler();

	protected final ResourceAssembler<Metric<Double>, ? extends MetricResource> shallowResourceAssembler =
			new ShallowMetricResourceAssembler();

	/**
	 * Create a {@link CounterController} that delegates to the provided {@link RedisMetricRepository}.
	 *
	 * @param metricRepository the {@link RedisMetricRepository} used by this controller
	 */
	public CounterController(RedisMetricRepository metricRepository) {
		Assert.notNull(metricRepository, "metricRepository must not be null");
		this.metricRepository = metricRepository;
	}

	/**
	 * List Counters that match the given criteria.
	 *
	 * @param pageable {@link Pageable}
	 * @param pagedAssembler {@link PagedResourcesAssembler}
	 * @param detailed details
	 * @return {@link PagedResources}
	 */
	@RequestMapping(value = "", method = RequestMethod.GET)
	public PagedResources<? extends MetricResource> list(
			Pageable pageable,
			PagedResourcesAssembler<Metric<Double>> pagedAssembler,
			@RequestParam(value = "detailed", defaultValue = "false") boolean detailed) {
		/* Page */ Iterable<Metric<?>> metrics = metricRepository.findAll(/* pageable */);
		List<Metric<Double>> content = filterCounters(metrics);
		long count = content.size();
		long pageEnd = Math.min(count, pageable.getOffset() + pageable.getPageSize());
		Page counterPage = new PageImpl<>(content.subList(toIntExact(pageable.getOffset()), toIntExact(pageEnd)),
				pageable, content.size());
		ResourceAssembler<Metric<Double>, ? extends MetricResource> assemblerToUse =
				detailed ? counterResourceAssembler : shallowResourceAssembler;
		return pagedAssembler.toResource(counterPage, assemblerToUse);
	}

	/**
	 * Retrieve information about a specific counter.
	 *
	 * @param name name
	 * @return counter information
	 */
	@RequestMapping(value = "/{name}", method = RequestMethod.GET)
	public CounterResource display(@PathVariable("name") String name) {
		Metric<Double> c = findCounter(name);
		return counterResourceAssembler.toResource(c);
	}

	/**
	 * Delete (reset) a specific counter.
	 *
	 * @param name to delete
	 */
	@RequestMapping(value = "/{name}", method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.OK)
	protected void delete(@PathVariable("name") String name) {
		Metric<Double> c = findCounter(name);
		metricRepository.reset(c.getName());
	}

	/**
	 * Find a given counter, taking care of name conversion between the Spring Boot domain and our domain.
	 *
	 * @param name name
	 * @return counter
	 * @throws NoSuchMetricException if the counter does not exist
	 */
	private Metric<Double> findCounter(@PathVariable("name") String name) {
		@SuppressWarnings("unchecked")
		Metric<Double> c = (Metric<Double>) metricRepository.findOne(COUNTER_PREFIX + name);
		if (c == null) {
			throw new NoSuchMetricException(name);
		}
		return c;
	}


	/**
	 * Filter the list of Boot metrics to only return those that are counters.
	 */
	@SuppressWarnings("unchecked")
	private <T extends Number> List<Metric<T>> filterCounters(Iterable<Metric<?>> input) {
		List<Metric<T>> result = new ArrayList<>();
		for (Metric<?> metric : input) {
			if (metric.getName().startsWith(COUNTER_PREFIX)) {
				result.add((Metric<T>) metric);
			}
		}
		return result;
	}

	/**
	 * Base class for a ResourceAssembler that builds shallow resources for metrics
	 * (exposing only their names, and hence their "self" rel).
	 *
	 * @author Eric Bottard
	 */
	static class ShallowMetricResourceAssembler extends
			ResourceAssemblerSupport<Metric<Double>, MetricResource> {

		public ShallowMetricResourceAssembler() {
			super(CounterController.class, MetricResource.class);
		}

		@Override
		public MetricResource toResource(Metric<Double> entity) {
			return createResourceWithId(entity.getName().substring(COUNTER_PREFIX.length()), entity);
		}

		@Override
		protected MetricResource instantiateResource(Metric<Double> entity) {
			return new MetricResource(entity.getName().substring(COUNTER_PREFIX.length()));
		}

	}

	/**
	 * Knows how to assemble {@link CounterResource}s out of counter {@link Metric}s.
	 *
	 * @author Eric Bottard
	 */
	static class DeepCounterResourceAssembler extends
			ResourceAssemblerSupport<Metric<Double>, CounterResource> {

		public DeepCounterResourceAssembler() {
			super(CounterController.class, CounterResource.class);
		}

		@Override
		public CounterResource toResource(Metric<Double> entity) {
			return createResourceWithId(entity.getName().substring(COUNTER_PREFIX.length()), entity);
		}

		@Override
		protected CounterResource instantiateResource(Metric<Double> entity) {
			return new CounterResource(entity.getName().substring(COUNTER_PREFIX.length()), entity.getValue().longValue());
		}

	}

}
