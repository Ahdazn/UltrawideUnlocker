package com.example.ultrawideunlock;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.util.Range;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HookInit implements IXposedHookLoadPackage {

    private static final String TAG = "UltrawideUnlocker: ";

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        if (lpparam.packageName.equals("com.example.ultrawideunlock") || 
            lpparam.packageName.equals("android") || 
            lpparam.packageName.equals("com.android.systemui")) {
            return;
        }

        XposedBridge.log(TAG + "Injected into: " + lpparam.packageName);

        // 1. Hook Zoom Ratio (0.5x / 0.6x)
        try {
            XposedHelpers.findAndHookMethod(
                CameraCharacteristics.class,
                "get",
                CameraCharacteristics.Key.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        CameraCharacteristics.Key<?> key = (CameraCharacteristics.Key<?>) param.args[0];

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            if (CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE.equals(key)) {
                                @SuppressWarnings("unchecked")
                                Range<Float> orig = (Range<Float>) param.getResult();
                                float max = (orig != null) ? orig.getUpper() : 10.0f;
                                param.setResult(new Range<>(0.5f, Math.max(max, 5.0f)));
                            }
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            if (CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES.equals(key)) {
                                int[] caps = (int[]) param.getResult();
                                if (caps != null) {
                                    boolean hasLogical = false;
                                    for (int cap : caps) {
                                        if (cap == CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA) {
                                            hasLogical = true;
                                            break;
                                        }
                                    }
                                    if (!hasLogical) {
                                        int[] n = Arrays.copyOf(caps, caps.length + 1);
                                        n[caps.length] = CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA;
                                        param.setResult(n);
                                    }
                                }
                            }
                        }
                    }
                }
            );
        } catch (Throwable t) {
            XposedBridge.log(TAG + "Error hooking CameraCharacteristics: " + t.getMessage());
        }

        // 2. Hook Camera IDs
        try {
            XposedHelpers.findAndHookMethod(
                CameraManager.class,
                "getCameraIdList",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        String[] origIds = (String[]) param.getResult();
                        if (origIds == null) return;

                        List<String> idList = new ArrayList<>(Arrays.asList(origIds));
                        for (String id : new String[]{"2", "3", "4"}) {
                            if (!idList.contains(id)) {
                                idList.add(id);
                            }
                        }
                        param.setResult(idList.toArray(new String[0]));
                    }
                }
            );
        } catch (Throwable ignored) {}

        // 3. Bypass OEM Aux Whitelist
        try {
            Class<?> sysProp = XposedHelpers.findClass("android.os.SystemProperties", lpparam.classLoader);
            XposedHelpers.findAndHookMethod(
                sysProp,
                "get",
                String.class,
                String.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        String key = (String) param.args[0];
                        if (key != null && (key.contains("camera.aux.packagelist") || key.contains("camera.privapp.list"))) {
                            String orig = (String) param.getResult();
                            if (orig == null || orig.isEmpty()) {
                                param.setResult(lpparam.packageName);
                            } else if (!orig.contains(lpparam.packageName)) {
                                param.setResult(orig + "," + lpparam.packageName);
                            }
                        }
                    }
                }
            );
        } catch (Throwable ignored) {}
    }
}
