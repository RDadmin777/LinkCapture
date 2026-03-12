package com.linkcapture.app;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import de.robv.android.xposed.XposedBridge;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.app.Application;
import android.app.Activity;

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
        hookIntent(lpparam);
    }

    private void hookWebView(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookMethod("android.webkit.WebView", lpparam.classLoader,
                "loadUrl", String.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        saveLink((String) param.args[0], "WebView.loadUrl");
                    }
                });
        } catch (Throwable t) {
            XposedBridge.log("LinkCapture: WebView hook failed: " + t.getMessage());
        }

        try {
            XposedHelpers.findAndHookMethod("android.webkit.WebView", lpparam.classLoader,
                "loadUrl", String.class, java.util.Map.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        saveLink((String) param.args[0], "WebView.loadUrl2");
                    }
                });
        } catch (Throwable t) {}
    }

    private void hookURL(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookMethod("java.net.URL", lpparam.classLoader,
                "openConnection", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        saveLink(param.thisObject.toString(), "URL.openConnection");
                    }
                });
        } catch (Throwable t) {
            XposedBridge.log("LinkCapture: URL hook failed: " + t.getMessage());
        }
    }

    private void hookIntent(XC_LoadPackage.LoadPackageParam lpparam) {
        // Hook startActivity to capture alipays://, weixin://, scheme links
        try {
            XposedHelpers.findAndHookMethod(Activity.class, "startActivity", Intent.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    Intent intent = (Intent) param.args[0];
                    extractFromIntent(intent, "startActivity");
                }
            });
        } catch (Throwable t) {
            XposedBridge.log("LinkCapture: startActivity hook failed: " + t.getMessage());
        }

        // Hook startActivityForResult
        try {
            XposedHelpers.findAndHookMethod(Activity.class, "startActivityForResult",
                Intent.class, int.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        Intent intent = (Intent) param.args[0];
                        extractFromIntent(intent, "startActivityForResult");
                    }
                });
        } catch (Throwable t) {}

        // Hook Context.startActivity
        try {
            XposedHelpers.findAndHookMethod("android.content.ContextWrapper", lpparam.classLoader,
                "startActivity", Intent.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        Intent intent = (Intent) param.args[0];
                        extractFromIntent(intent, "Context.startActivity");
                    }
                });
        } catch (Throwable t) {}

        // Hook Intent.setData to capture URI setting
        try {
            XposedHelpers.findAndHookMethod(Intent.class, "setData", Uri.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    Uri uri = (Uri) param.args[0];
                    if (uri != null) {
                        String scheme = uri.getScheme();
                        if (scheme != null && !scheme.equals("content") && !scheme.equals("file") && !scheme.equals("android-app")) {
                            saveLink(uri.toString(), "Intent.setData");
                        }
                    }
                }
            });
        } catch (Throwable t) {}

        // Hook Intent constructor with action and Uri
        try {
            XposedHelpers.findAndHookConstructor(Intent.class, String.class, Uri.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    Uri uri = (Uri) param.args[1];
                    if (uri != null) {
                        saveLink(uri.toString(), "new Intent(action,uri)");
                    }
                }
            });
        } catch (Throwable t) {}
    }

    private void extractFromIntent(Intent intent, String method) {
        if (intent == null) return;
        try {
            Uri data = intent.getData();
            if (data != null) {
                String url = data.toString();
                String scheme = data.getScheme();
                if (scheme != null && !scheme.equals("content") && !scheme.equals("file") && !scheme.equals("android-app")) {
                    saveLink(url, method);
                }
            }
            // Also check EXTRA_TEXT for shared links
            String extraText = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (extraText != null && (extraText.contains("://") || extraText.contains("http"))) {
                saveLink(extraText, method + "/EXTRA_TEXT");
            }
        } catch (Throwable t) {}
    }

    private void saveLink(String url, String method) {
        if (url == null || url.isEmpty()) return;
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
