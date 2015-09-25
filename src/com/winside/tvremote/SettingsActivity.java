package com.winside.tvremote;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;

import com.winside.tvremote.util.LogUtils;
import com.winside.tvremote.util.PromptManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Author        : lu
 * Data          : 2015/9/25
 * Time          : 11:05
 * Decription    :
 */
public class SettingsActivity extends CommonTitleActivity {

    private ListView settings_listview;
    private SettingsAdapter settingsAdapter;
    private ArrayList<SettingItem> settingItems;
    private SharedPreferences sharedPreferences;
    private static final String vibrator_str = "振动反馈";
    private static final String auto_connect_device = "自动连接设备";
    private SharedPreferences.Editor edit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.setting);

        sharedPreferences = getSharedPreferences(ConstValues.settings, Context.MODE_PRIVATE);
        edit = sharedPreferences.edit();

        actionBar.setTitle(R.string.app_setting);
        settings_listview = ((ListView) findViewById(R.id.settings));

        initData();
        settingsAdapter = new SettingsAdapter(this, settingItems);
        settings_listview.setAdapter(settingsAdapter);

        final File filesDir = getFilesDir();
        final File cacheDir = getCacheDir();
        final File sharedDir = new File("/data/data/" + getPackageName() + "/shared_prefs");

        settings_listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        //TODO 暂无功能具体的实现，默认是自动连接
                        boolean is_auto_connect = sharedPreferences.getBoolean(ConstValues
                                .auto_connect_device, true);
                        if (is_auto_connect) {
                            edit.putBoolean(ConstValues.auto_connect_device, false);
                            LogUtils.e("取消自动连接 " + is_auto_connect);
                        } else {
                            edit.putBoolean(ConstValues.auto_connect_device, true);
                            LogUtils.e("激活自动连接 " + is_auto_connect);
                        }
                        edit.commit();
                        settingsAdapter.notifyDataSetInvalidated();
                        break;
                    case 1:
                        try {
                            deleteFilesByDirectory(cacheDir);
                            deleteFilesByDirectory(filesDir);
                            deleteFilesByDirectory(sharedDir);
                        } catch (Exception e) {
                            e.printStackTrace();
                            PromptManager.showToast(SettingsActivity.this, "重置失败!");
                        }
                        PromptManager.showToast(SettingsActivity.this, "重置应用成功!");
                        break;
                    case 2:
                        boolean is_vibrator = sharedPreferences.getBoolean(ConstValues.vibrator, true);
                        if (is_vibrator) {
                            edit.putBoolean(ConstValues.vibrator, false);
                            LogUtils.e("取消振动 " + is_vibrator);
                        } else {
                            edit.putBoolean(ConstValues.vibrator, true);
                            LogUtils.e("激活振动 " + is_vibrator);
                        }
                        edit.commit();
                        settingsAdapter.notifyDataSetInvalidated();
                        break;
                    case 3:
                        Intent about = new Intent(SettingsActivity.this, AboutActivity.class);
                        startActivity(about);
                        break;
                    default:

                        break;
                }
            }
        });
    }

    private void initData() {
        settingItems = new ArrayList<SettingItem>();
        SettingItem auto_conn = new SettingItem(auto_connect_device, "自动匹配并连接可用设备", true);
        SettingItem reset_app = new SettingItem("重置应用", "清理缓存，恢复默认数据", false);
        SettingItem vibrator = new SettingItem(vibrator_str, "进行远程操作时智能振动", true);
        SettingItem about = new SettingItem("关于", "关于该应用的一些其他信息", false);

        settingItems.add(auto_conn);
        settingItems.add(reset_app);
        settingItems.add(vibrator);
        settingItems.add(about);
    }

    class SettingsAdapter extends BaseAdapter {

        private Context context;
        private List<SettingItem> settingItemList;
        private LayoutInflater inflater;

        public SettingsAdapter(Context context, List<SettingItem> settingItemList) {
            this.context = context;
            this.settingItemList = settingItemList;
            inflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return settingItemList.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            ViewHolder holder = null;

            if (convertView == null) {
                convertView = inflater.inflate(R.layout.setting_item, null);
                holder = new ViewHolder();
                holder.itemName_tv = ((TextView) convertView.findViewById(R.id.item_name));
                holder.item_description_tv = (TextView) convertView.findViewById(R.id.item_description);
                holder.is_enable_cb = (CheckBox) convertView.findViewById(R.id.is_enable);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            if ("关于".equals(settingItems.get(position).getItemName()) || "重置应用".equals(settingItems.get(position).getItemName())) {
                holder.is_enable_cb.setVisibility(View.GONE);
            }

            holder.itemName_tv.setText(settingItems.get(position).getItemName());
            holder.item_description_tv.setText(settingItems.get(position).getItem_description());

            if (auto_connect_device.equals(settingItems.get(position).getItemName())) {
                if (sharedPreferences.getBoolean(ConstValues.auto_connect_device, true)) {
                    holder.is_enable_cb.setChecked(true);
                } else {
                    holder.is_enable_cb.setChecked(false);
                }

            }
            if (vibrator_str.equals(settingItems.get(position).getItemName())) {
                if (sharedPreferences.getBoolean(ConstValues.vibrator, true)) {
                    holder.is_enable_cb.setChecked(true);
                } else {
                    holder.is_enable_cb.setChecked(false);
                }
            }
            return convertView;
        }

        class ViewHolder {
            TextView itemName_tv;
            TextView item_description_tv;
            CheckBox is_enable_cb;
        }
    }

    private class SettingItem {
        String itemName;
        String item_description;
        boolean is_enbale;

        public SettingItem(String itemName, String item_description, boolean is_enbale) {
            this.itemName = itemName;
            this.item_description = item_description;
            this.is_enbale = is_enbale;
        }

        public SettingItem() {
        }

        public String getItemName() {
            return itemName;
        }

        public void setItemName(String itemName) {
            this.itemName = itemName;
        }

        public String getItem_description() {
            return item_description;
        }

        public void setItem_description(String item_description) {
            this.item_description = item_description;
        }

        public boolean is_enbale() {
            return is_enbale;
        }

        public void setIs_enbale(boolean is_enbale) {
            this.is_enbale = is_enbale;
        }

        @Override
        public String toString() {
            return "SettingItem{" +
                    "itemName='" + itemName + '\'' +
                    ", item_description='" + item_description + '\'' +
                    ", is_enbale=" + is_enbale +
                    '}';
        }
    }

    private static void deleteFilesByDirectory(File directory) {
        if (directory != null && directory.exists() && directory.isDirectory()) {
            for (File item : directory.listFiles()) {
                item.delete();
            }
        }
    }

}
