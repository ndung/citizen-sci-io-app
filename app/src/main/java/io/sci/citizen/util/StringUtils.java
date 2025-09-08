package io.sci.citizen.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtils {

    public static final boolean isNullOrEmpty(String value){
        if(value == null || value.isEmpty() || value.equals("") || value.equals("-")){
            return true;
        }else {
            return false;
        }
    }


    public static final Pattern VALID_EMAIL_ADDRESS_REGEX =
            Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);

    public static boolean isEmailValid(String emailStr) {
        Matcher matcher = VALID_EMAIL_ADDRESS_REGEX .matcher(emailStr);
        return matcher.find();
    }

    public static boolean isPasswordValid(String password) {

        String PASSWORD_PATTERN = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d]{6,}$";

        /**if (isDigit) PASSWORD_PATTERN += "(?=.*\\d)";
         if (isCase) PASSWORD_PATTERN += "(?=.*[a-z])";
         PASSWORD_PATTERN += ".{" + minLength + "," + maxLength + "}";
         PASSWORD_PATTERN = "(" + PASSWORD_PATTERN + ")";*/

        //Pattern pattern = Pattern.compile(PASSWORD_PATTERN);
        //Matcher matcher = pattern.matcher(password);
        //return matcher.matches();

        return password.length()>=6;
    }

}