package com.biotag.vehiclenumberscanning;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.uuzuche.lib_zxing.activity.CodeUtils;

import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;

public class ScanningActivity extends AppCompatActivity {

    private ImageView iv_qrcode;
    private ImageView iv_nfc;
    private ConstraintLayout ctl;
    private Context context = this;
    private String vehiclenumber;
    public static final int REQUEST_PERMISSION_CAMERA = 1;
    public static final int REQUEST_BARCODE = 2;
    private TextView tv_vehiclenumber;
    private String base64key = "28AAAAEFGGIIIIIIIJLLNNNNOPPQTVSS",algorithm = "DESede";
    private SecureRandom sr = new SecureRandom();
    private SecretKeyFactory keyfactory;
    private DESedeKeySpec dks;
    private SecretKey securekey;
    private long mExittime;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_scanning);

        //权限问题
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) !=
                        PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE)!=
                        PackageManager.PERMISSION_GRANTED) {
            //申请了两种权限：WRITE_EXTERNAL_STORAGE与 CAMERA 权限
            ActivityCompat.requestPermissions((Activity) this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA,
                            Manifest.permission.READ_EXTERNAL_STORAGE},
                    1);
            return;
        }


        getAuthorityCamera();
        initView();
//        encryCode();
    }

//    private void encryCode() {
////        String s = "eUEcRw8IaDiuOn7+d0VNGw==";
//        String s = "mMRn7FkHM6puwUup15M2qg==";//  原文是“川1-23123%B”
//        Log.i(Constants.TAG,"s 的只是"+s);
//        String base64key = "28AAAAEFGGIIIIIIIJLLNNNNOPPQTVSS";
//        String algorithm = "DESede";
//        SecureRandom sr = new SecureRandom();
//        SecretKeyFactory keyfactory ;
//        DESedeKeySpec dks ;
//        SecretKey securekey;
//
//        byte[] orikey = Base64.decode(base64key,Base64.DEFAULT);
//        Log.i(Constants.TAG,"key shi "+new String(orikey));
//        byte[] oris   = Base64.decode(s,Base64.DEFAULT);
//        try {
//            dks = new DESedeKeySpec(orikey);
//            keyfactory = SecretKeyFactory.getInstance("DESede");
//            securekey = keyfactory.generateSecret(dks);
//            Cipher cipher = Cipher.getInstance("DESede");
//            cipher.init(Cipher.DECRYPT_MODE,securekey);
//            byte[] alot = cipher.doFinal(oris);
//            //下面代码的用意仅仅是为了得到去除那些补0 位之后真正有数据的位置组成的byte[];
//            byte[] alots = forMatebyte(alot);
//            //将原
//            alots = new String(alot).getBytes("GB2312");
//
//            String sk = new String(alot,"GB2312");
//            Log.i(Constants.TAG,"sk "+sk);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//    }

    private void getAuthorityCamera() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(ScanningActivity.this, new String[]{Manifest.permission.CAMERA}, REQUEST_PERMISSION_CAMERA);
            return;
        }

    }


    private void initView() {
        iv_qrcode = (ImageView) findViewById(R.id.iv_qrcode);
        iv_nfc = (ImageView)findViewById(R.id.iv_nfc);
        iv_qrcode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, SecondActivity.class);
                startActivityForResult(intent, REQUEST_BARCODE);
                if(tv_vehiclenumber.getText()!=null){
                    tv_vehiclenumber.setText("");
                }
            }
        });
        iv_nfc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, NFCScanActivity.class);
                startActivity(intent);
            }
        });
        ctl = (ConstraintLayout) findViewById(R.id.ctl);
        ctl.setBackgroundResource(R.color.pink);
        tv_vehiclenumber = (TextView) findViewById(R.id.tv_vehiclenumber);
    }




    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_BARCODE && resultCode == RESULT_OK) {
            if (data != null) {
                final Bundle bundle = data.getExtras();
                if (bundle == null) {
                    return;
                }
                if (bundle.getInt(CodeUtils.RESULT_TYPE) == CodeUtils.RESULT_SUCCESS) {
                    vehiclenumber = bundle.getString(CodeUtils.RESULT_STRING);
                    //未经base64 编码前的key 是orikey
                    byte[] orikey = Base64.decode(base64key,Base64.DEFAULT);
                    //未经base64 编码前的vehiclenumber  是 orivehiclenumber
                    try {

                        byte[] orivehiclenumber = Base64.decode(vehiclenumber,Base64.DEFAULT);
                        dks = new DESedeKeySpec(orikey);
                        keyfactory = SecretKeyFactory.getInstance("DESede");
                        securekey = keyfactory.generateSecret(dks);
                        Cipher cipher = Cipher.getInstance("DESede");
                        cipher.init(Cipher.DECRYPT_MODE,securekey);
                        // 解密之后的到的
                        byte[] realVehiclenumber = cipher.doFinal(orivehiclenumber);
//                        byte[] realvehiclenumbers = forMatebyte(realVehiclenumber);
                        byte[] realvehiclenumbers = new String(realVehiclenumber).getBytes("GB2312");
                        String location = new String(realVehiclenumber,"GB2312");
                        String[]  temp = location.split("%");
                        location = new StringBuffer().append(temp[0]).append(temp[1]).append("区").toString();
                        tv_vehiclenumber.setText(location);
                    }catch (Exception e){
                        e.printStackTrace();

                        final AlertDialog.Builder normalDialog = new AlertDialog.Builder(this);
                        normalDialog.setMessage("二维码数据格式错误");
                        normalDialog.setPositiveButton("确定", null);

                        AlertDialog dialog=normalDialog.create();
                        dialog.show();
                    }
                }
            }
        }
    }

    private byte[] forMatebyte(byte[] arr) {
        int i = 0 ;
        for (; i < arr.length; i++) {
            if(arr[i] == new Byte("0")){
                break;
            }
        }
        byte[] result = new byte[i];
        for (int j = 0; j < i; j++) {
            result[j]=arr[j];
        }
        return result;
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK&&event.getRepeatCount()==0){
            if(System.currentTimeMillis()-mExittime>2000){
                Toast.makeText(context, "再按一次退出App", Toast.LENGTH_SHORT).show();
                mExittime = System.currentTimeMillis();
            }else {
                Toast.makeText(context, "退出App", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        Intent intent = new Intent(this,HeadimgDownloadService.class);
//        stopService(intent);
    }
}
