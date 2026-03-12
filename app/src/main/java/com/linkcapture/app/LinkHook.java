package com.linkcapture.app;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import de.robv.android.xposed.XposedBridge;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.app.Application;

public class LinkHook implements IXposedHookLoadPackage {
    private Context appContext;
    private String currentPkg;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        currentPkg = lpparam.packageName;
        XposedHelpers.findAndHookMethod(Application.class, "onCreate", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                appContext = (Context) param.thisObject;
            }
        });
        hookWebView(lpparam);
        hookURL(lpparam);
    }

    private void hookWebView(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookMethod("android.webkit.WebView", lpparam.classLoader,
                "loadUrl", String.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        saveLink((String) param.args[0], "WebView");
                    }
                });
        } catch (Throwable t) {
            XposedBridge.log("LinkCapture: WebView hook failed: " + t.getMessage());
        }
    }

    private void hookURL(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookMethod("java.net.URL", lpparam.classLoader,
                "openConnection", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        saveLink(param.thisObject.toString(), "URL");
                    }
                });
        } catch (Throwable t) {
            XposedBridge.log("LinkCapture: URL hook failed: " + t.getMessage());
        }
    }

    private void saveLink(String url, String method) {
        if (url == null || url.isEmpty()) return;
        if (!url.startsWith("http")) return;
        try {
            if (appContext != null) {
                ContentValues cv = new ContentValues();
                cv.put("url", url);
                cv.put("source", currentPkg + "/" + method);
                cv.put("timestamp", System.currentTimeMillis());
                appContext.getContentResolver().insert(
                    Uri.parse("content://com.linkcapture.app.provider/links"), cv);
            }
        } catch (Throwable t) {
            XposedBridge.log("LinkCapture: save failed: " + t.getMessage());
        }
    }
}