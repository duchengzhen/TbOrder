package calvin.order.taobao;

import android.accessibilityservice.AccessibilityService;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    // 上一个事件
    private AccessibilityEvent prevEvent;
    // 当前执行动作的节点
    private Set<AccessibilityNodeInfo> currentActionNodes = new HashSet<>();

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
        // 根节点
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) {
            return;
        }
        Log.i(TAG, "node=" + rootNode.getClassName());
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
                            rootNode.findAccessibilityNodeInfosByViewId(
                                    "com.taobao.taobao:id/edit_del_btn");
                    if (performAction(editDelNodes, AccessibilityNodeInfo.ACTION_CLICK)) {
                        hasClearEdit = true;
                    }
                    performSearch(rootNode);
                } else if (isSearchResultActivity()) {
                    // 在搜索结果页面
                    List<AccessibilityNodeInfo> listViewNodes =
                            rootNode.findAccessibilityNodeInfosByViewId(
                                    "com.taobao.taobao:id/search_listview");
                }
                break;
            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
                // 在刷新页面
                if (isSearchResultActivity()) {
                    boolean hasResult = false;
                    List<AccessibilityNodeInfo> titleNodes =
                            rootNode.findAccessibilityNodeInfosByViewId(
                                    "com.taobao.taobao:id/title");
                    if (titleNodes.size() > 0) {
                        for (AccessibilityNodeInfo titleNode : titleNodes) {
                            String className = titleNode.getClassName().toString();
                            if ("android.widget.TextView".equalsIgnoreCase(className)) {
                                String titleStr = titleNode.getText().toString();
                                Log.i(TAG, "标题文本->" + titleStr);
                                if (titleStr.contains("反C曲度")) {
                                    Log.e(TAG, "bingooooooo");
                                    // 停止正在执行的事件
                                    clearAccessibilityFocus();
                                    // 执行点击
                                    boolean action = performAction(titleNode, AccessibilityNodeInfo.ACTION_CLICK);
                                    AccessibilityNodeInfo tempNode = titleNode.getParent();
                                    while (!action) {
                                        if (tempNode != null) {
                                            action = performAction(tempNode, AccessibilityNodeInfo.ACTION_CLICK);
                                            tempNode = tempNode.getParent();
                                        } else {
                                            break;
                                        }
                                    }
                                    // 执行说明有结果
                                    hasResult = action;
                                    break;
                                }
                            }
                        }
                    }
                    if (!hasResult) {
                        // 当前列表无结果,执行滚动
                        List<AccessibilityNodeInfo> listViewNodes =
                                rootNode.findAccessibilityNodeInfosByViewId(
                                        "com.taobao.taobao:id/search_listview");
                        performAction(listViewNodes, AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
                    }
                    // 防止列表卡在[加载中...]
                    if (prevEvent != null && prevEvent.getEventType() == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
                        performLoadMore(rootNode);
                    }
                }
                break;
            case AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED:

                break;
            case AccessibilityEvent.TYPE_VIEW_SCROLLED:
                // 视图滚动
                performLoadMore(rootNode);
                break;
        }
        hasClearEdit = false;
        prevEvent = event;
    }

    private void performLoadMore(AccessibilityNodeInfo rootNode) {
        List<AccessibilityNodeInfo> tipNodes = rootNode.findAccessibilityNodeInfosByViewId("com.taobao.taobao:id/tipText");
        if (!performAction(tipNodes, AccessibilityNodeInfo.ACTION_CLICK)) {
            performParentAction(tipNodes, AccessibilityNodeInfo.ACTION_CLICK);
        }
    }

    private boolean performParentAction(List<AccessibilityNodeInfo> nodeInfos, int action) {

        boolean result = false;
        for (AccessibilityNodeInfo nodeInfo : nodeInfos) {
            if (performAction(nodeInfo.getParent(), action)) {
                result = true;
            }
        }
        return result;
    }

    private boolean performAction(List<AccessibilityNodeInfo> nodeInfos, int action) {
        boolean result = false;
        for (AccessibilityNodeInfo nodeInfo : nodeInfos) {
            if (performAction(nodeInfo, action)) {
                result = true;
            }
        }
        return result;
    }

    private boolean performAction(AccessibilityNodeInfo nodeInfo, int action) {
        if (nodeInfo == null || !nodeInfo.isVisibleToUser()) {
            return false;
        }
        boolean result = nodeInfo.performAction(action);
        if (result) {
            currentActionNodes.add(nodeInfo);
        }
        return result;
    }

    private void clearAccessibilityFocus() {
        for (AccessibilityNodeInfo currentActionNode : currentActionNodes) {
            currentActionNode.performAction(AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS);
        }
        // 清理
        currentActionNodes.clear();
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
