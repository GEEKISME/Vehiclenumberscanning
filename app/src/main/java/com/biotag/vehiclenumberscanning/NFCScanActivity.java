package com.biotag.vehiclenumberscanning;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
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
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.biotag.vehiclenumberscanning.NFC.CardInfo;
import com.biotag.vehiclenumberscanning.NFC.Constants;
import com.biotag.vehiclenumberscanning.NFC.NFCTool;
import com.biotag.vehiclenumberscanning.NFC.Utils;
import com.biotag.vehiclenumberscanning.bean.IsCancelBean;
import com.biotag.vehiclenumberscanning.view.RadiationView;
import com.google.gson.Gson;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class NFCScanActivity extends BaseNFCActivity implements View.OnClickListener {
    private final int MSG_NFCREAD_FAIL = 1;
    private final int MSG_NFCREAD_SUCCESS = 2;
    private final int MSG_DENNIED = 3;
	private final int MSG_UPLOADSUC = 4;
    private final int MSG_DENIED_FAULT_AREA = 31;
    private final int MSG_DENIED_HAS_ENTERED = 32;
    private final int MSG_DENIED_NOT_TICKET_EMPLOYEECARD = 33;
    private final String TAG = "Panpan";
    private NFCScanHandler mHandler = new NFCScanHandler();
    private ImageView iv_headImg, iv_rotate;
    private LinearLayout ll_credential, ll_access, ll_wrapinfo, ll_company, ll_name;
    //    private LinearLayout ll_lastmodified;
    private TextView tv_approved, tv_denied, tv_access,tv_access2, tv_credential, tv_company, tv_name;
    //    private TextView tv_lastmodified;
    private Button btn_back, btn_scanneron;
    private RadiationView rv;
    private FrameLayout fl_anim;
    private RelativeLayout rl, rl_wrong;
    private String staffphotourl = "";
    private File headImgBackUp = new File(Environment.getExternalStorageDirectory() + "/VehiclenumberscanningBackUp/");
    private RelativeLayout rl_title;
    private TextView tv_authority;
    private MydatabaseHelper dbHelper;
    private SQLiteDatabase db ;

    private String funcchosed = com.biotag.vehiclenumberscanning.SharedPreferencesUtils.getString(this,"funcchosed","");
    private Bitmap bitmap;
    private CardInfo cardInfo;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_nfcscan);
        initdb();
        initView();
    }

    private void initdb() {
        DataBaseUtils.importdatabasefromassets(this);
        dbHelper = MydatabaseHelper.getInstance(this);
        db = dbHelper.getReadableDatabase();
        Intent intent = new Intent(this, UpLoadingInandOutInfoService.class);
        startService(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Intent intent = new Intent(this,UpLoadingInandOutInfoService.class);
        stopService(intent);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

//        mTextView.setText("");
        Tag detectedTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

        if(detectedTag!=null){
            String[] techList = detectedTag.getTechList();
            Log.i(Constants.TAG, "onNewIntent: techList length = " + techList.length);
            for (int i = 0; i < techList.length; i++) {
                Log.i(Constants.TAG, "onNewIntent: techList[" + i + "] = " + techList[i]);
            }
        }

        NFCToolThread nfcToolThread = new NFCToolThread(detectedTag);
        nfcToolThread.start();
    }

    private void initView() {
        rl_title = (RelativeLayout)findViewById(R.id.rl_title);
        tv_authority = (TextView)findViewById(R.id.tv_authority);
        rl = (RelativeLayout) findViewById(R.id.rl);
        rl_wrong = (RelativeLayout) findViewById(R.id.rl_wrong);
        rl_wrong.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (rl_wrong.getVisibility() == View.VISIBLE) {
                    rl_wrong.setVisibility(View.GONE);
                }
            }
        });
        iv_headImg = (ImageView) findViewById(R.id.iv_headImg);
        tv_name = (TextView) findViewById(R.id.tv_name);
        ll_name = (LinearLayout) findViewById(R.id.ll_name);
        tv_company = (TextView) findViewById(R.id.tv_company);
        ll_company = (LinearLayout) findViewById(R.id.ll_company);
        tv_credential = (TextView) findViewById(R.id.tv_credential);
        ll_credential = (LinearLayout) findViewById(R.id.ll_credential);
        tv_access = (TextView) findViewById(R.id.tv_access);
        tv_access2 = (TextView) findViewById(R.id.tv_access2);
        ll_access = (LinearLayout) findViewById(R.id.ll_access);
//        tv_lastmodified = (TextView) findViewById(R.id.tv_lastmodified);
//        ll_lastmodified = (LinearLayout) findViewById(R.id.ll_lastmodified);
        ll_wrapinfo = (LinearLayout) findViewById(R.id.ll_wrapinfo);
        tv_approved = (TextView) findViewById(R.id.tv_approved);
        tv_denied = (TextView) findViewById(R.id.denied);
        btn_back = (Button) findViewById(R.id.btn_back);
        btn_scanneron = (Button) findViewById(R.id.btn_scanneron);
        fl_anim = (FrameLayout) findViewById(R.id.fl_anim);
        iv_rotate = (ImageView) findViewById(R.id.iv_rotate);
        rv = (RadiationView) findViewById(R.id.rv);
        rv.setMinRadius(70);
        rv.startRadiate();
        Animation anim = AnimationUtils.loadAnimation(this, R.anim.rotate_circle_anim);
        iv_rotate.startAnimation(anim);


        btn_back.setOnClickListener(this);
        btn_scanneron.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_back:
                finish();
                break;
            case R.id.btn_scanneron:
                iv_headImg.setImageDrawable(getResources().getDrawable(R.mipmap.userss));
                rl_title.setVisibility(View.GONE);
                rl.setVisibility(View.GONE);
                fl_anim.setVisibility(View.VISIBLE);
                break;
        }
    }


    //=================+++++++++++++++++++++++++++++++++++++++

    class NFCToolThread extends Thread {

        private Tag tag = null;

        public NFCToolThread(Tag tag) {
            this.tag = tag;
        }

        @Override
        public void run() {
            if (tag == null) {
                mHandler.sendEmptyMessage(MSG_NFCREAD_FAIL);
                return;
            }
            NFCTool nfcRead = new NFCTool();
            cardInfo = nfcRead.readTag(tag);
            if (cardInfo == null) {
                Log.i(TAG, "run: MSG_NFCREAD_FAIL");
                mHandler.sendEmptyMessage(MSG_NFCREAD_FAIL);
            } else {
                Log.i(TAG, "run: MSG_NFCREAD_SUCCESS");
                Log.i(TAG, "run: cardinfo = " + cardInfo.toString());
                cardInfo.printInfo();

                boolean writeResult = false;
                boolean isNotTicketEmployeeCard = false;
                boolean isDeniedWrongArea = false;
                boolean isDeniedHasEntered = false;

                String AreaNo = cardInfo.getAreaNo();
                String settingAreaNo = SharedPreferencesUtils.getString(NFCScanActivity.this,"dischosed","").split(" ")[0];
                String func = SharedPreferencesUtils.getString(NFCScanActivity.this,"funcchosed","");
                String AreaNow = cardInfo.getAreaNow();
                android.util.Log.i(TAG, "run: settingAreaNo = " + settingAreaNo);
                android.util.Log.i(TAG, "run: func = " + func);
                if (AreaNow != null && AreaNo != null && Utils.checkArea(AreaNo,settingAreaNo)) {
                    if(cardInfo.getCardType() == Constants.CHIP_TICKET){
                        if(cardInfo.getAreaNow().trim().equals("")){
                            writeResult = nfcRead.WriteAreaNow(settingAreaNo);
                            Log.i(Constants.TAG,"writeResult is "+writeResult);
                        }else{
                            isDeniedHasEntered = true;
                        }
                    }else if(cardInfo.getCardType() == Constants.CHIP_EMPLOYEECARD){
                        writeResult = true;
                    }else{
                        isNotTicketEmployeeCard = true;
                    }

                }else if (cardInfo.getGroupID() == Constants.GROUPID_ALLPASS){
                    writeResult = true;
                }else if (cardInfo.getCardType() != Constants.CHIP_EMPLOYEECARD &&
                        cardInfo.getCardType() != Constants.CHIP_TICKET){
                    isNotTicketEmployeeCard = true;
                }else{
                    isDeniedWrongArea = true;
                }
                if(writeResult){
                    mHandler.removeMessages(MSG_NFCREAD_SUCCESS);
                    Message msg = mHandler.obtainMessage(MSG_NFCREAD_SUCCESS, cardInfo);
                    mHandler.sendMessage(msg);
                }else if(isNotTicketEmployeeCard){
                    mHandler.removeMessages(MSG_DENIED_NOT_TICKET_EMPLOYEECARD);
                    Message msg = mHandler.obtainMessage(MSG_DENIED_NOT_TICKET_EMPLOYEECARD, cardInfo);
                    mHandler.sendMessage(msg);
                }else if(isDeniedWrongArea){
                    mHandler.removeMessages(MSG_DENIED_FAULT_AREA);
                    Message msg = mHandler.obtainMessage(MSG_DENIED_FAULT_AREA, cardInfo);
                    mHandler.sendMessage(msg);
                }else if(isDeniedHasEntered){
                    mHandler.removeMessages(MSG_DENIED_HAS_ENTERED);
                    Message msg = mHandler.obtainMessage(MSG_DENIED_HAS_ENTERED, cardInfo);
                    mHandler.sendMessage(msg);
                }
            }
        }
    }


    class NFCScanHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == MSG_NFCREAD_FAIL) {
                readCardFail();
            } else if (msg.what == MSG_NFCREAD_SUCCESS) {
                cardInfo = (CardInfo) msg.obj;
                try{
                    showCardInfo(cardInfo, true,msg.what);
                }catch (Exception e){
                    readCardFail();
                }

            } else if (msg.what == MSG_DENIED_FAULT_AREA ||
                    msg.what == MSG_DENIED_HAS_ENTERED ||
                    msg.what == MSG_DENIED_NOT_TICKET_EMPLOYEECARD) {
                cardInfo = (CardInfo) msg.obj;
                try{
                    showCardInfo(cardInfo, false,msg.what);
                }catch (Exception e){
                    readCardFail();
                }
            } else if (msg.what == MSG_UPLOADSUC){
//                Toast.makeText(NFCScanActivity.this, "信息上传成功", Toast.LENGTH_SHORT).show();
            } else if(msg.what == 90){
//                iv_headImg.setImageBitmap(bitmap);
                String imgUrl = cardInfo.getImageUrl();
                String tempurl = imgUrl.replaceAll("\\\\","/");
                File headImgLocal = new File(Environment.getExternalStorageDirectory()+"/VehiclenumberscanningBackUp/"+tempurl);

                //        File headImgLocal = new File(headImgBackUp, cardInfo.getID().trim() + ".jpg");
                if (!headImgLocal.isDirectory()&&headImgLocal.exists()) {
                    Bitmap bitmap = BitmapFactory.decodeFile(Environment.getExternalStorageDirectory() + "/VehiclenumberscanningBackUp/" + tempurl);
                    iv_headImg.setImageBitmap(bitmap);
                    Log.i("nsdc","get img local suc");
                }else {
                    iv_headImg.setImageResource(R.mipmap.userss);
                }
            }else if(msg.what == 900){
                iv_headImg.setImageBitmap(bitmap);
                Log.i("nsdc","get img net suc");
            }else if(msg.what==901){
                Log.i("ttttt","901");
                tv_approved.setText("APPROVED");
                tv_approved.setTextColor(getResources().getColor(R.color.deepgreen));
                tv_approved.setVisibility(View.VISIBLE);
            }else if(msg.what==91){
                Log.i("ttttt","91");
                if(cardInfo.getCardType()==Constants.CHIP_EMPLOYEECARD){
                    tv_approved.setText("该工作证已被注销");
                }else if(cardInfo.getCardType()==Constants.CHIP_TICKET){
                    tv_approved.setText("该门票已被注销");
                }
                tv_approved.setTextColor(Color.RED);
                tv_approved.setVisibility(View.VISIBLE);
            }
        }
    }

    private void readCardFail() {
//        tv_name.setText("");
//        tv_company.setText("");
//        tv_credential.setText("");
//        tv_access.setText("");
//
//        tv_approved.setText("READ FAILED");
//        tv_approved.setTextColor(Color.BLACK);
        rl_wrong.setVisibility(View.VISIBLE);

    }

    private void showCardInfo(final CardInfo cardInfo, boolean isAllow, int code) {
        Log.i(Constants.TAG, "cardinfo = " + cardInfo.toString());
        Log.i(Constants.TAG, "isallow shi " + isAllow);
        fl_anim.setVisibility(View.GONE);
        rl.setVisibility(View.VISIBLE);
        rl_title.setVisibility(View.VISIBLE);
        tv_authority.setText(SharedPreferencesUtils.getString(NFCScanActivity.this,"dischosed","ALL ACCESS"));
        tv_name.setText(cardInfo.getStaffName());
//        tv_company.setText(cardInfo.getCompanyName());
//        tv_credential.setText(cardInfo.getStaffNo());

        String companyName = cardInfo.getCompanyName();
        if(companyName == null || companyName.equals("")){
            ll_company.setVisibility(View.GONE);
        }else{
            ll_company.setVisibility(View.VISIBLE);
            tv_company.setText(companyName);
        }
        String staffNo = cardInfo.getStaffNo();
        if(staffNo == null || staffNo.equals("")){
            ll_credential.setVisibility(View.GONE);
        }else{
            ll_credential.setVisibility(View.VISIBLE);
            tv_credential.setText(staffNo);
        }

        String AreaNo = cardInfo.getAreaNo();
        AreaNo = Utils.convertAreaToDisplay(AreaNo);
//        if(AreaNo != null && AreaNo.equals("A")){
//            AreaNo = "H M B F S O";
//        }
        tv_access.setText(AreaNo);

        AreaNo = cardInfo.getAreaNo();
        AreaNo = Utils.dealAreaNo(AreaNo);
        tv_access2.setText(AreaNo);

        staffphotourl = Constants.URL_GETSTAFFPHOTO2;
        staffphotourl = replacememgtUrl(staffphotourl, "{path}", cardInfo.getImageUrl());
        Log.i(Constants.TAG, "url is" + staffphotourl);
        Log.i("nsdc","start get img");
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.i("nsdc","thread  .....");
                try {
                    bitmap = Picasso.with(NFCScanActivity.this).load(staffphotourl).get();
                    if(!(bitmap==null)){
                        mHandler.sendEmptyMessage(900);
                        Log.i("nsdc","img suc net");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    mHandler.sendEmptyMessage(90);
                    Log.i("nsdc","get img from local");

                }
            }
        }).start();


        //加载图片时首先去VictoriaSecretBackUp文件夹找是否有对应id的图片，如果没有的话再去网络上去下载
//        String imgUrl = cardInfo.getImageUrl();
//        String tempurl = imgUrl.replaceAll("\\\\","/");
//        File headImgLocal = new File(headImgBackUp,tempurl);
////        File headImgLocal = new File(headImgBackUp, cardInfo.getID().trim() + ".jpg");
//        if (headImgLocal.exists()) {
//            Bitmap bitmap = BitmapFactory.decodeFile(Environment.getExternalStorageDirectory() + "/VehiclenumberscanningBackUp/" + tempurl);
//            iv_headImg.setImageBitmap(bitmap);
//        } else {
//            staffphotourl = Constants.URL_GETSTAFFPHOTO2;
//            staffphotourl = replacememgtUrl(staffphotourl, "{path}", cardInfo.getImageUrl());
//            Log.i(Constants.TAG, "url is" + staffphotourl);
//            Picasso.with(NFCScanActivity.this).load(staffphotourl).placeholder(R.mipmap.userss).error(R.mipmap.userss).into(iv_headImg);staffphotourl = Constants.URL_GETSTAFFPHOTO2;
//
//        }
        //====================================
//        staffphotourl = Constants.URL_GETSTAFFPHOTO2;
//        staffphotourl = replacememgtUrl(staffphotourl, "{path}", cardInfo.getImageUrl());
//        Log.i(Constants.TAG, "url is" + staffphotourl);
//        Picasso.with(NFCScanActivity.this).load(staffphotourl).placeholder(R.mipmap.userss).error(R.mipmap.userss).into(iv_headImg);
//        if(iv_headImg.getDrawable().getCurrent().getConstantState().equals(getResources().getDrawable(R.mipmap.userss).getConstantState())&&headImgLocal.exists()){
//            Bitmap bitmap = BitmapFactory.decodeFile(Environment.getExternalStorageDirectory() + "/VehiclenumberscanningBackUp/" + tempurl);
//            iv_headImg.setImageBitmap(bitmap);
//        }else {
//
//        }



        tv_approved.setVisibility(View.VISIBLE);
        if (isAllow) {
            ThreadManager.getInstance().execute(new Runnable() {
                @Override
                public void run() {
                    String isCancelurl = Constants.ISCANCEL_URL;
                    isCancelurl = replacememgtUrl(isCancelurl,"{id}",cardInfo.getID());
                    isCancelurl = replacememgtUrl(isCancelurl,"{chip}",cardInfo.getCardID());
                    Log.i("ttttt","iscancelurl  is  "+isCancelurl);
                    OkHttpClient client = new OkHttpClient().newBuilder().connectTimeout(2000, TimeUnit.MILLISECONDS).readTimeout(3000,TimeUnit.MILLISECONDS).build();
                    Request request = new Request.Builder().url(isCancelurl).build();
                    try{
                        Response response = client.newCall(request).execute();
                        String s = response.body().string();
                        Log.i("ttttt",s);
                        IsCancelBean icb = new Gson().fromJson(s,IsCancelBean.class);
                        if(icb!=null&&icb.isIsSuccess()){
                            mHandler.sendEmptyMessage(901);
                        }else {
                            mHandler.sendEmptyMessage(91);
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                        mHandler.sendEmptyMessage(901);
                        Log.i("ttttt","catch error");
                    }
                }
            });
            tv_approved.setVisibility(View.VISIBLE);
            tv_denied.setVisibility(View.INVISIBLE);
        } else {
            tv_approved.setVisibility(View.INVISIBLE);
            tv_denied.setVisibility(View.VISIBLE);
            if(code == MSG_DENIED_FAULT_AREA){
                tv_denied.setText("Denied(区域错误)");
            }else if(code == MSG_DENIED_HAS_ENTERED){
                tv_denied.setText("Denied(失效)");
            }else if(code == MSG_DENIED_NOT_TICKET_EMPLOYEECARD){
                tv_denied.setText("Denied(不是门票或工作证)");
            } else {
                tv_denied.setText("Denied");
            }
        }
        final String staffid = cardInfo.getID();
        final String areano = SharedPreferencesUtils.getString(NFCScanActivity.this, "dischosed", "A");
          //1代表进入，0 代表出去
        String action_type ;
        if(funcchosed.equals("检票")){
            action_type = "1";
        }else {
            action_type = "0";
        }
        Cursor cursor = db.query("Inoutinfo",new String[]{"staffid"},"staffid = ?",new String[]{cardInfo.getID()},null,null,null);
        if(!cursor.moveToFirst()){
            long current = System.currentTimeMillis();
            SimpleDateFormat sds = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String currettime = sds.format(current);
            Log.i(Constants.TAG,"精确时间是 "+currettime);
            ContentValues values = new ContentValues();
            values.put("StaffID",cardInfo.getID());
            values.put("ChipCode",cardInfo.getIdCard());
            values.put("AreaNo",areano);
            values.put("Action_Type",action_type);
            values.put("ActionTime",currettime);
            db.insert("Inoutinfo",null,values);
        }else { //说明数据库里有此条目，那么说明这个人已经进入过了
            Log.i(Constants.TAG,"数据库中已有此条目");
        }


//        postInfo(staffid, areano, action_type);

    }

//    private void postInfo(String staffid, String areano, String action_type) {
//        JSONObject obj = new JSONObject();
//        try {
//            obj.put("StaffID", staffid);
//            obj.put("AreaNo", areano);
//            obj.put("Action_Type", action_type);
//            final String json = String.valueOf(obj);
//            ThreadManager.getInstance().execute(new Runnable() {
//                @Override
//                public void run() {
//                    AddInfoSucBean asb = OkhttpPlusUtil.getInstance().post(Constants.URL_POSTINOUT_EMPLOYCARD, json, AddInfoSucBean.class);
//                    if(asb!=null&&asb.isIsSuccess()){
//                        mHandler.sendEmptyMessage(MSG_UPLOADSUC);
//                    }
//                }
//            });
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//    }

    private String replacememgtUrl(String staffphotourl, String regex, String replacement) {
        int index = -1;
        StringBuffer buffer = new StringBuffer();
        while ((index = staffphotourl.indexOf(regex)) >= 0) {
            buffer.append(staffphotourl.substring(0, index));
            buffer.append(replacement);
            staffphotourl = staffphotourl.substring(index + regex.length());
        }
        buffer.append(staffphotourl);
        return buffer.toString();
    }


}
