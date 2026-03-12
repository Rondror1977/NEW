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

        // Seul événement reçu via manifest (Android 8+)
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {

            // 1) Publier immédiatement l’état courant via BATTERY_CHANGED (sticky)
            String init = getCurrentChargingState(context);
            sendVelocityBarcodeIntent(context.getApplicationContext(), init);

            // 2) Enregistrer dynamiquement les receivers pour CHARGING / DISCHARGING
            registerChargingReceivers(context.getApplicationContext());
        }
    }

    /**
     * Enregistrement dynamique des broadcasts CHARGING / DISCHARGING
     * + écoute ACTION_BATTERY_CHANGED (sticky) pour mise à jour immédiate.
     */
    private void registerChargingReceivers(Context ctx) {
        IntentFilter f = new IntentFilter();
        f.addAction("android.os.action.CHARGING");       // Android moderne : entrée en charge
        f.addAction("android.os.action.DISCHARGING");    // Android moderne : sortie de charge
        f.addAction(Intent.ACTION_BATTERY_CHANGED);      // Sticky : état courant

        BroadcastReceiver r = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent i) {
                String action = i.getAction();
                String state = null;

                if ("android.os.action.CHARGING".equals(action)) {
                    state = "CHARGING";
                } else if ("android.os.action.DISCHARGING".equals(action)) {
                    state = "NOT_CHARGING";
                } else if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                    state = getCurrentChargingState(c);
                }

                if (state != null) {
                    sendVelocityBarcodeIntent(c.getApplicationContext(), state);
                }
            }
        };

        ctx.registerReceiver(r, f);
        Log.i(TAG, "Dynamic charging receivers registered");
    }

    /**
     * Lit l'état actuel via ACTION_BATTERY_CHANGED (sticky)
     */
    private String getCurrentChargingState(Context ctx) {
        try {
            Intent batt = ctx.registerReceiver(null,
                    new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

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

    /**
     * Envoi compatibilité Velocity (BARCODE intent)
     */
    private void sendVelocityBarcodeIntent(Context ctx, String state) {
        try {
            Intent v = new Intent("com.wavelink.intent.action.BARCODE");
            v.addCategory(Intent.CATEGORY_DEFAULT);

            // Champs Ivanti classiques
            v.putExtra("com.wavelink.extra.symbology_type", "POWER_EVENT");
            v.putExtra("com.wavelink.extra.data_string", state);

            // Compatibilité alternative (certaines versions Velocity / VScript)
            v.putExtra("scan.symbology_type", "POWER_EVENT");
            v.putExtra("scan.data_string", state);

            v.setPackage("com.wavelink.velocity");
            ctx.sendBroadcast(v);

            Log.i(TAG, "Sent Velocity power state: " + state);
        } catch (Throwable t) {
            Log.e(TAG, "Failed sending Velocity intent", t);
        }
    }
}
