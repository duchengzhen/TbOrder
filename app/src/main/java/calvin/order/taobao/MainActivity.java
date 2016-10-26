package calvin.order.taobao;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;

import java.util.List;

public class MainActivity extends Activity {

    private boolean serviceEnabled;
    private Button btnSetting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setPackage("com.taobao.taobao");
                intent.setClassName("com.taobao.taobao", "com.taobao.search.common.searchdoor.SearchDoorActivity");
                //intent.setAction("android.intent.action.MAIN");
                //intent.addCategory("android.intent.category.LAUNCHER");
                //intent.addCategory("android.intent.category.DEFAULT");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });

        btnSetting = (Button) findViewById(R.id.btn2);
        btnSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!serviceEnabled) {
                    startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                }
                if (!hasUsagePermission()) {
                    Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                    startActivity(intent);
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateServiceStatus();
    }

    private void updateServiceStatus() {
        // booleanServiceEnabled = false;
        // 循环遍历所有服务，查看是否开启
        AccessibilityManager accessibilityManager = (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);
        List<AccessibilityServiceInfo> accessibilityServices = accessibilityManager
                .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC);
        for (AccessibilityServiceInfo info : accessibilityServices) {
            if (info.getId().equals(getPackageName() + "/.TaobaoService")) {
                serviceEnabled = true;
                break;
            }
        }
        if (serviceEnabled) {
            btnSetting.setText("服务已开启");
        } else {
            btnSetting.setText("开启服务");
        }
    }

    //判断调用该设备中“有权查看使用权限的应用”这个选项的APP有没有打开
    private boolean hasUsagePermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return true;
        }
        long ts = System.currentTimeMillis();
        UsageStatsManager usageStatsManager = (UsageStatsManager)
                getApplicationContext().getSystemService("usagestats");
        List queryUsageStats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_BEST, 0, ts);
        if (queryUsageStats == null || queryUsageStats.isEmpty()) {
            return false;
        }
        return true;
    }

}
