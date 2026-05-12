package in.basulabs.shakealarmclock.backend;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

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

    public static LiveData<Dictionary<Integer, Bundle>> getSnoozedAlarmsLiveData() {
        return snoozedAlarms;
    }
}

