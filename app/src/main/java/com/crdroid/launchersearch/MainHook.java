package com.crdroid.launchersearch;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class MainHook implements IXposedHookLoadPackage {
    private static final String TAG = "LauncherSearchHook";
    private static final String TARGET_PKG = "com.android.launcher3";

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        if (!TARGET_PKG.equals(lpparam.packageName)) {
            return;
        }

        XposedBridge.log(TAG + ": Initializing in " + lpparam.packageName);

        try {
            Class<?> defaultProviderClass = XposedHelpers.findClassIfExists(
                    "com.android.launcher3.allapps.search.DefaultSearchAdapterProvider",
                    lpparam.classLoader
            );

            if (defaultProviderClass != null) {
                XposedHelpers.findAndHookMethod(
                        defaultProviderClass,
                        "performGoogleSearch",
                        View.class,
                        String.class,
                        new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                Object provider = param.thisObject;
                                Log.i(TAG, "performGoogleSearch intercepted!");

                                // Strategy 1: Check mHighlightedView in DefaultSearchAdapterProvider
                                View highlightedView = (View) XposedHelpers.getObjectField(provider, "mHighlightedView");
                                if (highlightedView != null) {
                                    Log.i(TAG, "Launching highlightedView: " + highlightedView);
                                    boolean handled = highlightedView.performClick();
                                    param.setResult(handled);
                                    return;
                                }

                                // Strategy 2: Check SearchRecyclerView first visible child
                                Object launcher = XposedHelpers.getObjectField(provider, "mLauncher");
                                if (launcher != null) {
                                    Object appsView = XposedHelpers.callMethod(launcher, "getAppsView");
                                    if (appsView != null) {
                                        ViewGroup searchRecyclerView = (ViewGroup) XposedHelpers.callMethod(appsView, "getSearchRecyclerView");
                                        if (searchRecyclerView != null && searchRecyclerView.getChildCount() > 0) {
                                            View firstChild = searchRecyclerView.getChildAt(0);
                                            if (firstChild != null) {
                                                Log.i(TAG, "Launching firstChild in SearchRecyclerView: " + firstChild);
                                                boolean handled = firstChild.performClick();
                                                param.setResult(handled);
                                                return;
                                            }
                                        }
                                    }
                                }

                                // Strategy 3: Check AlphabeticalAppsList directly
                                try {
                                    if (launcher != null) {
                                        Object appsView = XposedHelpers.callMethod(launcher, "getAppsView");
                                        if (appsView != null) {
                                            Object searchResultList = XposedHelpers.callMethod(appsView, "getSearchResultList");
                                            if (searchResultList != null) {
                                                java.util.List<?> adapterItems = (java.util.List<?>) XposedHelpers.callMethod(searchResultList, "getAdapterItems");
                                                if (adapterItems != null && !adapterItems.isEmpty()) {
                                                    Object firstItem = adapterItems.get(0);
                                                    Object itemInfo = XposedHelpers.getObjectField(firstItem, "itemInfo");
                                                    if (itemInfo != null) {
                                                        android.content.Intent intent = (android.content.Intent) XposedHelpers.callMethod(itemInfo, "getIntent");
                                                        if (intent != null) {
                                                            Log.i(TAG, "Launching intent from itemInfo: " + intent);
                                                            XposedHelpers.callMethod(launcher, "startActivitySafely", (View) param.args[0], intent, itemInfo);
                                                            param.setResult(true);
                                                            return;
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } catch (Throwable t) {
                                    Log.w(TAG, "Strategy 3 failed: " + t.getMessage());
                                }

                                Log.i(TAG, "No app search results found, letting Google web search proceed.");
                            }
                        }
                );
                XposedBridge.log(TAG + ": Successfully hooked performGoogleSearch");
            } else {
                XposedBridge.log(TAG + ": DefaultSearchAdapterProvider class not found");
            }
        } catch (Throwable t) {
            XposedBridge.log(TAG + " Error: " + t.getMessage());
        }
    }
}
