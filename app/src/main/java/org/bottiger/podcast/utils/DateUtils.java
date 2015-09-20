package org.bottiger.podcast.utils;

import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by aplb on 19-09-2015.
 */
public class DateUtils {

    @Nullable
    public static java.util.Date parseRFC3339Date(String datestring) throws java.text.ParseException, IndexOutOfBoundsException{

        if (TextUtils.isEmpty(datestring))
            return null;

        Date d = new Date();

        // Some feed format the date as: Wed, 09 Sep 2015 12:17:03 PDT
        //
        // http://stackoverflow.com/questions/1223052/how-do-i-find-out-if-first-character-of-a-string-is-a-number
        if (!Character.isDigit(datestring.charAt(0))) {
            try{
                SimpleDateFormat s = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
                d = s.parse(datestring);
            }
            catch(java.text.ParseException pe){//try again with optional decimals
                SimpleDateFormat s = new SimpleDateFormat("EEE, dd MMMM yyyy HH:mm:ss Z");
                s.setLenient(true);
                d = s.parse(datestring);
            }
            return d;
        }

        //if there is no time zone, we don't need to do any special parsing.
        if(datestring.endsWith("Z")){
            try{
                SimpleDateFormat s = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");//spec for RFC3339
                d = s.parse(datestring);
            }
            catch(java.text.ParseException pe){//try again with optional decimals
                SimpleDateFormat s = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'");//spec for RFC3339 (with fractional seconds)
                s.setLenient(true);
                d = s.parse(datestring);
            }
            return d;
        }

        //step one, split off the timezone.
        String firstpart = datestring.substring(0,datestring.lastIndexOf('-'));
        String secondpart = datestring.substring(datestring.lastIndexOf('-'));

        //step two, remove the colon from the timezone offset
        if (secondpart.contains(":"))
            secondpart = secondpart.substring(0,secondpart.indexOf(':')) + secondpart.substring(secondpart.indexOf(':')+1);

        datestring  = firstpart + secondpart;
        SimpleDateFormat s = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");//spec for RFC3339
        try{
            d = s.parse(datestring);
        }
        catch(java.text.ParseException pe){//try again with optional decimals
            s = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ");//spec for RFC3339 (with fractional seconds)
            s.setLenient(true);
            d = s.parse(datestring);
        }
        return d;
    }

}
