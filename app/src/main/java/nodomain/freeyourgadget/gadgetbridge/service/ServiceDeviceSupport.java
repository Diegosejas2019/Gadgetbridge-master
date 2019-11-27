/*  Copyright (C) 2015-2019 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti, José Rebelo, Julien Pivotto, Kasha, Sebastian Kranz, Steffen
    Liebergeld

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiService;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.VibrationProfile;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.model.CalendarEventSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CannedMessagesSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BtLEAction;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.common.SimpleNotification;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.DEFAULT_VALUE_VIBRATION_COUNT;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.DEFAULT_VALUE_VIBRATION_PROFILE;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.VIBRATION_COUNT;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.VIBRATION_PROFILE;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.getNotificationPrefIntValue;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.getNotificationPrefStringValue;

/**
 * Wraps another device support instance and supports busy-checking and throttling of events.
 */
public class ServiceDeviceSupport extends AbstractBTLEDeviceSupport implements DeviceSupport  {
    private static int currentButtonActionId = 0;
    private static int currentButtonPressCount = 0;
    private static long currentButtonPressTime = 0;
    private static long currentButtonTimerActivationTime = 0;
    enum Flags {
        THROTTLING,
        BUSY_CHECKING,
    }

    private static final Logger LOG = LoggerFactory.getLogger(ServiceDeviceSupport.class);

    private static final long THROTTLING_THRESHOLD = 1000; // throttle multiple events in between one second
    private final DeviceSupport delegate;

    private long lastNotificationTime = 0;
    private String lastNotificationKind;
    private final EnumSet<Flags> flags;

    public ServiceDeviceSupport(DeviceSupport delegate, EnumSet<Flags> flags) {
        super();
        this.delegate = delegate;
        this.flags = flags;
    }

    @Override
    public void setContext(GBDevice gbDevice, BluetoothAdapter btAdapter, Context context) {
        delegate.setContext(gbDevice, btAdapter, context);
    }

    @Override
    public boolean isConnected() {
        return delegate.isConnected();
    }

    @Override
    public boolean connectFirstTime() {
        return delegate.connectFirstTime();
    }

    @Override
    public boolean connect() {
        return delegate.connect();
    }

    @Override
    public void setAutoReconnect(boolean enable) {
        delegate.setAutoReconnect(enable);
    }

    @Override
    public boolean getAutoReconnect() {
        return delegate.getAutoReconnect();
    }

    @Override
    public void dispose() {
        delegate.dispose();
    }

    @Override
    public GBDevice getDevice() {
        return delegate.getDevice();
    }

    @Override
    public BluetoothAdapter getBluetoothAdapter() {
        return delegate.getBluetoothAdapter();
    }

    @Override
    public Context getContext() {
        return delegate.getContext();
    }

    @Override
    public String customStringFilter(String inputString) {
        return delegate.customStringFilter(inputString);
    }

    @Override
    public boolean useAutoConnect() {
        return delegate.useAutoConnect();
    }

    private boolean checkBusy(String notificationKind) {
        if (!flags.contains(Flags.BUSY_CHECKING)) {
            return false;
        }
        if (getDevice().isBusy()) {
            LOG.info("Ignoring " + notificationKind + " because we're busy with " + getDevice().getBusyTask());
            return true;
        }
        return false;
    }

    private boolean checkThrottle(String notificationKind) {
        if (!flags.contains(Flags.THROTTLING)) {
            return false;
        }
        long currentTime = System.currentTimeMillis();
        if ((currentTime - lastNotificationTime) < THROTTLING_THRESHOLD) {
            if (notificationKind != null && notificationKind.equals(lastNotificationKind)) {
                LOG.info("Ignoring " + notificationKind + " because of throttling threshold reached");
                return true;
            }
        }
        lastNotificationTime = currentTime;
        lastNotificationKind = notificationKind;
        return false;
    }

    @Override
    public void onNotification(NotificationSpec notificationSpec) {
        if (checkBusy("generic notification") || checkThrottle("generic notification")) {
            return;
        }
        delegate.onNotification(notificationSpec);
    }

    @Override
    public void onDeleteNotification(int id) {
        delegate.onDeleteNotification(id);
    }

    @Override
    public void onSetTime() {
        if (checkBusy("set time") || checkThrottle("set time")) {
            return;
        }
        delegate.onSetTime();
    }

    // No throttling for the other events

    @Override
    public void onSetCallState(CallSpec callSpec) {
        if (checkBusy("set call state")) {
            return;
        }
        delegate.onSetCallState(callSpec);
    }

    @Override
    public void onSetCannedMessages(CannedMessagesSpec cannedMessagesSpec) {
        if (checkBusy("set canned messages")) {
            return;
        }
        delegate.onSetCannedMessages(cannedMessagesSpec);
    }

    @Override
    public void onSetMusicState(MusicStateSpec stateSpec) {
        if (checkBusy("set music state")) {
            return;
        }
        delegate.onSetMusicState(stateSpec);
    }

    @Override
    public void onSetMusicInfo(MusicSpec musicSpec) {
        if (checkBusy("set music info")) {
            return;
        }
        delegate.onSetMusicInfo(musicSpec);
    }

    @Override
    public void onInstallApp(Uri uri) {
        if (checkBusy("install app")) {
            return;
        }
        delegate.onInstallApp(uri);
    }

    @Override
    public void onAppInfoReq() {
        if (checkBusy("app info request")) {
            return;
        }
        delegate.onAppInfoReq();
    }

    @Override
    public void onAppStart(UUID uuid, boolean start) {
        if (checkBusy("app start")) {
            return;
        }
        delegate.onAppStart(uuid, start);
    }

    @Override
    public void onAppDelete(UUID uuid) {
        if (checkBusy("app delete")) {
            return;
        }
        delegate.onAppDelete(uuid);
    }

    @Override
    public void onAppConfiguration(UUID uuid, String config, Integer id) {
        if (checkBusy("app configuration")) {
            return;
        }
        delegate.onAppConfiguration(uuid, config, id);
    }

    @Override
    public void onAppReorder(UUID[] uuids) {
        if (checkBusy("app reorder")) {
            return;
        }
        delegate.onAppReorder(uuids);
    }

    @Override
    public void onFetchRecordedData(int dataTypes) {
        if (checkBusy("fetch activity data")) {
            return;
        }
        delegate.onFetchRecordedData(dataTypes);
    }

    @Override
    public void onReset(int flags) {
        if (checkBusy("reset")) {
            return;
        }
        delegate.onReset(flags);
    }

    @Override
    public void onHeartRateTest() {
        if (checkBusy("heartrate")) {
            return;
        }
        delegate.onHeartRateTest();
    }

    @Override
    public void onFindDevice(boolean start) {
        if (checkBusy("find device")) {
            return;
        }
        delegate.onFindDevice(start);
    }

    @Override
    public void onSetConstantVibration(int intensity) {
        if (checkBusy("set constant vibration")) {
            return;
        }
        delegate.onSetConstantVibration(intensity);
    }

    @Override
    public void onScreenshotReq() {
        if (checkBusy("request screenshot")) {
            return;
        }
        delegate.onScreenshotReq();
    }

    @Override
    public void onSetAlarms(ArrayList<? extends Alarm> alarms) {
        if (checkBusy("set alarms")) {
            return;
        }
        delegate.onSetAlarms(alarms);
    }

    @Override
    public void onEnableRealtimeSteps(boolean enable) {
        if (checkBusy("enable realtime steps: " + enable)) {
            return;
        }
        delegate.onEnableRealtimeSteps(enable);
    }

    @Override
    public void onEnableHeartRateSleepSupport(boolean enable) {
        if (checkBusy("enable heart rate sleep support: " + enable)) {
            return;
        }
        delegate.onEnableHeartRateSleepSupport(enable);
    }

    @Override
    public void onSetHeartRateMeasurementInterval(int seconds) {
        if (checkBusy("set heart rate measurement interval: " + seconds + "s")) {
            return;
        }
        delegate.onSetHeartRateMeasurementInterval(seconds);
    }

    @Override
    public void onEnableRealtimeHeartRateMeasurement(boolean enable) {
        if (checkBusy("enable realtime heart rate measurement: " + enable)) {
            return;
        }
        delegate.onEnableRealtimeHeartRateMeasurement(enable);
    }

    @Override
    public void onAddCalendarEvent(CalendarEventSpec calendarEventSpec) {
        if (checkBusy("add calendar event")) {
            return;
        }
        delegate.onAddCalendarEvent(calendarEventSpec);
    }

    @Override
    public void onDeleteCalendarEvent(byte type, long id) {
        if (checkBusy("delete calendar event")) {
            return;
        }
        delegate.onDeleteCalendarEvent(type, id);
    }

    @Override
    public void onSendConfiguration(String config) {
        if (checkBusy("send configuration: " + config)) {
            return;
        }
        delegate.onSendConfiguration(config);
    }

    @Override
    public void onReadConfiguration(String config) {
        if (checkBusy("read configuration: " + config)) {
            return;
        }
        delegate.onReadConfiguration(config);
    }

    @Override
    public void onTestNewFunction() {
        if (checkBusy("test new function event")) {
            return;
        }
        delegate.onTestNewFunction();
    }

    @Override
    public void onSendWeather(WeatherSpec weatherSpec) {
        if (checkBusy("send weather event")) {
            return;
        }
        delegate.onSendWeather(weatherSpec);
    }

    @Override
    public void onSetFmFrequency(float frequency) {
        if (checkBusy("set frequency event")) {
            return;
        }
        delegate.onSetFmFrequency(frequency);
    }

    @Override
    public void onSetLedColor(int color) {
        if (checkBusy("set led color event")) {
            return;
        }
        delegate.onSetLedColor(color);
    }

    @Override
    public void handleButtonEventNew() {
        Prefs prefs = GBApplication.getPrefs();
        if (!prefs.getBoolean(MiBandConst.PREF_MIBAND_BUTTON_ACTION_ENABLE, false)) {
            return;
        }

        int buttonPressMaxDelay = prefs.getInt(MiBandConst.PREF_MIBAND_BUTTON_PRESS_MAX_DELAY, 2000);
        int buttonActionDelay = prefs.getInt(MiBandConst.PREF_MIBAND_BUTTON_ACTION_DELAY, 0);
        int requiredButtonPressCount = prefs.getInt(MiBandConst.PREF_MIBAND_BUTTON_PRESS_COUNT, 0);

        if (requiredButtonPressCount > 0) {
            long timeSinceLastPress = System.currentTimeMillis() - currentButtonPressTime;

            if ((currentButtonPressTime == 0) || (timeSinceLastPress < buttonPressMaxDelay)) {
                currentButtonPressCount++;
            }
            else {
                currentButtonPressCount = 1;
                currentButtonActionId = 0;
            }

            currentButtonPressTime = System.currentTimeMillis();
            if (currentButtonPressCount == requiredButtonPressCount) {
                currentButtonTimerActivationTime = currentButtonPressTime;
                if (buttonActionDelay > 0) {
                    LOG.info("Activating timer");
                    final Timer buttonActionTimer = new Timer("Mi Band Button Action Timer");
                    buttonActionTimer.scheduleAtFixedRate(new TimerTask() {
                        @Override
                        public void run() {
                            runButtonAction();
                            buttonActionTimer.cancel();
                        }
                    }, buttonActionDelay, buttonActionDelay);
                }
                else {
                    LOG.info("Activating button action");
                    runButtonAction();
                }
                currentButtonActionId++;
                currentButtonPressCount = 0;
            }
        }
    }

    public void runButtonAction() {
        Prefs prefs = GBApplication.getPrefs();

        if (currentButtonTimerActivationTime != currentButtonPressTime) {
            return;
        }

        String requiredButtonPressMessage = prefs.getString(MiBandConst.PREF_MIBAND_BUTTON_PRESS_BROADCAST,
                this.getContext().getString(R.string.mi2_prefs_button_press_broadcast_default_value));

        Intent in = new Intent();
        in.setAction(requiredButtonPressMessage);
        in.putExtra("button_id", currentButtonActionId);
        LOG.info("Sending " + requiredButtonPressMessage + " with button_id " + currentButtonActionId);
        this.getContext().getApplicationContext().sendBroadcast(in);
        if (prefs.getBoolean(MiBandConst.PREF_MIBAND_BUTTON_ACTION_VIBRATE, false)) {
            performPreferredNotification(null, null, null, HuamiService.ALERT_LEVEL_VIBRATE_ONLY, null);
        }

        currentButtonActionId = 0;

        currentButtonPressCount = 0;
        currentButtonPressTime = System.currentTimeMillis();
    }

    private short getPreferredVibrateCount(String notificationOrigin, Prefs prefs) {
        return (short) Math.min(Short.MAX_VALUE, getNotificationPrefIntValue(VIBRATION_COUNT, notificationOrigin, prefs, DEFAULT_VALUE_VIBRATION_COUNT));
    }

    private VibrationProfile getPreferredVibrateProfile(String notificationOrigin, Prefs prefs, short repeat) {
        String profileId = getNotificationPrefStringValue(VIBRATION_PROFILE, notificationOrigin, prefs, DEFAULT_VALUE_VIBRATION_PROFILE);
        return VibrationProfile.getProfile(profileId, repeat);
    }

    private void performPreferredNotification(String task, String notificationOrigin, SimpleNotification simpleNotification, int alertLevel, BtLEAction extraAction) {
        try {
            TransactionBuilder builder = performInitialized(task);
            Prefs prefs = GBApplication.getPrefs();
            short vibrateTimes = getPreferredVibrateCount(notificationOrigin, prefs);
            VibrationProfile profile = getPreferredVibrateProfile(notificationOrigin, prefs, vibrateTimes);
            profile.setAlertLevel(alertLevel);

           // getNotificationStrategy().sendCustomNotification(profile, simpleNotification, 0, 0, 0, 0, extraAction, builder);

            builder.queue(getQueue());
        } catch (IOException ex) {
            LOG.error("Unable to send notification to device", ex);
        }
    }


}
