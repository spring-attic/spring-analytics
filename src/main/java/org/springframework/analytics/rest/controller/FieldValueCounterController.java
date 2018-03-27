/*
 * Copyright 2015-2018 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;

import org.springframework.analytics.metrics.FieldValueCounter;
import org.springframework.analytics.metrics.FieldValueCounterRepository;
import org.springframework.analytics.rest.domain.FieldValueCounterResource;
import org.springframework.analytics.rest.domain.MetricResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.mvc.ResourceAssemblerSupport;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static java.lang.Math.toIntExact;

/**
 * Allows interaction with Field Value Counters.
 *
 * @author Eric Bottard
 * @author Ilayaperumal Gopinathan
 */
@RestController
@RequestMapping("/metrics/field-value-counters")
@ExposesResourceFor(FieldValueCounterResource.class)
public class FieldValueCounterController {

	private final FieldValueCounterRepository repository;

	public FieldValueCounterController(FieldValueCounterRepository repository) {
		this.repository = repository;
	}

	private DeepResourceAssembler deepAssembler = new DeepResourceAssembler();

	private ShallowResourceAssembler shallowAssembler = new ShallowResourceAssembler();

	/**
	 * Retrieve information about a specific counter.
	 *
	 * @param name name
	 * @return counter information
	 */
	@RequestMapping(value = "/{name}", method = RequestMethod.GET)
	public FieldValueCounterResource display(@PathVariable("name") String name) {
		FieldValueCounter counter = repository.findOne(name);
		if (counter == null) {
			throw new NoSuchMetricException(name);
		}
		return deepAssembler.toResource(counter);
	}

	/**
	 * Delete (reset) a specific counter.
	 *
	 * @param name name
	 */
	@RequestMapping(value = "/{name}", method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.OK)
	protected void delete(@PathVariable("name") String name) {
		FieldValueCounter counter = repository.findOne(name);
		if (counter == null) {
			throw new NoSuchMetricException(name);
		}
		repository.reset(name);
	}

	/**
	 * List Counters that match the given criteria.
	 *
	 * @param pageable {@link Pageable}
	 * @param pagedAssembler {@link PagedResourcesAssembler}
	 * @return counters
	 */
	@RequestMapping(value = "", method = RequestMethod.GET)
	public PagedResources<? extends MetricResource> list(Pageable pageable,
			PagedResourcesAssembler<String> pagedAssembler) {
		List<String> names = new ArrayList<>(repository.list());
		long count = names.size();
		long pageEnd = Math.min(count, pageable.getOffset() + pageable.getPageSize());
		Page fieldValueCounterPage = new PageImpl<>(names.subList(toIntExact(pageable.getOffset()), toIntExact(pageEnd)), pageable, names.size());
		return pagedAssembler.toResource(fieldValueCounterPage, shallowAssembler);
	}

	/**
	 * Knows how to assemble {@link MetricResource} out of simple String names
	 */
	private static class ShallowResourceAssembler extends
			ResourceAssemblerSupport<String, MetricResource> {

		private ShallowResourceAssembler() {
			super(FieldValueCounterController.class, MetricResource.class);
		}

		@Override
		public MetricResource toResource(String name) {
			return createResourceWithId(name, name);
		}

		@Override
		protected MetricResource instantiateResource(String name) {
			return new MetricResource(name);
		}
	}

	/**
	 * Knows how to assemble {@link FieldValueCounterResource} out of {@link FieldValueCounter}.
	 *
	 * @author Eric Bottard
	 */
	private static class DeepResourceAssembler extends
			ResourceAssemblerSupport<FieldValueCounter, FieldValueCounterResource> {

		private DeepResourceAssembler() {
			super(FieldValueCounterController.class, FieldValueCounterResource.class);
		}

		@Override
		public FieldValueCounterResource toResource(FieldValueCounter entity) {
			return createResourceWithId(entity.getName(), entity);
		}

		@Override
		protected FieldValueCounterResource instantiateResource(FieldValueCounter entity) {
			return new FieldValueCounterResource(entity.getName(), entity.getFieldValueCounts());
		}

	}
}
