package io.sci.citizen.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.sci.citizen.BuildConfig;
import io.sci.citizen.model.Project;
import io.sci.citizen.model.QuestionParameter;
import io.sci.citizen.model.Data;
import io.sci.citizen.model.User;

public class Memory {

    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;

    private Context _context;

    private static final String PREF_NAME = BuildConfig.APPLICATION_ID;
    private static final String CURRENT_DATA = "current_data";
    private static final String DATA_MAP = "data_map";
    private static final String TOKEN = "token";
    private static final String USER_DATA = "user_data";
    private static final String PROJECTS = "projects";
    private static final String CURRENT_PROJECT = "current_project";
    private static final String SURVEY_ANSWERS = "survey_answers";
    private static final String LOGIN_KEY = "login_key";

    public Memory(Context context) {
        this._context = context;
        preferences = _context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = preferences.edit();
    }

    private static final String TAG = Memory.class.toString();

    public void putString(Context context, String key, String value){
        editor.putString(key, value).commit();
    }

    public String getString(Context context, String key){
        return preferences.getString(key, "");
    }

    public void putBoolean(Context context, String key, boolean value){
        editor.putBoolean(key, value).commit();
    }

    public boolean getBoolean(Context context, String key){
        return preferences.getBoolean(key, false);
    }
    public void setToken(Context context, String token){
        putString(context, TOKEN, token);
    }

    public String getToken(Context context){
        return getString(context, TOKEN);
    }

    public User getUser(Context context){
        try{
            String json = getString(context, USER_DATA);
            return new Gson().fromJson(json, User.class);
        }catch (Exception e){
            return null;
        }
    }

    public void setUser(Context context, User user){
        putString(context, USER_DATA, new Gson().toJson(user));
    }

    public void setLoginFlag(Context context, boolean flag){
        putBoolean(context, LOGIN_KEY, flag);
    }

    public boolean getLoginFlag(Context context){
        return getBoolean(context, LOGIN_KEY);
    }


    private Map<Long,Data> currentData(){
        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").enableComplexMapKeySerialization().create();
        String json = preferences.getString(CURRENT_DATA, "");
        if (json.isEmpty()){
            return null;
        }
        return gson.fromJson(json, new TypeToken<LinkedHashMap<Long, Data>>() {}.getType());
    }

    public void setData(Data data){
        Map<Long,Data> map = currentData();
        if (map==null){
            map = new LinkedHashMap<>();
        }
        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").enableComplexMapKeySerialization().create();
        if (data !=null) {
            map.put(project().getId(),data);
            String json = gson.toJson(map);
            editor.putString(CURRENT_DATA, json);
        }else{
            editor.putString(CURRENT_DATA, "");
        }
        editor.commit();
    }

    public Data getData(){
        Map<Long,Data> currentData = currentData();
        if (currentData!=null){
            return currentData.get(project().getId());
        }
        return null;
    }

    private Map<Long,LinkedHashMap<String,Data>> allData(){
        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").enableComplexMapKeySerialization().create();
        String json = preferences.getString(DATA_MAP, "");
        if (Objects.equals(json, "")){
            return new LinkedHashMap<>();
        }
        return gson.fromJson(json, new TypeToken<Map<Long,LinkedHashMap<String,Data>>>() {}.getType());
    }

    public LinkedHashMap<String, Data> getDataMap(){
        Map<Long,LinkedHashMap<String,Data>> map = allData();
        LinkedHashMap<String,Data> data = map.get(project().getId());
        if (data==null){
            return new LinkedHashMap<>();
        }
        return data;
    }

    public LinkedHashMap<String, Data> removeData(){
        Map<Long,LinkedHashMap<String,Data>> map = allData();
        LinkedHashMap<String,Data> data = map.remove(project().getId());
        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").enableComplexMapKeySerialization().create();
        String json = gson.toJson(map);
        editor.putString(DATA_MAP, json);
        editor.commit();
        return data;
    }

    public Data getData(String uuid){
        Map<String, Data> map = getDataMap();
        return map.get(uuid);
    }

    public void putData(String uuid, Data data){
        Map<Long,LinkedHashMap<String,Data>> allData = allData();
        LinkedHashMap<String, Data> map = getDataMap();
        map.put(uuid, data);
        allData.put(project().getId(),map);
        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").enableComplexMapKeySerialization().create();
        String json = gson.toJson(allData);
        editor.putString(DATA_MAP, json);
        editor.commit();
    }

    public void deleteData(String id){
        Map<Long,LinkedHashMap<String,Data>> allData = allData();
        LinkedHashMap<String, Data> map = getDataMap();
        map.remove(id);
        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").enableComplexMapKeySerialization().create();
        String json = gson.toJson(allData);
        editor.putString(DATA_MAP, json);
        editor.commit();
    }

    public Data saveData(){
        Data data = getData();
        data.setFinishDate(new Date());
        putData(data.getUuid(), data);
        setData(null);
        setSurveyAnswers(new HashMap<>());
        return data;
    }

    public Map<Long,Project> getProjects(){
        try {
            Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").enableComplexMapKeySerialization().create();
            String json = preferences.getString(PROJECTS, "");
            if (Objects.equals(json, "")){
                return new LinkedHashMap<>();
            }
            return gson.fromJson(json, new TypeToken<Map<Long,Project>>() {}.getType());
        }catch(Exception ex){
            return null;
        }
    }

    public void setProjects(Map<Long,Project> list){
        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").enableComplexMapKeySerialization().create();
        String json = gson.toJson(list);
        editor.putString(PROJECTS, json);
        editor.commit();
    }

    public void setProject(Project project){
        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").enableComplexMapKeySerialization().create();
        if (project !=null) {
            String json = gson.toJson(project);
            editor.putString(CURRENT_PROJECT, json);
        }else{
            editor.putString(CURRENT_PROJECT, "");
        }
        editor.commit();
    }

    public Project project(){
        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").enableComplexMapKeySerialization().create();
        String json = preferences.getString(CURRENT_PROJECT, "");
        if (json.isEmpty()){
            return null;
        }
        return gson.fromJson(json, Project.class);
    }

    public void putSurveyAnswers(Long key, List<QuestionParameter> value){
        Map<Long, List<QuestionParameter>> map = getSurveyAnswers();
        map.put(key,value);
        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").enableComplexMapKeySerialization().create();
        String json = gson.toJson(map);
        editor.putString(SURVEY_ANSWERS, json);
        editor.commit();
    }

    public void setSurveyAnswers(Map<Long, List<QuestionParameter>> map){
        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").enableComplexMapKeySerialization().create();
        String json = gson.toJson(map);
        editor.putString(SURVEY_ANSWERS, json);
        editor.commit();
    }

    public List<QuestionParameter> getSurveyAnswers(Long question){
        Map<Long, List<QuestionParameter>> map = getSurveyAnswers();
        return map.get(question);
    }

    public Map<Long, List<QuestionParameter>> getSurveyAnswers(){
        try {
            Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").enableComplexMapKeySerialization().create();
            String json = preferences.getString(SURVEY_ANSWERS, "");
            if (Objects.equals(json, "")){
                return new LinkedHashMap<>();
            }
            return gson.fromJson(json, new TypeToken<Map<Long, List<QuestionParameter>>>() {}.getType());
        }catch(Exception ex){
            return null;
        }
    }
}
