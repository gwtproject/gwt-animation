/*
 * Copyright © 2020 The GWT Project Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gwtproject.animation.client;

import com.google.gwt.junit.DoNotRunWith;
import com.google.gwt.junit.Platform;
import com.google.gwt.junit.client.GWTTestCase;
import org.gwtproject.animation.client.AnimationScheduler.AnimationHandle;
import org.gwtproject.core.client.Duration;
import org.gwtproject.dom.client.DivElement;
import org.gwtproject.dom.client.Document;
import org.gwtproject.timer.client.Timer;

/** Tests the {@link AnimationScheduler} class. */
public class AnimationGwt2SchedulerTest extends GWTTestCase {

  /** The default timeout of asynchronous tests. */
  private static final int TEST_TIMEOUT = 60000;

  /**
   * Test maximum expected delay before the scheduler calls the callback. If the browser tab does
   * not have focus, the browser may dramatically reduce the rate that timers fire, down to 1000ms.
   */
  private static final int TIMER_DELAY = 3000;

  private AnimationScheduler scheduler;

  @Override
  public String getModuleName() {
    return "org.gwtproject.animation.AnimationTest";
  }

  @Override
  protected void gwtSetUp() throws Exception {
    scheduler = AnimationScheduler.get();
  }

  @Override
  protected void gwtTearDown() throws Exception {
    scheduler = null;
  }

  public void testCancel() {
    delayTestFinish(TEST_TIMEOUT);
    AnimationHandle handle =
        scheduler.requestAnimationFrame(
            timestamp -> fail("The animation frame was cancelled and should not execute."), null);
    // Cancel the animation frame.
    handle.cancel();
    // Wait to make sure it doesn't execute.
    new Timer() {
      @Override
      public void run() {
        finishTest();
      }
    }.schedule(TIMER_DELAY);
  }

  // TODO(davido): doesn't work on htmlunit-2.19 (works in 2.18)
  // Presumably because of: http://sourceforge.net/p/htmlunit/code/11004
  @DoNotRunWith(Platform.HtmlUnitBug)
  public void testRequestAnimationFrame() {
    delayTestFinish(TEST_TIMEOUT);
    final double startTime = Duration.currentTimeMillis();
    DivElement element = Document.get().createDivElement();
    scheduler.requestAnimationFrame(
        timestamp -> {
          // Make sure timestamp is not a high-res timestamp (see issue 8570)
          assertTrue(timestamp >= startTime);
          finishTest();
        },
        element);
  }

  // The same as above
  @DoNotRunWith(Platform.HtmlUnitBug)
  public void testRequestAnimationFrameWithoutElement() {
    delayTestFinish(TEST_TIMEOUT);
    final double startTime = Duration.currentTimeMillis();
    scheduler.requestAnimationFrame(
        timestamp -> {
          // Make sure timestamp is not a high-res timestamp (see issue 8570)
          assertTrue(timestamp >= startTime);
          finishTest();
        },
        null);
  }
}
