package com.ceco.gm2.gravitybox.quicksettings;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.ceco.gm2.gravitybox.R;

public class NfcTile extends AQuickSettingsTile {

    private static final boolean DEBUG = true;

    private static final String INTENT_ADAPTER_STATE_CHANGED = "android.nfc.action.ADAPTER_STATE_CHANGED";
    private static final String INTENT_NFC_SETTINGS = "android.settings.NFC_SETTINGS";

    private static final int STATE_TURNING_ON = XposedHelpers.getStaticIntField(NfcAdapter.class, "STATE_TURNING_ON");
    private static final int STATE_ON = XposedHelpers.getStaticIntField(NfcAdapter.class, "STATE_ON");
    @SuppressWarnings("unused")
    private static final int STATE_TURNING_OFF = XposedHelpers.getStaticIntField(NfcAdapter.class, "STATE_TURNING_OFF");
    private static final int STATE_OFF = XposedHelpers.getStaticIntField(NfcAdapter.class, "STATE_OFF");
    private static final int NFC_ADAPTER_UNKNOWN = -100;

    private static final String TAG = "GB:NfcTile";

    private static NfcAdapter mNfcAdapter = null;

    /* TODO: Remove log lines, the way we build the Strings uses operators and such */
    private void log(String message) {
        if (!DEBUG) return;
        XposedBridge.log(TAG + ": " + message);
    }

    private BroadcastReceiver mNfcStateChangedReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (INTENT_ADAPTER_STATE_CHANGED.equals(intent.getAction())) {
                if (mNfcAdapter == null)
                    tryToGetNfcAdapter(true);
                updateTile(getNfcState());
            }
        }
    };

    private TextView mTextView;

    public NfcTile(Context context, Context gbContext, Object statusBar, Object panelBar) {
        super(context, gbContext, statusBar, panelBar);
        log("Initializing NFC tile");

        mOnClick = new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                toggleState();
            }
        };

        mOnLongClick = new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                Intent intent = new Intent(INTENT_NFC_SETTINGS);
                intent.addCategory(Intent.CATEGORY_DEFAULT);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                return true;
            }
        };
    }

    private void toggleState() {
        log("toggleState()");
        int state = getNfcState();
        /* For some reason, a switch-case way of handling this didn't work */

        if (mNfcAdapter == null)
            tryToGetNfcAdapter(true);

        if (mNfcAdapter == null) {
            log("toggleState(): mNfcAdapter is null, returning");
            return;
        }

        if (state == STATE_TURNING_ON || state == STATE_ON)
            XposedHelpers.callMethod(mNfcAdapter, "disable");
        else
            XposedHelpers.callMethod(mNfcAdapter, "enable");

        updateTile();
    }

    @Override
    protected void onTileCreate() {
        LayoutInflater inflater = LayoutInflater.from(mGbContext);
        inflater.inflate(R.layout.quick_settings_tile_nfc, mTile);
        mTextView = (TextView) mTile.findViewById(R.id.nfc_tileview);

        mTextView.setText(mLabel);
        mTextView.setCompoundDrawablesWithIntrinsicBounds(0, mDrawableId, 0, 0);
    }

    @Override
    protected void updateTile() {
        updateTile(getNfcState());
    }

    private void updateTile(int state) {
        if (state == STATE_TURNING_ON || state == STATE_ON) {
            mDrawableId = R.drawable.ic_qs_nfc_on;
            mLabel = mGbContext.getString(R.string.quick_settings_nfc);
        } else {
            mDrawableId = R.drawable.ic_qs_nfc_off;
            mLabel = mGbContext.getString(R.string.quick_settings_nfc_off);
        }

        if (mTextView != null) {
            mTextView.setText(mLabel);
            mTextView.setCompoundDrawablesWithIntrinsicBounds(0, mDrawableId, 0, 0);
        }
    }

    @Override
    public void updateResources() {
        updateTile(getNfcState());
    }

    private int getNfcState() {
        log("getNfcState()");
        int state = STATE_OFF;
        try {
            tryToGetNfcAdapter(false);
        } catch (UnsupportedOperationException e) {
            state = NFC_ADAPTER_UNKNOWN;
        }

        if (mNfcAdapter != null)
            state = (Integer) XposedHelpers.callMethod(mNfcAdapter, "getAdapterState");
        else
            log("getNfcState(): mNfcAdapter is null");

        log("getNfcAdapter(): state is " + String.valueOf(state));

        return state;
    }

    @Override
    protected void onTilePostCreate() {
        mContext.registerReceiver(mNfcStateChangedReceiver,
                new IntentFilter(INTENT_ADAPTER_STATE_CHANGED));

        super.onTilePostCreate();
    }

    private void tryToGetNfcAdapter(boolean suppressThrow) throws UnsupportedOperationException {
        log("tryToGetNfcAdapter()");
        if (mNfcAdapter == null) {
            log("tryToGetNfcAdapter: adapter was indeed null");
            try {
                mNfcAdapter = (NfcAdapter) XposedHelpers.callStaticMethod(NfcAdapter.class, "getNfcAdapter", mContext);
                log("tryToGetNfcAdapter: we got an adapter! Is it null? " + String.valueOf(mNfcAdapter == null));
            } catch (UnsupportedOperationException e) {
                if (!suppressThrow)
                    throw e;
            }
        } else {
            log("tryToGetNfcAdapter(): We already have an NFC adapter, returning");
        }
    }
}
