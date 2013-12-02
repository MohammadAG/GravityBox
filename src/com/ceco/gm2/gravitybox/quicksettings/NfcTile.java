package com.ceco.gm2.gravitybox.quicksettings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.ceco.gm2.gravitybox.R;

import de.robv.android.xposed.XposedHelpers;

public class NfcTile extends AQuickSettingsTile {

    private static final String INTENT_ADAPTER_STATE_CHANGED = "android.nfc.action.ADAPTER_STATE_CHANGED";
    private static final String INTENT_NFC_SETTINGS = "android.settings.NFC_SETTINGS";

    private static final int STATE_TURNING_ON = XposedHelpers.getStaticIntField(NfcAdapter.class, "STATE_TURNING_ON");
    private static final int STATE_ON = XposedHelpers.getStaticIntField(NfcAdapter.class, "STATE_ON");
    @SuppressWarnings("unused")
    private static final int STATE_TURNING_OFF = XposedHelpers.getStaticIntField(NfcAdapter.class, "STATE_TURNING_OFF");
    private static final int STATE_OFF = XposedHelpers.getStaticIntField(NfcAdapter.class, "STATE_OFF");
    private static final int NFC_ADAPTER_UNKNOWN = -100;

    @SuppressWarnings("unused")
	private static final String TAG = "GB:NfcTile";

    private static NfcAdapter mNfcAdapter = null;

    private BroadcastReceiver mNfcStateChangedReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (INTENT_ADAPTER_STATE_CHANGED.equals(intent.getAction())) {
                if (mNfcAdapter == null)
                    getNfcAdapter(true);
                updateTile(getNfcState());
            }
        }
    };

    private TextView mTextView;

    public NfcTile(Context context, Context gbContext, Object statusBar, Object panelBar) {
        super(context, gbContext, statusBar, panelBar);

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
        int state = getNfcState();
        /* For some reason, a switch-case way of handling this didn't work */

        if (mNfcAdapter == null)
            getNfcAdapter(true);

        if (mNfcAdapter == null) {
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
        int state = STATE_OFF;
        try {
            getNfcAdapter(false);
        } catch (UnsupportedOperationException e) {
            state = NFC_ADAPTER_UNKNOWN;
        }

        if (mNfcAdapter != null)
            state = (Integer) XposedHelpers.callMethod(mNfcAdapter, "getAdapterState");

        return state;
    }

    @Override
    protected void onTilePostCreate() {
        mContext.registerReceiver(mNfcStateChangedReceiver,
                new IntentFilter(INTENT_ADAPTER_STATE_CHANGED));

        super.onTilePostCreate();
    }

    private void getNfcAdapter(boolean suppressThrow) throws UnsupportedOperationException {
        Class<?> ServiceManager;
        try {
            ServiceManager = Class.forName("android.os.ServiceManager");
        } catch (ClassNotFoundException e1) {
            e1.printStackTrace();
            return;
        }
        
        if (mNfcAdapter == null && XposedHelpers.callStaticMethod(ServiceManager, "getService", "nfc") != null) {
            try {
                mNfcAdapter = NfcAdapter.getDefaultAdapter(mContext);
            } catch (UnsupportedOperationException e) {
                if (!suppressThrow)
                    throw e;
            }
        }
    }
}
