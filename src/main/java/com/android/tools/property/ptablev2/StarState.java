/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.tools.property.ptablev2;

/**
 * Indicates whether an item in a table is starred, can be starred, or cannot be starred.
 */
public enum StarState {
  STARRED, STAR_ABLE, NOT_STAR_ABLE;

  public StarState opposite() {
    switch (this) {
      case STARRED:
        return STAR_ABLE;
      case STAR_ABLE:
        return STARRED;
      default:
        return NOT_STAR_ABLE;
    }
  }
}
