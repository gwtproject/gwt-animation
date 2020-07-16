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
package org.gwtproject.animation.client.testing;

import java.util.ArrayList;
import java.util.List;
import org.gwtproject.animation.client.AnimationScheduler;
import org.gwtproject.dom.client.Element;

/**
 * A stub implementation of {@link AnimationScheduler} that does not execute the callbacks. Use
 * {@link StubAnimationScheduler#getAnimationCallbacks()} to retrieve and execute callbacks
 * manually.
 */
public class StubAnimationScheduler extends AnimationScheduler {

  private final List<AnimationCallback> callbacks = new ArrayList<>();

  /**
   * Get the list of all animation callbacks that have been requested and have not been canceled.
   *
   * @return the list of callbacks.
   */
  public List<AnimationCallback> getAnimationCallbacks() {
    return callbacks;
  }

  @Override
  public StubAnimationHandle requestAnimationFrame(AnimationCallback callback, Element element) {
    callbacks.add(callback);
    return new StubAnimationHandle(callback);
  }

  /**
   * A handle to the requested animation frame created by {@link
   * #requestAnimationFrame(AnimationCallback, Element)}.
   */
  public class StubAnimationHandle extends AnimationHandle {

    private final AnimationCallback callback;

    public StubAnimationHandle(AnimationCallback callback) {
      this.callback = callback;
    }

    @Override
    public void cancel() {
      callbacks.remove(callback);
    }
  }
}
