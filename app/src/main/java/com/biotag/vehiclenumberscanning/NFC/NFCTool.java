package com.biotag.vehiclenumberscanning.NFC;

import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.util.Log;

import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Map;

/**
 * Created by Administrator on 2017-10-18.
 */

public class NFCTool {
    private final String TAG = this.getClass().getSimpleName();

    private MifareClassic mc = null;
    private Map<String,byte[]> mKeyMap = null;


    private byte[] defalutKey = {(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF};

    public CardInfo readTag(Tag tag){
        boolean isMifareClassic = false;
        CardInfo cardInfo = new CardInfo();

        for(String tech:tag.getTechList()){
            if(tech.equals("android.nfc.tech.MifareClassic")){
                isMifareClassic = true;
            }
        }

        if(tag == null){
            android.util.Log.i(TAG, "writeToNFC: detectedTag == null");
            return null;
        }

        if(!isMifareClassic){
            android.util.Log.i(TAG, "readMifareClassic: detectTag not include MifareClassic");

        }

        mc = MifareClassic.get(tag);
        if(mc == null){
            android.util.Log.i(TAG, "writeToNFC: mc == null");
            return null;
        }

        boolean result = false;
        try {
            mc.connect();

            byte[] uid = mc.getTag().getId();
            mKeyMap = M1Key.CalculateKey(uid);
            if(mKeyMap == null){
                mc.close();
                return null;
            }
            byte[] keyB = mKeyMap.get(Constants.KEYB);
            Log.i(TAG, "readTag: keyB = " + Utils.bytesToHexString(keyB));

            cardInfo.setCardID(Utils.bytesToHexString(uid).toUpperCase());
            result = readIDGroupID(cardInfo);
            if(!result){
                mc.close();
                return null;
            }
            result = readIDCard(cardInfo);
            if(!result){
                mc.close();
                return null;
            }
            result = readStaffNo(cardInfo);
            if(!result){
                mc.close();
                return null;
            }
            result = readStaffName(cardInfo);
            if(!result){
                mc.close();
                return null;
            }
            result = readCompanyName(cardInfo);
            if(!result){
                mc.close();
                return null;
            }
            result = readAreaNow(cardInfo);
            if(!result){
                mc.close();
                return null;
            }
            result = readLastModifiedTime(cardInfo);
            if(!result){
                mc.close();
                return null;
            }
            result = readAreaNo(cardInfo);
            if(!result){
                mc.close();
                return null;
            }
            result = readSeatNo(cardInfo);
            if(!result){
                mc.close();
                return null;
            }
            result = readImageUrl(cardInfo);
            if(!result){
                mc.close();
                return null;
            }

            cardInfo.printInfo();
            return cardInfo;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }finally {
            try {
                mc.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    private boolean doAuthenticate(int sector){
        if(mc == null){
            Log.i(TAG, "doAuthenticate: mc == null");
        }

        try {
            if(mc.isConnected()){
                boolean auth = mc.authenticateSectorWithKeyB(sector,mKeyMap.get(Constants.KEYB));
                if(auth){
                    return true;
                }
                auth = mc.authenticateSectorWithKeyB(sector,defalutKey);
                if(auth){
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean readIDGroupID(CardInfo cardInfo){
        int sector = Constants.IDGROUPIDCARDTYPE_SECTOR;
        ByteArrayInputStream bais = null;
        try {
            if(doAuthenticate(sector) ){
                Log.i(TAG, "readIDGroupID: doAuthenticate success");
                byte[] data = mc.readBlock(mc.sectorToBlock(sector));
                String IDtmp = Utils.bytesToHexString(data).toUpperCase();
                String ID = IDtmp.substring(0, 8) + "-" + IDtmp.substring(8, 12) + "-" + IDtmp.substring(12, 16) + "-" + IDtmp.substring(16, 20) + "-" + IDtmp.substring(20, 32);
                Log.i(TAG, "readIDGroupID: ID = " + ID);
                cardInfo.setID(ID.trim());

                data = mc.readBlock(mc.sectorToBlock(sector) + 1);
                bais = new ByteArrayInputStream(data);

                byte[] GroupIDByte = new byte[4];
                bais.read(GroupIDByte);
                int GroupID = (int)Utils.byteArrayToLong(GroupIDByte);
                cardInfo.setGroupID(GroupID);

                int CardType = bais.read();
                cardInfo.setCardType(CardType);


            }else{
                Log.i(TAG, "readIDGroupID: doAuthenticate fail");
                return false;
            }
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }finally {
            try {
                if(bais != null)bais.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    private boolean readIDCard(CardInfo cardInfo){
        int sector = Constants.IDCARD_SECTOR;
        ByteArrayOutputStream baos = null;
        ByteArrayInputStream bais = null;
        try {
            if(doAuthenticate(sector) ){
                int start = mc.sectorToBlock(sector);
                int end = mc.sectorToBlock(sector) + 3;

                baos = new ByteArrayOutputStream();
                for (int i = start;i < end;i++) {
                    byte[] data = mc.readBlock(i);
                    baos.write(data);
                }
                bais = new ByteArrayInputStream(baos.toByteArray());

                int IdCard_type = bais.read();
                cardInfo.setIdCard_type(IdCard_type);
                int IdCardByteLen = bais.read();
                byte[] IdCardByte = new byte[IdCardByteLen];
                bais.read(IdCardByte);
                String IdCard = new String(IdCardByte,"utf-8");

                cardInfo.setIdCard(IdCard);

            }else{
                Log.i(TAG, "readIDCard: doAuthenticate fail");
                return false;
            }
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }finally {
            try {
                if(bais != null)bais.close();
                if(baos != null)baos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    private boolean readStaffNo(CardInfo cardInfo) {
        int sector = Constants.STAFFNO_SECTOR;
        ByteArrayOutputStream baos = null;
        ByteArrayInputStream bais = null;
        try {
            if(doAuthenticate(sector) ){
                int start = mc.sectorToBlock(sector);
                int end = mc.sectorToBlock(sector) + 3;

                baos = new ByteArrayOutputStream();
                for (int i = start;i < end;i++) {
                    byte[] data = mc.readBlock(i);
                    baos.write(data);
                }
                bais = new ByteArrayInputStream(baos.toByteArray());

                int StaffNoLen = bais.read();
                byte[] StaffNoByte = new byte[StaffNoLen];
                bais.read(StaffNoByte);
                String StaffNo = new String(StaffNoByte,"utf-8");
                cardInfo.setStaffNo(StaffNo);

            }else{
                Log.i(TAG, "readStaffNo: doAuthenticate fail");
                return false;
            }
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }finally {
            try {
                if(bais != null)bais.close();
                if(baos != null)baos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    private boolean readStaffName(CardInfo cardInfo){

        ByteArrayOutputStream baos = null;
        ByteArrayInputStream bais = null;
        try {
            int sector = Constants.STAFFNAME_SECTORA;
            baos = new ByteArrayOutputStream();
            for (;sector <=  Constants.STAFFNAME_SECTORB;sector ++){
                if(doAuthenticate(sector) ){
                    int start = mc.sectorToBlock(sector);
                    int end = mc.sectorToBlock(sector) + 3;

                    for (int i = start;i < end;i++) {
                        byte[] data = mc.readBlock(i);
                        baos.write(data);
                    }
                }else{
                    Log.i(TAG, "readStaffName: doAuthenticate fail");
                    return false;
                }
            }
            bais = new ByteArrayInputStream(baos.toByteArray());

            int StaffNameByteLen = bais.read();
            byte[] StaffNameByte = new byte[StaffNameByteLen];
            bais.read(StaffNameByte);
            String StaffName = new String(StaffNameByte,"utf-8");
            cardInfo.setStaffName(StaffName);

        }catch (Exception e){
            e.printStackTrace();
            return false;
        }finally {
            try {
                if(bais != null)bais.close();
                if(baos != null)baos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return true;
    }


    private boolean readCompanyName(CardInfo cardInfo){

        ByteArrayOutputStream baos = null;
        ByteArrayInputStream bais = null;
        try {
            int sector = Constants.COMPANYNAME_SECTORA;
            baos = new ByteArrayOutputStream();
            for (;sector <=  Constants.COMPANYNAME_SECTORB;sector ++){
                if(doAuthenticate(sector) ){
                    int start = mc.sectorToBlock(sector);
                    int end = mc.sectorToBlock(sector) + 3;

                    for (int i = start;i < end;i++) {
                        byte[] data = mc.readBlock(i);
                        baos.write(data);
                    }
                }else{
                    Log.i(TAG, "readStaffName: doAuthenticate fail");
                    return false;
                }
            }
            bais = new ByteArrayInputStream(baos.toByteArray());

            int CompanyNameByteLen = bais.read();
            byte[] CompanyNameByte = new byte[CompanyNameByteLen];
            bais.read(CompanyNameByte);
            String CompanyName = new String(CompanyNameByte,"utf-8");
            cardInfo.setCompanyName(CompanyName);

        }catch (Exception e){
            e.printStackTrace();
            return false;
        }finally {
            try {
                if(bais != null)bais.close();
                if(baos != null)baos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    private boolean readAreaNow(CardInfo cardInfo) {
        int sector = Constants.AREANOW_SECTOR;
        ByteArrayOutputStream baos = null;
        ByteArrayInputStream bais = null;
        try {
            if(doAuthenticate(sector) ){
                int start = mc.sectorToBlock(sector);
                int end = mc.sectorToBlock(sector) + 3;

                baos = new ByteArrayOutputStream();
                for (int i = start;i < end;i++) {
                    byte[] data = mc.readBlock(i);
                    baos.write(data);
                }
                bais = new ByteArrayInputStream(baos.toByteArray());

                int AreaNowByteLen = bais.read();
                byte[] AreaNowByte = new byte[AreaNowByteLen];
                bais.read(AreaNowByte);
                String AreaNow = new String(AreaNowByte,"utf-8");
                cardInfo.setAreaNow(AreaNow.trim());

            }else{
                Log.i(TAG, "readStaffNo: doAuthenticate fail");
                return false;
            }
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }finally {
            try {
                if(bais != null)bais.close();
                if(baos != null)baos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return true;
    }


    private boolean readLastModifiedTime(CardInfo cardInfo) {
        int sector = Constants.LASTMODIFIEDTIME_SECTOR;
        ByteArrayOutputStream baos = null;
        ByteArrayInputStream bais = null;
        try {
            if(doAuthenticate(sector) ){
                int start = mc.sectorToBlock(sector);
                int end = mc.sectorToBlock(sector) + 3;

                baos = new ByteArrayOutputStream();
                for (int i = start;i < end;i++) {
                    byte[] data = mc.readBlock(i);
                    baos.write(data);
                }
                bais = new ByteArrayInputStream(baos.toByteArray());

                int LastModifiedTimeByteLen = bais.read();
                byte[] LastModifiedTimeByte = new byte[LastModifiedTimeByteLen];
                bais.read(LastModifiedTimeByte);
                String LastModifiedTime = new String(LastModifiedTimeByte,"utf-8");
                cardInfo.setLastModifiedTime(LastModifiedTime);

            }else{
                Log.i(TAG, "readStaffNo: doAuthenticate fail");
                return false;
            }
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }finally {
            try {
                if(bais != null)bais.close();
                if(baos != null)baos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    private boolean readAreaNo(CardInfo cardInfo) {
        int sector = Constants.AREANO_SECTOR;
        ByteArrayOutputStream baos = null;
        ByteArrayInputStream bais = null;
        try {
            if(doAuthenticate(sector) ){
                int start = mc.sectorToBlock(sector);
                int end = mc.sectorToBlock(sector) + 1;

                baos = new ByteArrayOutputStream();
                for (int i = start;i < end;i++) {
                    byte[] data = mc.readBlock(i);
                    baos.write(data);
                }
                bais = new ByteArrayInputStream(baos.toByteArray());

                int AreaNoByteLen = bais.read();
                byte[] AreaNoByte = new byte[AreaNoByteLen];
                bais.read(AreaNoByte);
                String AreaNo = new String(AreaNoByte,"utf-8");
                AreaNo = AreaNo.replaceAll("[^A-Z]","");
                String[] strArray = AreaNo.split("");
                AreaNo = StringUtils.join(strArray," ");
                Log.i(TAG, "readAreaNo: AreaNo = " + AreaNo);

                cardInfo.setAreaNo(AreaNo.trim());

            }else{
                Log.i(TAG, "readStaffNo: doAuthenticate fail");
                return false;
            }
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }finally {
            try {
                if(bais != null)bais.close();
                if(baos != null)baos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    private boolean readSeatNo(CardInfo cardInfo) {
        int sector = Constants.SEAT_NO;
        ByteArrayOutputStream baos = null;
        ByteArrayInputStream bais = null;
        try {
            if(doAuthenticate(sector) ){
                int start = mc.sectorToBlock(sector) + 1;
                int end = mc.sectorToBlock(sector) + 3;

                baos = new ByteArrayOutputStream();
                for (int i = start;i < end;i++) {
                    byte[] data = mc.readBlock(i);
                    baos.write(data);
                }
                bais = new ByteArrayInputStream(baos.toByteArray());

                int SeatNoByteLen = bais.read();
                byte[] SeatNoByte = new byte[SeatNoByteLen];
                bais.read(SeatNoByte);
                String SeatNo = new String(SeatNoByte,"utf-8");
                Log.i(TAG, "readAreaNo: SeatNo = " + SeatNo);

                cardInfo.setSeatNo(SeatNo.trim());

            }else{
                Log.i(TAG, "readStaffNo: doAuthenticate fail");
                return false;
            }
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }finally {
            try {
                if(bais != null)bais.close();
                if(baos != null)baos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return true;
    }


    private boolean readImageUrl(CardInfo cardInfo) {
        int sector = Constants.IMAGEURL_SECTOR;
        ByteArrayOutputStream baos = null;
        ByteArrayInputStream bais = null;
        try {
            if(doAuthenticate(sector) ){
                int start = mc.sectorToBlock(sector);
                int end = mc.sectorToBlock(sector) + 3;

                baos = new ByteArrayOutputStream();
                for (int i = start;i < end;i++) {
                    byte[] data = mc.readBlock(i);
                    baos.write(data);
                }
                bais = new ByteArrayInputStream(baos.toByteArray());

                int ImageUrlByteLen = bais.read();
                byte[] ImageUrlByte = new byte[ImageUrlByteLen];
                bais.read(ImageUrlByte);
                String ImageUrl = new String(ImageUrlByte,"utf-8");
                ImageUrl = ImageUrl.replaceAll(" ","");

                Log.i(TAG, "readImageUrl: ImageUrl = " + ImageUrl);

                cardInfo.setImageUrl(ImageUrl);

            }else{
                Log.i(TAG, "readImageUrl: doAuthenticate fail");
                return false;
            }
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }finally {
            try {
                if(bais != null)bais.close();
                if(baos != null)baos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return true;
    }


    public boolean WriteAreaNow(String AreaNow){
        int sector = Constants.AREANOW_SECTOR;
        ByteArrayOutputStream baos = null;

        try{
            mc.connect();
            if(doAuthenticate(sector)){
                baos = new ByteArrayOutputStream();

                byte[] AreaNowByte = AreaNow.getBytes("utf-8");
                baos.write(AreaNowByte.length);
                baos.write(AreaNowByte);

                int end = 16 - baos.size();
                for(int i = 0; i < end;i ++){
                    baos.write(0);
                }

                mc.writeBlock(mc.sectorToBlock(sector),baos.toByteArray());
            }

            mc.close();

            return true;
        }catch(Exception e){
            e.printStackTrace();
            return  false;
        }finally {
            try {
                 if(baos != null)baos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
