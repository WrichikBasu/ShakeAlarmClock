/*
 * Copyright (c) 2026. Wrichik Basu (basulabs.developer@gmail.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package in.basulabs.shakealarmclock.backend;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Objects;
import java.util.Queue;
import java.util.ArrayDeque;

public final class AlarmRingDS {

    private static final Queue<Bundle> ringQueue = new ArrayDeque<>();
    private static final Object lock = new Object();

    private static final MutableLiveData<Dictionary<Integer, Bundle>> snoozedAlarms
            = new MutableLiveData<>(new Hashtable<>());

    public static void enqueueRingQ(@NonNull Bundle alarmData) {
        synchronized (lock) {
            ringQueue.add(alarmData);
            Log.e(ConstantsAndStatics.DEBUG_TAG, "Alarm added to queue");
        }
    }

    @NonNull
    public static Bundle dequeueRingQ() {
        synchronized (lock) {
            Log.e(ConstantsAndStatics.DEBUG_TAG, "Alarm removed from queue");
            return ringQueue.remove();
        }
    }

    public static boolean isRingQEmpty() {
        synchronized (lock) {
            return ringQueue.isEmpty();
        }
    }

    public static void clearRingQ() {
        synchronized (lock) {
            ringQueue.clear();
        }
    }

    public static void addSnoozedAlarm(int alarmId, @NonNull Bundle alarmData) {
        Objects.requireNonNull(snoozedAlarms.getValue()).put(alarmId, alarmData);
    }

    public static void removeSnoozedAlarm(int alarmId) {
        Objects.requireNonNull(snoozedAlarms.getValue()).remove(alarmId);
    }

    public static void clearSnoozedAlarms() {
        snoozedAlarms.setValue(new Hashtable<>());
    }

    public static boolean isSnoozedAlarmsEmpty() {
        return Objects.requireNonNull(snoozedAlarms.getValue()).isEmpty();
    }

    public static Enumeration<Integer> getSnoozedAlarmIds() {
        return Objects.requireNonNull(snoozedAlarms.getValue()).keys();
    }

    public static Bundle getSnoozedAlarm(int alarmID) {
        return Objects.requireNonNull(snoozedAlarms.getValue()).get(alarmID);
    }

    @NonNull
    public static ArrayList<Bundle> getSnoozedAlarms() {
        ArrayList<Bundle> snoozedAlarmsList = new ArrayList<>();
        Enumeration<Integer> keys = Objects.requireNonNull(snoozedAlarms.getValue()).keys();
        while (keys.hasMoreElements()) {
            snoozedAlarmsList.add(snoozedAlarms.getValue().get(keys.nextElement()));
        }
        return snoozedAlarmsList;
    }

    public static LiveData<Dictionary<Integer, Bundle>> getSnoozedAlarmsLiveData() {
        return snoozedAlarms;
    }
}

