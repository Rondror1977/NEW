
package com.newlog.powerbridge;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.util.Log;

public class PowerEventBridgeReceiver extends BroadcastReceiver {
    private static final String TAG = "PowerBridgeRx";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        final String action = intent.getAction();
        String state = null;

        if (Intent.ACTION_POWER_CONNECTED.equals(action)) {
            state = "CHARGING";
        } else if (Intent.ACTION_POWER_DISCONNECTED.equals(action)) {
            state = "NOT_CHARGING";
        } else if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            state = getCurrentChargingState(context);
        }

        if (state != null) {
            sendVelocityBarcodeIntent(context.getApplicationContext(), state);
        }
    }

    private String getCurrentChargingState(Context ctx) {
        try {
            Intent batt = ctx.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (batt != null) {
                int plugged = batt.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
                boolean charging = plugged == BatteryManager.BATTERY_PLUGGED_AC
                        || plugged == BatteryManager.BATTERY_PLUGGED_USB
                        || plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS;
                return charging ? "CHARGING" : "NOT_CHARGING";
            }
        } catch (Throwable t) {
            Log.w(TAG, "Unable to read battery state", t);
        }
        return "UNKNOWN";
    }

    private void sendVelocityBarcodeIntent(Context ctx, String state) {
        try {
            Intent v = new Intent("com.wavelink.intent.action.BARCODE");
            v.addCategory(Intent.CATEGORY_DEFAULT);
            v.putExtra("com.wavelink.extra.symbology_type", "POWER_EVENT");
            v.putExtra("com.wavelink.extra.data_string", state);
            v.setPackage("com.wavelink.velocity");
            ctx.sendBroadcast(v);
            Log.i(TAG, "Sent Velocity power state: " + state);
        } catch (Throwable t) {
            Log.e(TAG, "Failed sending Velocity intent", t);
        }
    }
}
