package calvin.order.taobao;

import android.accessibilityservice.AccessibilityService;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.List;

/**
 * Created by Administrator on 2016/10/26.
 */

public class TaobaoService extends AccessibilityService {

    public static final String search_result_activity =
            "com.taobao.search.mmd.SearchResultActivity";
    public static final String search_activity =
            "com.taobao.search.common.searchdoor.SearchDoorActivity";
    private ClipboardManager clipboardManager;
    private String TAG = "calvin";
    private boolean hasClearEdit = false;
    // 当前前台activity
    private String mForegroundActivity;

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
        Log.e(TAG, "type=" + typeStr);
        AccessibilityNodeInfo nodeInfo = event.getSource();
        if (nodeInfo == null) {
            return;
        }
        Log.i(TAG, "node=" + nodeInfo.getClassName());
        switch (type) {
            case AccessibilityEvent.TYPE_WINDOWS_CHANGED:
                break;
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                // 切入口,
                // 首先检查当前界面的activity
                mForegroundActivity = getForegroundActivity(event);
                Log.e(TAG, "foregroundActivity=" + mForegroundActivity);
                if (isSearchActivity()) {
                    // 在搜索界面
                    List<AccessibilityNodeInfo> editDelNodes =
                            nodeInfo.findAccessibilityNodeInfosByViewId(
                                    "com.taobao.taobao:id/edit_del_btn");
                    if (editDelNodes.size() > 0) {
                        //首先清理文本
                        if (editDelNodes.get(0).isVisibleToUser()) {
                            editDelNodes.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            hasClearEdit = true;
                        }
                    }
                    performSearch(nodeInfo);
                } else if (isSearchResultActivity()) {
                    // 在搜索结果页面
                    List<AccessibilityNodeInfo> listViewNodes =
                            nodeInfo.findAccessibilityNodeInfosByViewId(
                                    "com.taobao.taobao:id/search_listview");
                }
                break;
            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
                // 在刷新页面
                if (isSearchResultActivity()) {
                    boolean hasResult = false;
                    List<AccessibilityNodeInfo> titleNodes =
                            nodeInfo.findAccessibilityNodeInfosByViewId(
                                    "com.taobao.taobao:id/title");
                    if (titleNodes.size() > 0) {
                        for (AccessibilityNodeInfo titleNode : titleNodes) {
                            String className = titleNode.getClassName().toString();
                            if ("android.widget.TextView".equalsIgnoreCase(className)) {
                                String titleStr = titleNode.getText().toString();
                                if (titleStr.contains("腰医生")) {
                                    // 执行点击
                                    titleNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                                    hasResult = true;
                                    break;
                                }
                            }
                        }
                    }
                    if (!hasResult) {
                        // 当前列表无结果,滚动
                        List<AccessibilityNodeInfo> listViewNodes =
                                nodeInfo.findAccessibilityNodeInfosByViewId(
                                        "com.taobao.taobao:id/search_listview");
                        if (listViewNodes.size() > 0) {
                            AccessibilityNodeInfo listNode = listViewNodes.get(0);
                            listNode.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
                        }
                    }
                }
                break;
            case AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED:
                break;
        }
        hasClearEdit = false;
    }

    private void performSearch(AccessibilityNodeInfo nodeInfo) {
        // 判断是否在搜索结果页面
        // 获取edit
        List<AccessibilityNodeInfo> searchEditNodes =
                nodeInfo.findAccessibilityNodeInfosByViewId("com.taobao.taobao:id/searchEdit");
        if (searchEditNodes.size() > 0) {
            AccessibilityNodeInfo searchNodeInfo = searchEditNodes.get(0);
            // 获取焦点
            if (!searchNodeInfo.isFocused()) {
                searchNodeInfo.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
            }
            //首先清理文本
            setClipString("");
            searchNodeInfo.performAction(AccessibilityNodeInfo.ACTION_PASTE);
            // 再发送本文
            setClipString("腰");
            searchNodeInfo.performAction(AccessibilityNodeInfo.ACTION_PASTE);
        }
        List<AccessibilityNodeInfo> searchBtnNodes =
                nodeInfo.findAccessibilityNodeInfosByViewId("com.taobao.taobao:id/searchbtn");
        if (searchBtnNodes.size() > 0) {
            AccessibilityNodeInfo searchBtnNode = searchBtnNodes.get(0);
            searchBtnNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        }
    }

    private boolean isSearchActivity() {
        return search_activity.equalsIgnoreCase(mForegroundActivity);
    }

    private boolean isSearchResultActivity() {
        return search_result_activity.equalsIgnoreCase(mForegroundActivity);
    }

    public String getForegroundActivity(AccessibilityEvent event) {
        String className = event.getClassName().toString();
        if (className.startsWith(".")) {
            className = event.getPackageName() + className;
        }
        return className;
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
