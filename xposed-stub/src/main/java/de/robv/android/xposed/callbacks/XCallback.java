package de.robv.android.xposed.callbacks;

public abstract class XCallback {
    public final int priority;

    public XCallback() {
        this.priority = 50;
    }

    public XCallback(int priority) {
        this.priority = priority;
    }

    public static class Param {
        public Param() {
        }
    }
}
