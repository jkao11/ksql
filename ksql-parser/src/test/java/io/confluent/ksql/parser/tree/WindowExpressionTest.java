/*
 * Copyright 2019 Confluent Inc.
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

package io.confluent.ksql.parser.tree;

import static org.mockito.Mockito.mock;

import com.google.common.testing.EqualsTester;
import io.confluent.ksql.execution.windows.KsqlWindowExpression;
import io.confluent.ksql.parser.NodeLocation;
import java.util.Optional;
import org.junit.Test;

public class WindowExpressionTest {

  public static final NodeLocation SOME_LOCATION = new NodeLocation(0, 0);
  public static final NodeLocation OTHER_LOCATION = new NodeLocation(1, 0);
  private static final String SOME_NAME = "bob";
  private static final KsqlWindowExpression SOME_WINDOW = mock(KsqlWindowExpression.class);

  @Test
  public void shouldImplementHashCodeAndEqualsProperty() {
    new EqualsTester()
        .addEqualityGroup(
            // Note: At the moment location does not take part in equality testing
            new WindowExpression(SOME_NAME, SOME_WINDOW),
            new WindowExpression(SOME_NAME, SOME_WINDOW),
            new WindowExpression(Optional.of(SOME_LOCATION), SOME_NAME, SOME_WINDOW),
            new WindowExpression(Optional.of(OTHER_LOCATION), SOME_NAME, SOME_WINDOW)
        )
        .addEqualityGroup(
            new WindowExpression("diff", SOME_WINDOW)
        )
        .addEqualityGroup(
            new WindowExpression(SOME_NAME, mock(KsqlWindowExpression.class))
        )
        .testEquals();
  }
}