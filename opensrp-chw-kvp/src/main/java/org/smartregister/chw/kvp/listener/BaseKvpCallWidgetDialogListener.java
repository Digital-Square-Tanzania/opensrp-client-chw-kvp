package org.smartregister.chw.kvp.listener;


import android.view.View;

import org.smartregister.chw.kvp.fragment.BaseKvpCallDialogFragment;
import org.smartregister.chw.kvp.util.KvpUtil;
import org.smartregister.chw.kvp.R;

import timber.log.Timber;

public class BaseKvpCallWidgetDialogListener implements View.OnClickListener {

    private BaseKvpCallDialogFragment callDialogFragment;

    public BaseKvpCallWidgetDialogListener(BaseKvpCallDialogFragment dialogFragment) {
        callDialogFragment = dialogFragment;
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.kvp_call_close) {
            callDialogFragment.dismiss();
        } else if (i == R.id.kvp_call_head_phone) {
            try {
                String phoneNumber = (String) v.getTag();
                KvpUtil.launchDialer(callDialogFragment.getActivity(), callDialogFragment, phoneNumber);
                callDialogFragment.dismiss();
            } catch (Exception e) {
                Timber.e(e);
            }
        } else if (i == R.id.call_kvp_client_phone) {
            try {
                String phoneNumber = (String) v.getTag();
                KvpUtil.launchDialer(callDialogFragment.getActivity(), callDialogFragment, phoneNumber);
                callDialogFragment.dismiss();
            } catch (Exception e) {
                Timber.e(e);
            }
        }
    }
}
