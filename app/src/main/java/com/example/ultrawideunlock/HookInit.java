package com.example.ultrawideunlock;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.util.Range;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class HookInit implements IXposedHookLoadPackage {

    private static final String TAG = "UltrawideUnlocker: ";

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        final String pkg = lpparam.packageName;
        if (pkg.equals("com.example.ultrawideunlock")
                || pkg.equals("android")
                || pkg.equals("com.android.systemui")) {
            return;
        }

        XposedBridge.log(TAG + "Injected into: " + pkg);

        hookCameraCharacteristics();
        hookCameraIdList();
        hookAuxWhitelist(lpparam);
    }

    /** 1. Zoom ratio 0.5x dan capability logical multi camera. */
    private void hookCameraCharacteristics() {
        try {
            XposedHelpers.findAndHookMethod(
                    CameraCharacteristics.class,
                    "get",
                    CameraCharacteristics.Key.class,
                    new XC_MethodHook() {
                        @Override
                        @SuppressWarnings("unchecked")
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            Object keyObj = param.args[0];
                            if (keyObj == null) return;

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                                    && CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE.equals(keyObj)) {
                                Range<Float> orig = (Range<Float>) param.getResult();
                                float max = (orig != null) ? orig.getUpper() : 10.0f;
                                param.setResult(new Range<>(0.5f, Math.max(max, 5.0f)));
                                return;
                            }

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                                    && CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES.equals(keyObj)) {
                                int[] caps = (int[]) param.getResult();
                                if (caps == null) return;
                                for (int cap : caps) {
                                    if (cap == CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA) {
                                        return;
                                    }
                                }
                                int[] n = Arrays.copyOf(caps, caps.length + 1);
                                n[caps.length] = CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA;
                                param.setResult(n);
                            }
                        }
                    }
            );
        } catch (Throwable t) {
            XposedBridge.log(TAG + "Gagal hook CameraCharacteristics: " + t);
        }
    }

    /** 2. Tampilkan Camera ID tambahan (2, 3, 4) hanya jika benar-benar bisa diakses. */
    private void hookCameraIdList() {
        try {
            XposedHelpers.findAndHookMethod(
                    CameraManager.class,
                    "getCameraIdList",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            String[] origIds = (String[]) param.getResult();
                            if (origIds == null) return;

                            CameraManager manager = (CameraManager) param.thisObject;
                            List<String> idList = new ArrayList<>(Arrays.asList(origIds));
                            boolean changed = false;

                            for (String id : new String[]{"2", "3", "4"}) {
                                if (idList.contains(id)) continue;
                                try {
                                    // Jika ID tidak valid, baris ini melempar exception dan ID dilewati.
                                    manager.getCameraCharacteristics(id);
                                    idList.add(id);
                                    changed = true;
                                } catch (Throwable ignored) {
                                    // ID tidak ada / tidak bisa diakses: jangan ditambahkan.
                                }
                            }

                            if (changed) {
                                param.setResult(idList.toArray(new String[0]));
                            }
                        }
                    }
            );
        } catch (Throwable t) {
            XposedBridge.log(TAG + "Gagal hook getCameraIdList: " + t);
        }
    }

    /** 3. Lewati whitelist aux kamera dari vendor. */
    private void hookAuxWhitelist(final LoadPackageParam lpparam) {
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
                            if (key == null) return;
                            if (key.contains("camera.aux.packagelist") || key.contains("camera.privapp.list")) {
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
        } catch (Throwable t) {
            XposedBridge.log(TAG + "Gagal hook SystemProperties: " + t);
        }
    }
}
