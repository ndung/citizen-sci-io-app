package io.sci.citizen.util;

import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ChipperUtils {
    private static final String APP_KEY = "hpc-lab";

    public static String getPublicKey(String cardNumber, String pwd){
        Date date = new Date();
        String today = new SimpleDateFormat("yyyyMMdd").format(date);
        String generatedKey = getMD5Hash(cardNumber + getMD5Hash(pwd) + today + APP_KEY);
        System.out.println(generatedKey);
        return generatedKey;
    }

    public static String getMD5Hash(String value){
        try{
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(value.getBytes());

            byte byteData[] = md.digest();

            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < byteData.length; i++) {
                sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
            }

            return sb.toString();
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }
}