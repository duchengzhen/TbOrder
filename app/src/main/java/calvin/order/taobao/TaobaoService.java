package calvin.order.taobao;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityThread;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

/**
 * Created by Administrator on 2016/10/26.
 */

public class TaobaoService extends AccessibilityService {


    private ClipboardManager clipboardManager;
    private String TAG = "calvin";
    private boolean hasClearEdit = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        // 获取剪切板对象
        clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
    }


    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        final int type = event.getEventType();
        String typeStr = AccessibilityEvent.eventTypeToString(type);
        Log.e("calvin", typeStr);
        AccessibilityNodeInfo nodeInfo = event.getSource();
        if (nodeInfo == null) {
            return;
        }
        switch (type) {
            case AccessibilityEvent.TYPE_WINDOWS_CHANGED:
                break;
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                // 切入口
                // 首先检查当前界面的activity
                String activityName = getRunningActivity();
                Log.e("calvin", activityName);
                List<AccessibilityNodeInfo> editDelNodes = nodeInfo.findAccessibilityNodeInfosByViewId("com.taobao.taobao:id/edit_del_btn");
                if (editDelNodes.size() > 0) {
                    //首先清理文本
                    if (editDelNodes.get(0).isVisibleToUser()) {
                        editDelNodes.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        hasClearEdit = true;
                    }
                }
                performSearch(nodeInfo);
                break;
            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
                //performSearch(nodeInfo);
                //
                break;
            case AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED:
                //performSearch(nodeInfo);
                break;
        }
        hasClearEdit = false;
    }


    private void performSearch(AccessibilityNodeInfo nodeInfo) {
        // 判断是否在搜索结果页面
        // 获取edit
        List<AccessibilityNodeInfo> searchEditNodes = nodeInfo.findAccessibilityNodeInfosByViewId("com.taobao.taobao:id/searchEdit");
        if (searchEditNodes.size() > 0) {
            AccessibilityNodeInfo searchNodeInfo = searchEditNodes.get(0);
            // huoqu jiaodian
            if (!searchNodeInfo.isFocused()) {
                searchNodeInfo.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
            }
            //首先清理文本
            setClipString("");
            searchNodeInfo.performAction(AccessibilityNodeInfo.ACTION_PASTE);
            // 再发送本文
            setClipString("微软手机");
            searchNodeInfo.performAction(AccessibilityNodeInfo.ACTION_PASTE);
        }
        List<AccessibilityNodeInfo> searchBtnNodes = nodeInfo.findAccessibilityNodeInfosByViewId("com.taobao.taobao:id/searchbtn");
        if (searchBtnNodes.size() > 0) {
            AccessibilityNodeInfo searchBtnNode = searchBtnNodes.get(0);
            searchBtnNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        }
    }

    private String getRunningActivity() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return getForegroundActivity();
        } else {
            return getRunningApp();
        }
    }


    /**
     * 6.0 pre
     *
     * @return
     */
    public String getForegroundActivity() {
        ActivityManager mActivityManager =
                (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (mActivityManager.getRunningTasks(1) == null) {
            Log.e(TAG, "running task is null, ams is abnormal!!!");
            return null;
        }
        ActivityManager.RunningTaskInfo mRunningTask =
                mActivityManager.getRunningTasks(1).get(0);
        if (mRunningTask == null) {
            Log.e(TAG, "failed to get RunningTaskInfo");
            return null;
        }

        String pkgName = mRunningTask.topActivity.getClassName();
        //String activityName =  mRunningTask.topActivity.getClassName();
        return pkgName;
    }

    /**
     * 6.0 over
     *
     * @return
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private String getRunningApp() {
        UsageStatsManager usageStatsManager =
                (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        long ts = System.currentTimeMillis();
        List<UsageStats> queryUsageStats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_BEST, ts - 2000, ts);
        if (queryUsageStats == null || queryUsageStats.isEmpty()) {
            return null;
        }
        UsageStats recentStats = null;
        for (UsageStats usageStats : queryUsageStats) {
            if (recentStats == null ||
                    recentStats.getLastTimeUsed() < usageStats.getLastTimeUsed()) {
                recentStats = usageStats;
            }
        }
        return recentStats.getPackageName();
    }

    private void setClipString(String str) {
        if (clipboardManager == null) {
            return;
        }
        ClipData clipData = ClipData.newPlainText("关键词", str);
        clipboardManager.setPrimaryClip(clipData);
    }

    @Override
    public void onInterrupt() {

    }


}
