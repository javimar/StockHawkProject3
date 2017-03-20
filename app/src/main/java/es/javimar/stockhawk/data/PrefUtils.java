package es.javimar.stockhawk.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.Toast;

import es.javimar.stockhawk.R;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static es.javimar.stockhawk.Utils.isNetworkAvailable;
import static es.javimar.stockhawk.Utils.isValidStock;

public final class PrefUtils
{
    private PrefUtils() {}

    /** Returns the stock symbols stored in preferences */
    public static Set<String> getStocks(Context context)
    {
        String stocksKey = context.getString(R.string.pref_stocks_key);
        String initializedKey = context.getString(R.string.pref_stocks_initialized_key);
        String[] defaultStocksList = context.getResources().getStringArray(R.array.default_stocks);

        HashSet<String> defaultStocks = new HashSet<>(Arrays.asList(defaultStocksList));
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);


        boolean initialized = prefs.getBoolean(initializedKey, false);

        if (!initialized)
        {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(initializedKey, true);
            editor.putStringSet(stocksKey, defaultStocks);
            editor.apply();
            return defaultStocks;
        }
        return prefs.getStringSet(stocksKey, new HashSet<String>());

    }

    /** Updates the preferences with a new stock symbol */
    private static void editStockPref(Context context, String symbol, Boolean add)
    {
        String key = context.getString(R.string.pref_stocks_key);
        Set<String> stocks = getStocks(context);

        if (add)
        {
            stocks.add(symbol.trim());
        }
        else
        {
            stocks.remove(symbol);
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putStringSet(key, stocks);
        editor.apply();
    }

    /** add new stock to the pref, this is the public method that accesses the one above */
    public static void addStock(Context context, String symbol)
    {
        // if stock symbol is not valid, don't put it into pref, and bail out early
        if(!isValidStock(symbol.trim()) && isNetworkAvailable(context))
        {
            String message = context.getString(R.string.error_nonvalid_stock, symbol);
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
            return;
        }
        editStockPref(context, symbol, true);
    }


    public static void removeStock(Context context, String symbol)
    {
        editStockPref(context, symbol, false);
    }

    public static String getDisplayMode(Context context)
    {
        String key = context.getString(R.string.pref_display_mode_key);
        String defaultValue = context.getString(R.string.pref_display_mode_default);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(key, defaultValue);
    }

    public static void toggleDisplayMode(Context context)
    {
        String key = context.getString(R.string.pref_display_mode_key);
        String absoluteKey = context.getString(R.string.pref_display_mode_absolute_key);
        String percentageKey = context.getString(R.string.pref_display_mode_percentage_key);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        String displayMode = getDisplayMode(context);

        SharedPreferences.Editor editor = prefs.edit();

        if (displayMode.equals(absoluteKey))
        {
            editor.putString(key, percentageKey);
        }
        else
        {
            editor.putString(key, absoluteKey);
        }

        editor.apply();
    }


}
