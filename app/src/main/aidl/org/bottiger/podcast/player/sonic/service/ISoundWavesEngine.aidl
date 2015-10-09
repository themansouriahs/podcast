// Copyright 2011, Aocate, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.bottiger.podcast.player.sonic.service;

import org.bottiger.podcast.player.sonic.service.IDeathCallback;
import org.bottiger.podcast.player.sonic.service.IOnBufferingUpdateListenerCallback;
import org.bottiger.podcast.player.sonic.service.IOnCompletionListenerCallback;
import org.bottiger.podcast.player.sonic.service.IOnErrorListenerCallback;
import org.bottiger.podcast.player.sonic.service.IOnPitchAdjustmentAvailableChangedListenerCallback;
import org.bottiger.podcast.player.sonic.service.IOnPreparedListenerCallback;
import org.bottiger.podcast.player.sonic.service.IOnSeekCompleteListenerCallback;
import org.bottiger.podcast.player.sonic.service.IOnSpeedAdjustmentAvailableChangedListenerCallback;
import org.bottiger.podcast.player.sonic.service.IOnInfoListenerCallback;

interface ISoundWavesEngine {
	boolean canSetPitch(long sessionId);
	boolean canSetSpeed(long sessionId);
	float getCurrentPitchStepsAdjustment(long sessionId);
	int getCurrentPosition(long sessionId);
	float getCurrentSpeedMultiplier(long sessionId);
	int getDuration(long sessionId);
	float getMaxSpeedMultiplier(long sessionId);
	float getMinSpeedMultiplier(long sessionId);
	int getVersionCode();
	String getVersionName();
	boolean isLooping(long sessionId);
	boolean isPlaying(long sessionId);
	void pause(long sessionId);
	void prepare(long sessionId);
	void prepareAsync(long sessionId);
	void registerOnBufferingUpdateCallback(long sessionId, IOnBufferingUpdateListenerCallback cb);
	void registerOnCompletionCallback(long sessionId, IOnCompletionListenerCallback cb);
	void registerOnErrorCallback(long sessionId, IOnErrorListenerCallback cb);
	void registerOnInfoCallback(long sessionId, IOnInfoListenerCallback cb);
	void registerOnPitchAdjustmentAvailableChangedCallback(long sessionId, IOnPitchAdjustmentAvailableChangedListenerCallback cb);
	void registerOnPreparedCallback(long sessionId, IOnPreparedListenerCallback cb);
	void registerOnSeekCompleteCallback(long sessionId, IOnSeekCompleteListenerCallback cb);
	void registerOnSpeedAdjustmentAvailableChangedCallback(long sessionId, IOnSpeedAdjustmentAvailableChangedListenerCallback cb);
	void release(long sessionId);
	void reset(long sessionId);
	void seekTo(long sessionId, int msec);
	void setAudioStreamType(long sessionId, int streamtype);
	void setDataSourceString(long sessionId, String path);
	void setDataSourceUri(long sessionId, in Uri uri);
	void setEnableSpeedAdjustment(long sessionId, boolean enableSpeedAdjustment);
	void setLooping(long sessionId, boolean looping);
	void setPitchStepsAdjustment(long sessionId, float pitchSteps);
	void setPlaybackPitch(long sessionId, float f);
	void setPlaybackSpeed(long sessionId, float f);
	void setSpeedAdjustmentAlgorithm(long sessionId, int algorithm);
	void setVolume(long sessionId, float left, float right);
	void start(long sessionId);
	long startSession(IDeathCallback cb);
	void stop(long sessionId);
	void unregisterOnBufferingUpdateCallback(long sessionId, IOnBufferingUpdateListenerCallback cb);
	void unregisterOnCompletionCallback(long sessionId, IOnCompletionListenerCallback cb);
	void unregisterOnErrorCallback(long sessionId, IOnErrorListenerCallback cb);
	void unregisterOnInfoCallback(long sessionId, IOnInfoListenerCallback cb);
	void unregisterOnPitchAdjustmentAvailableChangedCallback(long sessionId, IOnPitchAdjustmentAvailableChangedListenerCallback cb);
	void unregisterOnPreparedCallback(long sessionId, IOnPreparedListenerCallback cb);
	void unregisterOnSeekCompleteCallback(long sessionId, IOnSeekCompleteListenerCallback cb);
	void unregisterOnSpeedAdjustmentAvailableChangedCallback(long sessionId, IOnSpeedAdjustmentAvailableChangedListenerCallback cb);
}