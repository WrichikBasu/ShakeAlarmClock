package in.basulabs.shakealarmclock.backend;

import android.os.Bundle;
import android.util.Log;

import java.util.Queue;
import java.util.ArrayDeque;

public final class AlarmRingQueue {

    private static final Queue<Bundle> queue = new ArrayDeque<>();
    private static final Object lock = new Object();

    private static boolean isRunning = false;

    public static void enqueue(Bundle alarmData) {
        synchronized (lock) {
            queue.add(alarmData);
            Log.e(ConstantsAndStatics.DEBUG_TAG, "Alarm added to queue");
        }
    }

    public static Bundle dequeue() {
        synchronized (lock) {
            Log.e(ConstantsAndStatics.DEBUG_TAG, "Alarm removed from queue");
            return queue.poll();
        }
    }

    public static boolean isRunning() {
        synchronized (lock) {
            return isRunning;
        }
    }

    public static void setRunning(boolean value) {
        synchronized (lock) {
            isRunning = value;
        }
    }

    public static boolean isEmpty() {
        synchronized (lock) {
            return queue.isEmpty();
        }
    }

    public static void clear() {
        synchronized (lock) {
            queue.clear();
        }
    }
}

