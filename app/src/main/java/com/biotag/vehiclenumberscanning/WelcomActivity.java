package com.biotag.vehiclenumberscanning;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.biotag.vehiclenumberscanning.NFC.CardInfo;
import com.biotag.vehiclenumberscanning.NFC.Constants;
import com.biotag.vehiclenumberscanning.NFC.NFCTool;
import com.biotag.vehiclenumberscanning.bean.AppVersionBean;
import com.biotag.vehiclenumberscanning.view.RadiationView;

import java.io.File;
import java.lang.ref.WeakReference;

import static com.biotag.vehiclenumberscanning.AppDownloadService.VEHICLESCA_APK;

public class WelcomActivity extends BaseNFCActivity {

    private ImageView VS;
    private RelativeLayout rl_staffinfo;
    private TextView tv_name, tv_staffno, tv_tips;
    private FrameLayout fl_anim;
    private static final int MSG_NFCREAD_FAIL = 1;
    private static final int MSG_NFCREAD_SUCCESS = 2;
    private static final int MSG_NFCCARD_WRONGTYPE = 3;
    private ImageView iv_rotate;
    private RadiationView rv;
    private Button btn_login;
    private Tag detecttag;
    private String appNo, appDownloadUrl, newestVersion;
    public static final int NO_NEED = 10;
    public static final int NEED = 11;
    private Context context = this;
    private String areano;

    static class NFCHandler extends Handler {

        private WeakReference<WelcomActivity> welcomActivityWeakReference;

        public NFCHandler(WelcomActivity welcomActivity) {
            welcomActivityWeakReference = new WeakReference<WelcomActivity>(welcomActivity);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            WelcomActivity welcomActivity = welcomActivityWeakReference.get();
            if (welcomActivity != null) {
                switch (msg.what) {
                    case MSG_NFCREAD_FAIL:
                        Toast.makeText(welcomActivity, "读取证件信息失败，再试一次吧 ！", Toast.LENGTH_SHORT).show();
                        break;
                    case MSG_NFCREAD_SUCCESS:
                        CardInfo cardInfo = (CardInfo) msg.obj;
                        welcomActivity.fl_anim.setVisibility(View.INVISIBLE);
                        welcomActivity.tv_tips.setVisibility(View.INVISIBLE);
                        welcomActivity.rl_staffinfo.setVisibility(View.VISIBLE);
                        welcomActivity.tv_name.setText(cardInfo.getStaffName());
                        welcomActivity.tv_staffno.setText(cardInfo.getIdCard());
                        welcomActivity.areano = cardInfo.getAreaNo();
                        break;
                    case MSG_NFCCARD_WRONGTYPE:
                        Toast.makeText(welcomActivity, "非工作证", Toast.LENGTH_SHORT).show();
                        break;
                    case NO_NEED:
                        break;
                    case NEED:
                        if (NetCheckUtil.getNetworkType(welcomActivity) == NetCheckUtil.NETTYPE_WIFI) {
                            welcomActivity.appDownloadUrl = SharedPreferencesUtils.getString(welcomActivity, "appdownloadurl", "");
                            Intent intent = new Intent(welcomActivity, AppDownloadService.class);
//                            intent.putExtra("appurl", welcomActivity.appDownloadUrl);
                            intent.putExtra("newestVersion", welcomActivity.newestVersion);
                            welcomActivity.startService(intent);
                            Log.i(Constants.TAG, "service 启动了");
                        }
                        break;


                }
            }
        }
    }


    private NFCHandler nfcHandler = new NFCHandler(this);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_welcom);
        Log.i(Constants.TAG, "来到welcome");
        try {
            PackageManager pm = getPackageManager();
            PackageInfo pi = pm.getPackageInfo(getPackageName(), 0);
            appNo = pi.versionName;
            Log.i(Constants.TAG, "加载welcome 拿到的appno is" + appNo);
        } catch (Exception e) {
            e.printStackTrace();

        }

        appDownloadUrl = SharedPreferencesUtils.getString(this, "appdownloadurl", "");
        //Log.i(Constants.TAG,"appDownloadUrl is "+appDownloadUrl);
        Log.i(Constants.TAG, "开始检测新版本");

        initView();

        checkNewAppVersion();
    }

    private void checkNewAppVersion() {
        ThreadManager.getInstance().execute(new Runnable() {
            @Override
            public void run() {
                String requl = Constants.APP_VERSION;
                AppVersionBean avb = OkhttpPlusUtil.getInstance().get(requl, AppVersionBean.class);
                if (avb != null && avb.isIsSuccess()) {
//                    String versionurl = avb.getValues();
//                    VersionUrlBean vub = new Gson().fromJson(versionurl,VersionUrlBean.class);


                    if (!appNo.equals(avb.getValues())) {
                        newestVersion = avb.getValues();
                        nfcHandler.sendEmptyMessage(NEED);
                    } else {
                        nfcHandler.sendEmptyMessage(NO_NEED);
                    }
                }
            }
        });
    }

    private void initView() {
        btn_login = (Button) findViewById(R.id.btn_login);
        btn_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(WelcomActivity.this, SettingDistrictActivity.class);
                intent.putExtra("areano", areano);
                Log.i("tms", "areano is " + areano);
                startActivity(intent);
                finish();
            }
        });
        VS = (ImageView) findViewById(R.id.VS);
        rl_staffinfo = (RelativeLayout) findViewById(R.id.rl_staffinfo);
        tv_name = (TextView) findViewById(R.id.tv_name);
        tv_staffno = (TextView) findViewById(R.id.tv_staffno);
        tv_tips = (TextView) findViewById(R.id.tv_tips);
        fl_anim = (FrameLayout) findViewById(R.id.fl_anim);
        iv_rotate = (ImageView) findViewById(R.id.iv_rotate);
        rv = (RadiationView) findViewById(R.id.rv);
        rv.setMinRadius(70);
        rv.startRadiate();
        Animation anim = AnimationUtils.loadAnimation(this, R.anim.rotate_circle_anim);
        iv_rotate.startAnimation(anim);
    }


    protected void hideInput(View view) {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        detecttag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

        if (detecttag != null) {
            String[] techList = detecttag.getTechList();
            Log.i("tms", "onNewIntent: techList length = " + techList.length);
            for (int i = 0; i < techList.length; i++) {
                Log.i("tms", "onNewIntent: techList[" + i + "] = " + techList[i]);
            }
        }
        ThreadManager.getInstance().execute(new Runnable() {
            @Override
            public void run() {
                if (detecttag == null) {
                    nfcHandler.sendEmptyMessage(MSG_NFCREAD_FAIL);
                    return;
                }
                NFCTool nfcToolread = new NFCTool();
                CardInfo cardInfo = nfcToolread.readTag(detecttag);
                nfcHandler.removeCallbacksAndMessages(null);
                if (cardInfo == null) {
                    nfcHandler.sendEmptyMessage(MSG_NFCREAD_FAIL);
                } else {
                    Log.i(Constants.TAG, "run: cardinfo = " + cardInfo.toString());
                    if (cardInfo.getCardType() == Constants.CHIP_EMPLOYEECARD) {
                        Message msg = nfcHandler.obtainMessage(MSG_NFCREAD_SUCCESS, cardInfo);
                        nfcHandler.sendMessage(msg);
                    } else {
                        Message msg = nfcHandler.obtainMessage(MSG_NFCCARD_WRONGTYPE, cardInfo);
                        nfcHandler.sendMessage(msg);
                    }

                }
            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Intent intent = new Intent(this, AppDownloadService.class);
        stopService(intent);
        nfcHandler.removeCallbacksAndMessages(null);
    }


    @Override
    protected void onResume() {
        super.onResume();
        String apkstatus = SharedPreferencesUtils.getString(context, "apkstatus", "0");
        if (apkstatus.equals("1")) {
            Intent intent = new Intent(context, AppDownloadService.class);
            stopService(intent);
            AlertDialog ab = null;
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Update tip");
            builder.setMessage("The new version has been downloaded in wifi state,restart the app now ?");
            builder.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    SharedPreferencesUtils.saveString(context, "apkstatus", "0");
                    Intent intent1 = new Intent();
                    intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent1.setAction(Intent.ACTION_VIEW);
                    intent1.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent1.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    File apkfile = new File(Environment.getExternalStorageDirectory() + "/VehicleScanning/", VEHICLESCA_APK);
                    intent1.setDataAndType(FileUtils.getUriForFile(context, apkfile), "application/vnd.android.package-archive");
                    startActivity(intent1);
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            });
            ab = builder.create();
            ab.show();

        }
    }
}
