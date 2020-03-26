/*
 * Copyright 2018 Confluent Inc.
 *
 * Licensed under the Confluent Community License (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 *
 * http://www.confluent.io/confluent-community-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package io.confluent.ksql.rest.client.properties;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import io.confluent.ksql.config.PropertyParser;
import io.confluent.ksql.util.KsqlConfig;
import io.confluent.ksql.util.KsqlException;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.streams.StreamsConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class LocalPropertiesTest {

  private static final Map<String, Object> INITIAL = ImmutableMap.of(
      "prop-1", "initial-val-1",
      "prop-2", "initial-val-2"
  );

  @Mock
  private PropertyParser parser;
  private LocalProperties propsWithMockParser;
  private LocalProperties realProps;

  @Before
  public void setUp() {
    when(parser.parse(any(), any()))
        .thenAnswer(inv -> "parsed-" + inv.getArgument(1));

    propsWithMockParser = new LocalProperties(INITIAL, parser);

    realProps = new LocalProperties(ImmutableMap.of());
  }

  @Test
  public void shouldValidateInitialPropsByParsing() {
    assertThat(propsWithMockParser.toMap().get("prop-1"), is("parsed-initial-val-1"));
    assertThat(propsWithMockParser.toMap().get("prop-2"), is("parsed-initial-val-2"));
  }

  @Test
  public void shouldThrowInInitialPropsInvalid() {
    // Given:
    final Map<String, Object> invalid = ImmutableMap.of(
        "this.is.not.valid", "value"
    );

    // When:
    final KsqlException e = assertThrows(
        KsqlException.class,
        () -> new LocalProperties(invalid)
    );

    // Then:
    assertThat(e.getMessage(), containsString("invalid property found"));
    assertThat(e.getMessage(), containsString("'this.is.not.valid'"));
  }

  @Test
  public void shouldUnsetInitialValue() {
    // When:
    final Object oldValue = propsWithMockParser.unset("prop-1");

    // Then:
    assertThat(oldValue, is("parsed-initial-val-1"));
    assertThat(propsWithMockParser.toMap().get("prop-1"), is(nullValue()));
    assertThat(propsWithMockParser.toMap().get("prop-2"), is("parsed-initial-val-2"));
  }

  @Test
  public void shouldOverrideInitialValue() {
    // When:
    final Object oldValue = propsWithMockParser.set("prop-1", "new-val");

    // Then:
    assertThat(oldValue, is("parsed-initial-val-1"));
    assertThat(propsWithMockParser.toMap().get("prop-1"), is("parsed-new-val"));
    assertThat(propsWithMockParser.toMap().get("prop-2"), is("parsed-initial-val-2"));
  }

  @Test
  public void shouldUnsetOverriddenValue() {
    // Given:
    propsWithMockParser.set("prop-1", "new-val");

    // When:
    final Object oldValue = propsWithMockParser.unset("prop-1");

    // Then:
    assertThat(oldValue, is("parsed-new-val"));
    assertThat(propsWithMockParser.toMap().get("prop-1"), is(nullValue()));
    assertThat(propsWithMockParser.toMap().get("prop-2"), is("parsed-initial-val-2"));
  }

  @Test
  public void shouldReturnOnlyKnownKeys() {
    assertThat(propsWithMockParser.toMap().keySet(), containsInAnyOrder("prop-1", "prop-2"));
  }

  @Test
  public void shouldSetNewValue() {
    // When:
    final Object oldValue = propsWithMockParser.set("new-prop", "new-val");

    // Then:
    assertThat(oldValue, is(nullValue()));
    assertThat(propsWithMockParser.toMap().get("new-prop"), is("parsed-new-val"));
    assertThat(propsWithMockParser.toMap().get("prop-2"), is("parsed-initial-val-2"));
  }

  @Test
  public void shouldUnsetNewValue() {
    // Given:
    propsWithMockParser.set("new-prop", "new-val");

    // When:
    final Object oldValue = propsWithMockParser.unset("new-prop");

    // Then:
    assertThat(oldValue, is("parsed-new-val"));
    assertThat(propsWithMockParser.toMap().get("new-prop"), is(nullValue()));
    assertThat(propsWithMockParser.toMap().get("prop-2"), is("parsed-initial-val-2"));
  }

  @Test
  public void shouldInvokeParserCorrectly() {
    // Given:
    when(parser.parse("prop-1", "new-val")).thenReturn("parsed-new-val");

    // When:
    propsWithMockParser.set("prop-1", "new-val");

    // Then:
    assertThat(propsWithMockParser.toMap().get("prop-1"), is("parsed-new-val"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldThrowIfParserThrows() {
    // Given:
    when(parser.parse("prop-1", "new-val"))
        .thenThrow(new IllegalArgumentException("Boom"));

    // When:
    propsWithMockParser.set("prop-1", "new-val");
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldNotAllowUnknownPropertyToBeSet() {
    realProps.set("some.unknown.prop", "some.value");
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldNotAllowUnknownConsumerPropertyToBeSet() {
    realProps.set(StreamsConfig.CONSUMER_PREFIX + "some.unknown.prop", "some.value");
  }

  @Test
  public void shouldAllowKnownConsumerPropertyToBeSet() {
    realProps.set(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, "100");
  }

  @Test
  public void shouldAllowKnownPrefixedConsumerPropertyToBeSet() {
    realProps.set(StreamsConfig.CONSUMER_PREFIX + ConsumerConfig.FETCH_MIN_BYTES_CONFIG, "100");
  }

  @Test
  public void shouldAllowKnownKsqlPrefixedConsumerPropertyToBeSet() {
    realProps.set(KsqlConfig.KSQL_STREAMS_PREFIX + StreamsConfig.CONSUMER_PREFIX
        + ConsumerConfig.FETCH_MIN_BYTES_CONFIG, "100");
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldNotAllowUnknownProducerPropertyToBeSet() {
    realProps.set(StreamsConfig.PRODUCER_PREFIX + "some.unknown.prop", "some.value");
  }

  @Test
  public void shouldAllowKnownProducerPropertyToBeSet() {
    realProps.set(ProducerConfig.BUFFER_MEMORY_CONFIG, "100");
  }

  @Test
  public void shouldAllowKnownKsqlPrefixedProducerPropertyToBeSet() {
    realProps.set(KsqlConfig.KSQL_STREAMS_PREFIX + StreamsConfig.PRODUCER_PREFIX
        + ProducerConfig.BUFFER_MEMORY_CONFIG, "100");
  }

  @Test
  public void shouldAllowKnownPrefixedProducerPropertyToBeSet() {
    realProps.set(StreamsConfig.PRODUCER_PREFIX + ProducerConfig.BUFFER_MEMORY_CONFIG, "100");
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldNotAllowUnknownStreamsConfigToBeSet() {
    realProps.set(KsqlConfig.KSQL_STREAMS_PREFIX + "some.unknown.prop", "some.value");
  }

  @Test
  public void shouldAllowKnownStreamsConfigToBeSet() {
    realProps.set(StreamsConfig.NUM_STREAM_THREADS_CONFIG, "2");
  }

  @Test
  public void shouldAllowKnownPrefixedStreamsConfigToBeSet() {
    realProps.set(KsqlConfig.KSQL_STREAMS_PREFIX + StreamsConfig.NUM_STREAM_THREADS_CONFIG, "2");
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldNotAllowUnknownKsqlConfigToBeSet() {
    realProps.set(KsqlConfig.KSQL_CONFIG_PROPERTY_PREFIX + "some.unknown.prop", "some.value");
  }

  @Test
  public void shouldAllowKnownUdfConfigToBeSet() {
    realProps.set(KsqlConfig.KSQL_FUNCTIONS_SUBSTRING_LEGACY_ARGS_CONFIG, "true");
  }

  @Test
  public void shouldAllowUnknownUdfConfigToBeSet() {
    realProps.set(KsqlConfig.KSQL_FUNCTIONS_PROPERTY_PREFIX + "some_udf.some.prop", "some thing");
  }
}