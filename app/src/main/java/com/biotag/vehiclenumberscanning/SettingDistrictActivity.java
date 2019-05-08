package com.biotag.vehiclenumberscanning;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.biotag.vehiclenumberscanning.view.DensityUtils;
import com.biotag.vehiclenumberscanning.view.FlexRadioGroup;
import com.biotag.vehiclenumberscanning.view.HeadimgDownloadService;
import com.google.android.flexbox.FlexboxLayout;

public class SettingDistrictActivity extends AppCompatActivity implements View.OnClickListener {

    private boolean mProtectFromCheckedChange = false;
    private boolean mflexfuncCheckChange      = false;
    private FlexRadioGroup flexrg;
    private FlexRadioGroup flexfunc;
    private Button btn_ok;
    private String[] districts = {"H 区","M 区","B 区","F 区","S 区","C 区","T 区","A 区"};
    private String[] funcs     = {"检票","验证"};
    private String[] districts_1 = {"F 区","S 区"};
    private String areano;
    private Context context;
    private float margin,width;
    private TextView tv_tips;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_setting_district);
        initView();
        context = this;
        margin = DensityUtils.dp2px(context,10);
        width = DensityUtils.getWidth(context);
        //开启服务从服务器拉去头像图片
        if(NetCheckUtil.isNetworkConnected(SettingDistrictActivity.this)){
            Intent intent = new Intent(this, HeadimgDownloadService.class);
            startService(intent);
        }else {
            Toast.makeText(context, "网络未连接", Toast.LENGTH_SHORT).show();
        }
        areano = getIntent().getStringExtra("areano");
        if(areano!=null){
            String[] areaarray = areano.split(" ");
        }
        createfuncradiobutton();
    }


    private void createdistrictradiobutton(String[] districts, final FlexRadioGroup flexrg) {
        for (String district:districts) {
            RadioButton rbdistrict = (RadioButton)getLayoutInflater().inflate(R.layout.item_label,null);
            rbdistrict.setText(district);
            FlexboxLayout.LayoutParams lp = new FlexboxLayout.LayoutParams((int) (width - margin) / 3, ViewGroup.LayoutParams.WRAP_CONTENT);
            rbdistrict.setLayoutParams(lp);
            flexrg.addView(rbdistrict);
            /**
             * 下面两个监听器用于点击两次可以清除当前RadioButton的选中
             * 点击RadioButton后，{@link FlexRadioGroup#OnCheckedChangeListener}先回调，然后再回调{@link View#OnClickListener}
             * 如果当前的RadioButton已经被选中时，不会回调OnCheckedChangeListener方法，故判断没有回调该方法且当前RadioButton确实被选中时清除掉选中
             */
            flexrg.setOnCheckedChangeListener(new FlexRadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(int checkedId) {
                    mProtectFromCheckedChange = true;
                }
            });
            rbdistrict.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(!mProtectFromCheckedChange&&((RadioButton)v).isChecked()){
                        flexrg.clearCheck();
                    }else mProtectFromCheckedChange = false;
                }
            });
        }
    }

    private void createfuncradiobutton() {
        for (String func:funcs) {
            RadioButton rbfunc = (RadioButton)getLayoutInflater().inflate(R.layout.item_label,null);
            rbfunc.setText(func);
            FlexboxLayout.LayoutParams lp = new FlexboxLayout.LayoutParams((int) (width-margin)/2, ViewGroup.LayoutParams.WRAP_CONTENT);
            rbfunc.setLayoutParams(lp);
            flexfunc.addView(rbfunc);
            flexfunc.setOnCheckedChangeListener(new FlexRadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(int checkedId) {
                    mflexfuncCheckChange = true;
                    RadioButton rb = (RadioButton)findViewById(checkedId);
                    if(rb!=null&&rb.getText().toString().trim().equals("检票")){
                        flexrg.removeAllViews();
                        createdistrictradiobutton(districts_1,flexrg);
                        tv_tips.setVisibility(View.VISIBLE);
                        flexrg.setVisibility(View.VISIBLE);
                    }else if(rb!=null&&rb.getText().toString().trim().equals("验证")){
                        flexrg.removeAllViews();
                        createdistrictradiobutton(districts,flexrg);
                        tv_tips.setVisibility(View.VISIBLE);
                        flexrg.setVisibility(View.VISIBLE);
                    }
                }
            });
            rbfunc.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(!mflexfuncCheckChange&&((RadioButton)v).isChecked()){
                        flexfunc.clearCheck();
                    }else mflexfuncCheckChange = false;
                }
            });
        }
    }

    private void initView() {
        flexfunc = (FlexRadioGroup)findViewById(R.id.flexfunc);
        flexrg = (FlexRadioGroup)findViewById(R.id.flexrg);
        btn_ok = (Button) findViewById(R.id.btn_ok);
        btn_ok.setOnClickListener(this);
        tv_tips = (TextView)findViewById(R.id.tv_tips);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_ok:
                //=============================
                RadioButton rbdistrict = (RadioButton)findViewById(flexrg.getCheckedRadioButtonId());
                RadioButton rbfunc     = (RadioButton)findViewById(flexfunc.getCheckedRadioButtonId());
                if(rbdistrict!=null&&rbfunc!=null){
                    String dischosed = rbdistrict.getText().toString().trim();
                    String funcchosed  = rbfunc.getText().toString().trim();
                    SharedPreferencesUtils.saveString(SettingDistrictActivity.this, "dischosed", dischosed);
                    SharedPreferencesUtils.saveString(SettingDistrictActivity.this,"funcchosed",funcchosed);
                    Toast.makeText(this, "该设备被设置的区域是"+ rbdistrict.getText().toString()+",功能是"+rbfunc.getText().toString(), Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, NFCScanActivity.class);
                    startActivity(intent);
                    finish();
                }else {
                    Toast.makeText(this, "您的设置信息不完整", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }
}
