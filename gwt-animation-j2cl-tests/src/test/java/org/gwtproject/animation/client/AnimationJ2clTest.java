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

import static junit.framework.TestCase.*;

import com.google.j2cl.junit.apt.J2clTestInput;
import java.util.List;
import org.gwtproject.animation.client.AnimationScheduler.AnimationCallback;
import org.gwtproject.animation.client.testing.StubAnimationScheduler;
import org.gwtproject.core.client.Duration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the {@link Animation} class.
 *
 * <p>This class uses the {@link StubAnimationScheduler} to manually trigger callbacks.
 */
@J2clTestInput(AnimationJ2clTest.class)
public class AnimationJ2clTest {

  /**
   * The maximum delay before an animation will run. Animations may run slowly if the browser tab is
   * not focused.
   *
   * <p>Increase this multiplier to increase the duration of the tests, reducing the potential of an
   * error caused by timing issues.
   */
  private static int DELAY_MULTIPLIER = 3000;

  private List<AnimationCallback> callbacks;
  private double curTime;
  private StubAnimationScheduler scheduler;

  @Before
  public void setUp() {
    scheduler = new StubAnimationScheduler();
    callbacks = scheduler.getAnimationCallbacks();
    curTime = Duration.currentTimeMillis();
  }

  @After
  public void teardown() {
    scheduler = null;
    callbacks = null;
  }

  /** Test canceling an {@link Animation} after it completes. */
  @Test
  public void testCancelAfterOnComplete() {
    final TestAnimation anim = new TestAnimation();
    assertFalse(anim.isRunning());
    anim.run(DELAY_MULTIPLIER);
    assertTrue(anim.isRunning());
    anim.assertStarted(true);
    anim.assertUpdated(false);
    anim.assertCompleted(false);
    anim.assertCancelled(false);
    anim.reset();

    // Complete the animation.
    // TODO(cromwellian) remove this 4000 hack after the failure is found
    executeLastCallbackAt(curTime + DELAY_MULTIPLIER + 4000);
    assertFalse(anim.isRunning());
    anim.assertStarted(false);
    anim.assertUpdated(false);
    anim.assertCompleted(true);
    anim.assertCancelled(false);
    assertEquals(0, callbacks.size());
    anim.reset();

    // Cancel the animation.
    anim.cancel(); // no-op.
    assertFalse(anim.isRunning());
    anim.assertStarted(false);
    anim.assertUpdated(false);
    anim.assertProgress(-1);
    anim.assertCancelled(false);
  }

  /**
   * Execute the last callback requested from the scheduler at the specified time.
   *
   * @param timestamp the time to pass to the callback
   */
  private void executeLastCallbackAt(double timestamp) {
    assertTrue(callbacks.size() > 0);
    AnimationCallback callback = callbacks.remove(callbacks.size() - 1);
    callback.execute(timestamp);
  }

  /** Test canceling an {@link Animation} before onStart is called. */
  @Test
  public void testCancelBeforeOnStart() {
    final TestAnimation anim = new TestAnimation();
    assertFalse(anim.isRunning());

    // Run the animation in the future.
    anim.run(DELAY_MULTIPLIER, curTime + 1000);
    assertTrue(anim.isRunning());
    anim.assertStarted(false);
    anim.assertUpdated(false);
    anim.assertCompleted(false);
    anim.assertCancelled(false);
    assertEquals(1, callbacks.size());
    anim.reset();

    // Cancel the animation before it starts.
    anim.cancel();
    assertFalse(anim.isRunning());
    anim.assertStarted(false);
    anim.assertUpdated(false);
    anim.assertCompleted(false);
    anim.assertCancelled(true);
    assertEquals(0, callbacks.size());
  }

  /** Test canceling an {@link Animation} between updates. */
  @Test
  public void testCancelBetweenUpdates() {
    TestAnimation anim = new TestAnimation();
    assertFalse(anim.isRunning());
    anim.run(10 * DELAY_MULTIPLIER);
    assertTrue(anim.isRunning());
    anim.assertStarted(true);
    anim.assertUpdated(false);
    anim.assertCompleted(false);
    anim.assertCancelled(false);
    anim.reset();

    // Update the animation.
    executeLastCallbackAt(curTime + DELAY_MULTIPLIER);
    assertTrue(anim.isRunning());
    anim.assertStarted(false);
    anim.assertUpdated(true);
    anim.assertCompleted(false);
    anim.assertCancelled(false);
    anim.reset();

    // Cancel the animation.
    assertEquals(1, callbacks.size());
    anim.cancel();
    assertFalse(anim.isRunning());
    anim.assertStarted(false);
    anim.assertUpdated(false);
    anim.assertCompleted(false);
    anim.assertCancelled(true);
    anim.assertProgress(-1.0);
    assertEquals(0, callbacks.size());
  }

  /** Test canceling an {@link Animation} within onComplete. */
  @Test
  public void testCancelDuringOnComplete() {
    final TestAnimation anim =
        new TestAnimation() {
          @Override
          protected void onComplete() {
            super.onComplete();
            assertStarted(false);
            assertUpdated(false);
            assertCompleted(true);
            assertCancelled(false);
            reset();

            // Cancel the animation.
            cancel(); // no-op.
          }
        };

    assertFalse(anim.isRunning());

    // Run the animation.
    anim.run(DELAY_MULTIPLIER);
    assertTrue(anim.isRunning());
    anim.assertStarted(true);
    anim.assertUpdated(false);
    anim.assertCompleted(false);
    anim.assertCancelled(false);
    anim.reset();

    // Force the animation to complete.
    executeLastCallbackAt(curTime + DELAY_MULTIPLIER + 100);
    anim.assertStarted(false);
    anim.assertUpdated(false);
    anim.assertCompleted(false);
    anim.assertCancelled(false);
    assertEquals(0, callbacks.size());
    assertFalse(anim.isRunning());
  }

  /** Test canceling an {@link Animation} within onStart. */
  @Test
  public void testCancelDuringOnStart() {
    final TestAnimation anim =
        new TestAnimation() {
          @Override
          protected void onStart() {
            super.onStart();
            assertStarted(true);
            assertUpdated(false);
            assertCompleted(false);
            assertCancelled(false);
            reset();

            // Cancel the animation.
            cancel();
          }
        };

    assertFalse(anim.isRunning());

    // Run the animation.
    anim.run(DELAY_MULTIPLIER);
    assertFalse(anim.isRunning());
    anim.assertStarted(false);
    anim.assertUpdated(false);
    anim.assertCancelled(true);
    anim.assertCompleted(false);
    assertEquals(0, callbacks.size());
  }

  /** Test canceling an {@link Animation} during an update. */
  @Test
  public void testCancelDuringOnUpdate() {
    final TestAnimation anim =
        new TestAnimation() {
          @Override
          protected void onUpdate(double progress) {
            super.onUpdate(progress);
            assertStarted(false);
            assertUpdated(true);
            assertCompleted(false);
            assertCancelled(false);
            reset();

            // Cancel the test while it is running.
            cancel();
          }
        };

    assertFalse(anim.isRunning());

    // Run the animation.
    anim.run(10 * DELAY_MULTIPLIER);
    assertTrue(anim.isRunning());
    anim.assertStarted(true);
    anim.assertUpdated(false);
    anim.assertCompleted(false);
    anim.assertCancelled(false);
    anim.assertProgress(-1.0);
    anim.reset();

    // Force the update.
    executeLastCallbackAt(curTime + DELAY_MULTIPLIER);
    anim.assertStarted(false);
    anim.assertUpdated(false);
    anim.assertCompleted(false);
    anim.assertCancelled(true);
    anim.assertProgress(-1.0);
    assertEquals(0, callbacks.size());
  }

  /** Test the default implementations of events in {@link Animation}. */
  @Test
  public void testDefaultAnimation() {
    // Verify initial state
    final DefaultAnimation anim = new DefaultAnimation();
    assertFalse(anim.isRunning());
    anim.assertStarted(false);
    anim.assertUpdated(false);
    anim.assertCancelled(false);
    anim.assertCompleted(false);

    // Starting an animation calls onUpdate(interpolate(0.0))
    anim.reset();
    anim.onStart();
    anim.assertProgress(0.0);
    anim.assertStarted(true);
    anim.assertCompleted(false);
    anim.assertCancelled(false);

    // Completing an animation calls onUpdate(interpolate(1.0))
    anim.reset();
    anim.onComplete();
    anim.assertProgress(1.0);
    anim.assertStarted(false);
    anim.assertCompleted(true);
    anim.assertCancelled(false);

    // Canceling an animation that is not running does not call onStart or
    // onComplete
    anim.reset();
    anim.onCancel();
    anim.assertProgress(-1.0);
    anim.assertStarted(false);
    anim.assertCompleted(false);
    anim.assertCancelled(true);

    // Canceling an animation before it starts does not call onStart or
    // onComplete
    anim.reset();
    anim.run(10 * DELAY_MULTIPLIER, curTime + DELAY_MULTIPLIER);
    assertTrue(anim.isRunning());
    anim.cancel();
    assertFalse(anim.isRunning());
    anim.assertProgress(-1.0);
    anim.assertStarted(false);
    anim.assertCompleted(false);
    anim.assertCancelled(true);
  }

  /** Test that restarting an {@link Animation} within onComplete does not break. See issue 5639. */
  @Test
  public void testRunDuringOnComplete() {
    final TestAnimation anim =
        new TestAnimation() {
          @Override
          protected void onComplete() {
            super.onComplete();
            assertStarted(false);
            assertUpdated(false);
            assertCompleted(true);
            assertCancelled(false);
            reset();

            // Run the animation.
            run(DELAY_MULTIPLIER);
          }
        };

    assertFalse(anim.isRunning());

    // Run the animation.
    anim.run(DELAY_MULTIPLIER);
    assertTrue(anim.isRunning());
    anim.assertStarted(true);
    anim.assertUpdated(false);
    anim.assertCompleted(false);
    anim.assertCancelled(false);
    anim.reset();

    // Force the animation to complete.
    executeLastCallbackAt(curTime + DELAY_MULTIPLIER + 100);
    anim.assertStarted(true);
    anim.assertUpdated(false);
    anim.assertCompleted(false);
    anim.assertCancelled(false);
    assertEquals(1, callbacks.size());
  }

  /** Test that an animation runs in the future. */
  @Test
  public void testRunFuture() {
    final TestAnimation anim = new TestAnimation();
    assertFalse(anim.isRunning());
    anim.run(2 * DELAY_MULTIPLIER, curTime + 2 * DELAY_MULTIPLIER);
    assertTrue(anim.isRunning());
    anim.assertStarted(false);
    anim.assertUpdated(false);
    anim.assertCompleted(false);
    anim.reset();

    // Update, but still before the start time.
    executeLastCallbackAt(curTime + DELAY_MULTIPLIER);
    anim.assertStarted(false);
    anim.assertUpdated(false);
    anim.assertCompleted(false);
    anim.reset();

    // Start the animation.
    executeLastCallbackAt(curTime + 2 * DELAY_MULTIPLIER);
    anim.assertStarted(true);
    anim.assertUpdated(false);
    anim.assertCompleted(false);
    anim.reset();

    // Update the animation.
    executeLastCallbackAt(curTime + 3 * DELAY_MULTIPLIER);
    anim.assertStarted(false);
    anim.assertUpdated(true);
    anim.assertCompleted(false);
    anim.reset();

    // Complete the animation.
    executeLastCallbackAt(curTime + 4 * DELAY_MULTIPLIER + 100);
    anim.assertStarted(false);
    anim.assertUpdated(false);
    anim.assertCompleted(true);
    assertFalse(anim.isRunning());
  }

  /** Test that an animation runs synchronously if its duration is 0. */
  @Test
  public void testRunNow() {
    final TestAnimation anim = new TestAnimation();
    assertFalse(anim.isRunning());
    anim.run(2 * DELAY_MULTIPLIER);
    assertTrue(anim.isRunning());
    anim.assertStarted(true);
    anim.assertUpdated(false);
    anim.assertCompleted(false);
    anim.reset();

    // Update the progress.
    executeLastCallbackAt(curTime + DELAY_MULTIPLIER);
    anim.assertStarted(false);
    anim.assertUpdated(true);
    anim.assertCompleted(false);
    anim.reset();

    // Complete the animation.
    executeLastCallbackAt(curTime + 2 * DELAY_MULTIPLIER + 100);
    anim.assertStarted(false);
    anim.assertUpdated(false);
    anim.assertCompleted(true);
    assertFalse(anim.isRunning());
  }

  /** Test running an animation that started in the past. */
  @Test
  public void testRunPast() {
    final TestAnimation anim = new TestAnimation();
    assertFalse(anim.isRunning());
    anim.run(3 * DELAY_MULTIPLIER, curTime - DELAY_MULTIPLIER);
    assertTrue(anim.isRunning());
    anim.assertStarted(true);
    anim.assertUpdated(false);
    anim.assertCompleted(false);
    anim.reset();

    // Update the progress.
    executeLastCallbackAt(curTime + DELAY_MULTIPLIER);
    anim.assertStarted(false);
    anim.assertUpdated(true);
    anim.assertCompleted(false);
    anim.reset();

    // Complete the animation.
    executeLastCallbackAt(curTime + 2 * DELAY_MULTIPLIER + 100);
    anim.assertStarted(false);
    anim.assertUpdated(false);
    anim.assertCompleted(true);
    assertFalse(anim.isRunning());
  }

  /** Test running an animation that started and finished in the past. */
  @Test
  public void testRunPaster() {
    final TestAnimation anim = new TestAnimation();
    assertFalse(anim.isRunning());
    anim.run(DELAY_MULTIPLIER, curTime - 2 * DELAY_MULTIPLIER);
    anim.assertStarted(true);
    anim.assertUpdated(false);
    anim.assertCompleted(true);
    assertFalse(anim.isRunning());
  }

  /** A default implementation of {@link Animation} used for testing. */
  private class DefaultAnimation extends Animation {

    protected boolean canceled = false;
    protected boolean completed = false;
    protected double curProgress = -1.0;
    protected boolean started = false;
    protected boolean updated = false;

    public DefaultAnimation() {
      super(scheduler);
    }

    /** Assert the value of canceled. */
    public void assertCancelled(boolean expected) {
      assertEquals(expected, canceled);
    }

    /** Assert the value of completed. */
    public void assertCompleted(boolean expected) {
      assertEquals(expected, completed);
    }

    /** Assert that the progress equals the specified value. */
    public void assertProgress(double expected) {
      assertEquals(expected, curProgress);
    }

    /** Assert the value of started. */
    public void assertStarted(boolean expected) {
      assertEquals(expected, started);
    }

    /** Assert the value of updated. */
    public void assertUpdated(boolean expected) {
      assertEquals(expected, updated);
    }

    public void reset() {
      canceled = false;
      completed = false;
      updated = false;
      started = false;
      curProgress = -1.0;
    }

    @Override
    protected void onCancel() {
      super.onCancel();
      canceled = true;
    }

    @Override
    protected void onComplete() {
      super.onComplete();
      completed = true;
    }

    @Override
    protected void onUpdate(double progress) {
      updated = true;
      curProgress = progress;
    }

    @Override
    protected void onStart() {
      super.onStart();
      started = true;
    }
  }

  /** A custom {@link Animation} used for testing. */
  private class TestAnimation extends DefaultAnimation {

    @Override
    protected void onCancel() {
      canceled = true;
    }

    @Override
    protected void onComplete() {
      completed = true;
    }

    @Override
    protected void onStart() {
      started = true;
    }
  }
}
