package com.biotag.vehiclenumberscanning;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;

import com.biotag.vehiclenumberscanning.NFC.Constants;

import org.json.JSONObject;

/**
 * Created by Lxh on 2017/11/14.
 */

public class UpLoadingInandOutInfoService extends Service {
    private SQLiteDatabase db;

    @Override
    public void onCreate() {
        super.onCreate();
        db = MydatabaseHelper.getInstance(this).getReadableDatabase();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final Cursor cursor = db.query("Inoutinfo",new String[]{"StaffID","ChipCode","AreaNo","Action_Type","ActionTime"},null,null,null,null,null);
        if(cursor.moveToFirst()){
            ThreadManager.getInstance().execute(new Runnable() {
                @Override
                public void run() {
                    do {
                        JSONObject obj = new JSONObject();
                        try {
                            obj.put("StaffID",cursor.getString(cursor.getColumnIndex("StaffID")));
                            obj.put("ChipCode",cursor.getString(cursor.getColumnIndex("ChipCode")));
                            obj.put("AreaNo",cursor.getString(cursor.getColumnIndex("AreaNo")));
                            obj.put("Action_Type",cursor.getString(cursor.getColumnIndex("Action_Type")));
                            obj.put("ActionTime",cursor.getString(cursor.getColumnIndex("ActionTime")));
                            String json = String.valueOf(obj);
                            Log.i("Panpan","json is  "+ json);
                            AddInfoSucBean asb =OkhttpPlusUtil.getInstance().post(Constants.URL_POSTINOUT_EMPLOYCARD,json, AddInfoSucBean.class);
                            if(asb!=null&&asb.isIsSuccess()){
                                Log.i("tmsk","信息上传成功");
                            }
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }while (cursor.moveToNext());
                    cursor.close();
                }
            });
        }


        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        int finemin = 5*60*1000;//5 min
        Log.i(Constants.TAG,"Alarm 设置了5 min");
        long triggerAtTime = SystemClock.elapsedRealtime()+finemin;
        Intent intent1 = new Intent(this, InandOutReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(this,0,intent1,0);
        alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,triggerAtTime,pi);
        Log.i(Constants.TAG,"set 已过");
        return super.onStartCommand(intent, flags, startId);
    }
}
