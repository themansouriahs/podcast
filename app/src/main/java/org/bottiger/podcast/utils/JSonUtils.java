package org.bottiger.podcast.utils;

import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;

/**
 * Created by aplb on 06-01-2016.
 */
public class JSonUtils {

    public static void setComplexObject(String argKey, SharedPreferences preferences, HashMap<String, List<String>> obj){
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(argKey,new Gson().toJson(obj));
        editor.apply();
    }

    public static HashMap<String, List<String>> getComplexObject (String argKey, SharedPreferences preferences){
        String sobj = preferences.getString(argKey, "");
        Type typeOfHashMap = new TypeToken<HashMap<String, List<String>>>() { }.getType();

        if (sobj.equals(""))
            return new HashMap<>();
        else
            return new Gson().fromJson(sobj, typeOfHashMap);
    }

}
