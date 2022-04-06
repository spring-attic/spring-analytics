/*
 * Copyright 2016-2018 the original author or authors.
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

package org.springframework.analytics.rest.controller;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.analytics.rest.domain.Metric;
import org.springframework.analytics.metrics.redis.RedisMetricRepository;
import org.springframework.analytics.test.support.CounterService;
import org.springframework.analytics.test.support.DefaultCounterService;
import org.springframework.analytics.test.support.RedisTestSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.springframework.hateoas.config.EnableHypermediaSupport.HypermediaType.HAL;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for {@link CounterController}.
 *
 * @author Ilayaperumal Gopinathan
 * @author Soby Chacko
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {CounterControllerTests.Config.class})
@WebAppConfiguration
public class CounterControllerTests {

	@Rule
	public RedisTestSupport redisTestSupport = new RedisTestSupport();

	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Autowired
	private RedisMetricRepository repository;

	@Autowired
	private CounterService counterService;

	@Autowired
	private WebApplicationContext wac;

	private MockMvc mockMvc;

	@Before
	public void setupMockMVC() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(wac).defaultRequest(
				get("/").accept(MediaType.APPLICATION_JSON)).build();
	}

	@After
	public void cleanUp() {
		for (Metric counter : repository.findAll()) {
			repository.reset(counter.getName());
		}
	}

	@Test
	public void testList() throws Exception {
		counterService.increment("foo");
		counterService.increment("bar");
		mockMvc.perform(get("/metrics/counters").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.page.totalElements", is(2)))
				.andExpect(jsonPath("$.content.*.name", containsInAnyOrder("foo", "bar")));
	}

	@Test
	public void testGetAndDelete() throws Exception {
		counterService.increment("foo");
		counterService.increment("foo");
		mockMvc.perform(
				get("/metrics/counters/foo").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name", is("foo")))
				.andExpect(jsonPath("$.value", is(2)));

		mockMvc.perform(
				delete("/metrics/counters/foo").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk());

		mockMvc.perform(
				get("/metrics/counters/foo").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound());

	}

	@Configuration
	@EnableSpringDataWebSupport
	@EnableHypermediaSupport(type = HAL)
	@EnableWebMvc
	@Import(RedisAutoConfiguration.class)
	public static class Config {

		@Autowired
		RedisConnectionFactory redisConnectionFactory;

		@Bean
		public RedisMetricRepository counterRepository() {
			return new RedisMetricRepository(redisConnectionFactory);
		}

		@Bean
		public CounterController counterController(RedisMetricRepository metricRepository) {
			return new CounterController(metricRepository);
		}

		@Bean
		public CounterService counterService(RedisMetricRepository metricRepository) {
			return new DefaultCounterService(metricRepository);
		}

	}
}
