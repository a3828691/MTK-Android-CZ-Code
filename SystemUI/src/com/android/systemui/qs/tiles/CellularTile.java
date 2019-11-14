/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.qs.tiles;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.SystemProperties;
import android.service.quicksettings.Tile;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
import android.widget.Switch;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settingslib.net.DataUsageController;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.R.string;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.qs.DetailAdapter;
import com.android.systemui.plugins.qs.QSIconView;
import com.android.systemui.plugins.qs.QSTile.SignalState;
import com.android.systemui.qs.CellTileView;
import com.android.systemui.qs.CellTileView.SignalIcon;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.SignalTileView;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.android.systemui.statusbar.phone.SystemUIDialog;
import com.android.systemui.statusbar.policy.KeyguardMonitor;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.NetworkController.IconState;
import com.android.systemui.statusbar.policy.NetworkController.SignalCallback;

/// M: add DataUsage in quicksetting @{
import com.mediatek.systemui.ext.IQuickSettingsPlugin;
import com.mediatek.systemui.ext.OpSystemUICustomizationFactoryBase;
/// add DataUsage in quicksetting @}

import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionInfo;
import java.util.List;


/** Quick settings tile: Cellular **/
public class CellularTile extends QSTileImpl<SignalState> {
    private static final ComponentName CELLULAR_SETTING_COMPONENT = new ComponentName(
            "com.android.settings", "com.android.settings.Settings$DataUsageSummaryActivity");
    private static final ComponentName DATA_PLAN_CELLULAR_COMPONENT = new ComponentName(
            "com.android.settings", "com.android.settings.Settings$DataPlanUsageSummaryActivity");

    private static final Intent CELLULAR_SETTINGS =
            new Intent().setComponent(CELLULAR_SETTING_COMPONENT);
    private static final Intent DATA_PLAN_CELLULAR_SETTINGS =
            new Intent().setComponent(DATA_PLAN_CELLULAR_COMPONENT);

    private static final String ENABLE_SETTINGS_DATA_PLAN = "enable.settings.data.plan";

    // / M: For debug @{
    private static final String TAG = "CellularTile";
    private static final boolean DBG = true;
    // @}
    /// M: add DataUsage for operator @{
    private IQuickSettingsPlugin mQuickSettingsPlugin;
    private boolean mDisplayDataUsage;
    private Icon mIcon;
    /// add DataUsage for operator @}

    private final NetworkController mController;
    private final DataUsageController mDataController;
    private final CellularDetailAdapter mDetailAdapter;

    private final CellSignalCallback mSignalCallback = new CellSignalCallback();
    private final ActivityStarter mActivityStarter;
    private final KeyguardMonitor mKeyguardMonitor;

    public CellularTile(QSHost host) {
        super(host);
        mController = Dependency.get(NetworkController.class);
        mActivityStarter = Dependency.get(ActivityStarter.class);
        mKeyguardMonitor = Dependency.get(KeyguardMonitor.class);
        mDataController = mController.getMobileDataController();
        mDetailAdapter = new CellularDetailAdapter();
        /// M: add DataUsage for operator @{
        mQuickSettingsPlugin = OpSystemUICustomizationFactoryBase
                .getOpFactory(mContext).makeQuickSettings(mContext);
        mDisplayDataUsage = mQuickSettingsPlugin.customizeDisplayDataUsage(false);
        mIcon = ResourceIcon.get(R.drawable.ic_qs_data_usage);
        /// add DataUsage for operator @}
    }

    @Override
    public SignalState newTileState() {
        return new SignalState();
    }

    @Override
    public DetailAdapter getDetailAdapter() {
        return mDetailAdapter;
    }

    @Override
    public void handleSetListening(boolean listening) {
        if (listening) {
            mController.addCallback(mSignalCallback);
        } else {
            mController.removeCallback(mSignalCallback);
        }
    }

    @Override
    public QSIconView createTileView(Context context) {
        /// M: add DataUsage for operator @{
        if (mDisplayDataUsage) {
            return new SignalTileView(context);
        }
        /// add DataUsage for operator @}
        return new CellTileView(context);
    }

    @Override
    public Intent getLongClickIntent() {
        return getCellularSettingIntent(mContext);
    }

    @Override
    protected void handleClick() {
        int subId = getActiveSubId();

        if (mDataController.isMobileDataEnabled()) {
            if (mKeyguardMonitor.isSecure() && !mKeyguardMonitor.canSkipBouncer()) {
                mActivityStarter.postQSRunnableDismissingKeyguard(this::showDisableDialog);
            } else {
                mUiHandler.post(this::showDisableDialog);
            }
        } else {
            Log.e("DataUsageController", "setMobileDataEnabled");

            mDataController.setMobileDataEnabled(true);
            /// M: Disable other sub's data when enable default sub's data @{
            mQuickSettingsPlugin.disableDataForOtherSubscriptions();
            /// @}
        }
    }
    //20190926 pjz add default choose SIM CARD 1
    private int getActiveSubId(){
        int defaultSubId = 1;
        SubscriptionManager mSubscriptionManager = SubscriptionManager.from(mContext);
        List<SubscriptionInfo> subscriptions = mSubscriptionManager.getActiveSubscriptionInfoList();

        if (subscriptions == null || subscriptions.size() == 0) {
            Log.e("DataUsageController", "size 0 need defaultSubId");
        }else{
            defaultSubId = subscriptions.get(0).getSubscriptionId();
        }
        for (int i = 0; subscriptions != null && i < subscriptions.size(); i++) {
            SubscriptionInfo subInfo = subscriptions.get(i);
            Log.i("DataUsageController", "subId="+subInfo.getSubscriptionId() + "  num=="+i);
        }
        mSubscriptionManager.setDefaultDataSubId(defaultSubId);
        return defaultSubId;
    }
    //E

    private void showDisableDialog() {
        mHost.collapsePanels();
        AlertDialog dialog = new Builder(mContext)
                .setMessage(string.data_usage_disable_mobile)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(
                        com.android.internal.R.string.alert_windows_notification_turn_off_action,
                        (d, w) -> mDataController.setMobileDataEnabled(false))
                .create();
        dialog.getWindow().setType(LayoutParams.TYPE_KEYGUARD_DIALOG);
        SystemUIDialog.setShowForAllUsers(dialog, true);
        SystemUIDialog.registerDismissListener(dialog);
        SystemUIDialog.setWindowOnTop(dialog);
        dialog.show();
    }

    @Override
    protected void handleSecondaryClick() {
        if (mDataController.isMobileDataSupported()) {
            showDetail(true);
        } else {
            mActivityStarter
                    .postStartActivityDismissingKeyguard(getCellularSettingIntent(mContext),
                            0 /* delay */);
        }
    }

    @Override
    public CharSequence getTileLabel() {
        /// M: add DataUsage for operator @{
        if (mDisplayDataUsage) {
            return mContext.getString(R.string.data_usage);
        }
        /// add DataUsage for operator @}
        return mContext.getString(R.string.quick_settings_cellular_detail_title);
    }

    @Override
    protected void handleUpdateState(SignalState state, Object arg) {
        /// M: add DataUsage for operator @{
        if (mDisplayDataUsage) {
            //state.visible = true;
            state.icon = mIcon;
            state.label = mContext.getString(R.string.data_usage);
            state.contentDescription = mContext.getString(R.string.data_usage);
            return;
        }
        /// add DataUsage for operator @}

        CallbackInfo cb = (CallbackInfo) arg;
        if (cb == null) {
            cb = mSignalCallback.mInfo;
        }

        final Resources r = mContext.getResources();
        state.activityIn = cb.enabled && cb.activityIn;
        state.activityOut = cb.enabled && cb.activityOut;
        state.isOverlayIconWide = cb.isDataTypeIconWide;
        state.overlayIconId = cb.dataTypeIconId;

        state.label = r.getString(R.string.mobile_data);

        final String signalContentDesc = cb.enabled && (cb.mobileSignalIconId > 0)
                ? cb.signalContentDescription
                : r.getString(R.string.accessibility_no_signal);
        if (cb.noSim) {
            state.contentDescription = state.label;
        } else {
            state.contentDescription = signalContentDesc + ", " + state.label;
        }

        state.expandedAccessibilityClassName = Switch.class.getName();
        state.value = mDataController.isMobileDataSupported()
                && mDataController.isMobileDataEnabled();

        if (cb.noSim) {
            state.icon = ResourceIcon.get(R.drawable.ic_qs_no_sim);
        } else {
            state.icon = new SignalIcon(cb.mobileSignalIconId);
        }

        if (cb.airplaneModeEnabled | cb.noSim) {
            state.state = Tile.STATE_INACTIVE;
        } else {
            state.state = Tile.STATE_ACTIVE;
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.QS_CELLULAR;
    }

    @Override
    public boolean isAvailable() {
        return mController.hasMobileDataFeature();
    }

    // Remove the period from the network name
    public static String removeTrailingPeriod(String string) {
        if (string == null) return null;
        final int length = string.length();
        if (string.endsWith(".")) {
            return string.substring(0, length - 1);
        }
        return string;
    }

    private static final class CallbackInfo {
        boolean enabled;
        boolean wifiEnabled;
        boolean airplaneModeEnabled;
        int mobileSignalIconId;
        String signalContentDescription;
        int dataTypeIconId;
        String dataContentDescription;
        boolean activityIn;
        boolean activityOut;
        String enabledDesc;
        boolean noSim;
        boolean isDataTypeIconWide;
        boolean roaming;
    }

    private final class CellSignalCallback implements SignalCallback {
        private final CallbackInfo mInfo = new CallbackInfo();
        @Override
        public void setWifiIndicators(boolean enabled, IconState statusIcon, IconState qsIcon,
                boolean activityIn, boolean activityOut, String description, boolean isTransient) {
            mInfo.wifiEnabled = enabled;
            refreshState(mInfo);
        }

        @Override
        public void setMobileDataIndicators(IconState statusIcon, IconState qsIcon, int statusType,
                int networkIcon, int volteIcon, int qsType, boolean activityIn, boolean activityOut,
                String typeContentDescription,String description, boolean isWide, int subId,
                boolean roaming) {
            if (qsIcon == null) {
                // Not data sim, don't display.
                Log.d(TAG, "setMobileDataIndicator qsIcon = null, Not data sim, don't display");
                return;
            }
            mInfo.enabled = qsIcon.visible;
            //20190822 pjz add show signalicon stay with statusicon
            mInfo.mobileSignalIconId = /*qsIcon.icon*/statusIcon.icon;
            mInfo.signalContentDescription = qsIcon.contentDescription;
            mInfo.dataTypeIconId = qsType;
            mInfo.dataContentDescription = typeContentDescription;
            mInfo.activityIn = activityIn;
            mInfo.activityOut = activityOut;
            mInfo.enabledDesc = description;
            mInfo.isDataTypeIconWide = qsType != 0 && isWide;
            mInfo.roaming = roaming;
            Log.i("ccz", "qsIcon.. mobileSignalIconId=="+ mInfo.mobileSignalIconId);
            if (DBG) {
                Log.d(TAG, "setMobileDataIndicators info.enabled = " + mInfo.enabled +
                    " mInfo.mobileSignalIconId = " + mInfo.mobileSignalIconId +
                    " mInfo.signalContentDescription = " + mInfo.signalContentDescription +
                    " mInfo.dataTypeIconId = " + mInfo.dataTypeIconId +
                    " mInfo.dataContentDescription = " + mInfo.dataContentDescription +
                    " mInfo.activityIn = " + mInfo.activityIn +
                    " mInfo.activityOut = " + mInfo.activityOut +
                    " mInfo.enabledDesc = " + mInfo.enabledDesc +
                    " mInfo.isDataTypeIconWide = " + mInfo.isDataTypeIconWide +
                    " mInfo.roaming = " + mInfo.roaming);
            }
            refreshState(mInfo);
        }

        @Override
        public void setNoSims(boolean show, boolean simDetected) {
            mInfo.noSim = show;
            if (mInfo.noSim) {
                // Make sure signal gets cleared out when no sims.
                mInfo.mobileSignalIconId = 0;
                mInfo.dataTypeIconId = 0;
                // Show a No SIMs description to avoid emergency calls message.
                mInfo.enabled = true;
                mInfo.enabledDesc = mContext.getString(
                        R.string.keyguard_missing_sim_message_short);
                mInfo.signalContentDescription = mInfo.enabledDesc;
            }
            refreshState(mInfo);
        }

        @Override
        public void setIsAirplaneMode(IconState icon) {
            mInfo.airplaneModeEnabled = icon.visible;
            refreshState(mInfo);
        }

        @Override
        public void setMobileDataEnabled(boolean enabled) {
            mDetailAdapter.setMobileDataEnabled(enabled);
        }
    }

    static Intent getCellularSettingIntent(Context context) {
        // TODO(b/62349208): We should replace feature flag check below with data plans
        // availability check. If the data plans are available we display the data plans usage
        // summary otherwise we display data usage summary without data plans.
        boolean isDataPlanFeatureEnabled =
                SystemProperties.getBoolean(ENABLE_SETTINGS_DATA_PLAN, false /* default */);
        context.getPackageManager()
                .setComponentEnabledSetting(
                        DATA_PLAN_CELLULAR_COMPONENT,
                        isDataPlanFeatureEnabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                                : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP);
        context.getPackageManager()
                .setComponentEnabledSetting(
                        CELLULAR_SETTING_COMPONENT,
                        isDataPlanFeatureEnabled ? PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                                : PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                        PackageManager.DONT_KILL_APP);
        return isDataPlanFeatureEnabled ? DATA_PLAN_CELLULAR_SETTINGS : CELLULAR_SETTINGS;
    }

    private final class CellularDetailAdapter implements DetailAdapter {

        @Override
        public CharSequence getTitle() {
            return mContext.getString(R.string.quick_settings_cellular_detail_title);
        }

        @Override
        public Boolean getToggleState() {
            return mDataController.isMobileDataSupported()
                    ? mDataController.isMobileDataEnabled()
                    : null;
        }

        @Override
        public Intent getSettingsIntent() {
            return getCellularSettingIntent(mContext);
        }

        @Override
        public void setToggleState(boolean state) {
            MetricsLogger.action(mContext, MetricsEvent.QS_CELLULAR_TOGGLE, state);
            mDataController.setMobileDataEnabled(state);
            /// M: Disable other sub's data when enable default sub's data @{
            if (state) {
                mQuickSettingsPlugin.disableDataForOtherSubscriptions();
            }
            /// @}
        }

        @Override
        public int getMetricsCategory() {
            return MetricsEvent.QS_DATAUSAGEDETAIL;
        }

        @Override
        public View createDetailView(Context context, View convertView, ViewGroup parent) {
            final DataUsageDetailView v = (DataUsageDetailView) (convertView != null
                    ? convertView
                    : LayoutInflater.from(mContext).inflate(R.layout.data_usage, parent, false));
            final DataUsageController.DataUsageInfo info = mDataController.getDataUsageInfo();
            if (info == null) return v;
            v.bind(info);
            v.findViewById(R.id.roaming_text).setVisibility(mSignalCallback.mInfo.roaming
                    ? View.VISIBLE : View.INVISIBLE);
            return v;
        }

        public void setMobileDataEnabled(boolean enabled) {
            fireToggleStateChanged(enabled);
        }
    }
}
