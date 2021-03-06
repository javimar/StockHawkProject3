package es.javimar.stockhawk;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import es.javimar.stockhawk.data.HistoryEntries;

import java.io.IOException;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import yahoofinance.YahooFinance;


public final class Utils
{
    private Utils() {}

    /** Returns true if the network is connected or about to become available */
    public static boolean isNetworkAvailable(Context c)
    {
        // Get a reference to the ConnectivityManager to check state of network connectivity
        ConnectivityManager cm =
                (ConnectivityManager)c.getSystemService(Context.CONNECTIVITY_SERVICE);
        // Get details on the currently active default data network
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }



    public static boolean isValidStock(String stock)
    {
        try
        {
            return YahooFinance.get(stock).getName() != null;
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return false;
    }


    /** Return a list of value pairs: date, price formatted */
    public static List<HistoryEntries> formatHistory(String history)
    {
        List<HistoryEntries> list = new ArrayList<>();

        String[] lineDivider = history.split("\n");

        NumberFormat formatter = NumberFormat.getNumberInstance(Locale.US);
        formatter.setMinimumFractionDigits(2);
        formatter.setMaximumFractionDigits(2);

        for (String line : lineDivider)
        {
            String[] commaDivider = line.split(",");
            //String date = formatDate(Long.parseLong(commaDivider[0]));
            String date = commaDivider[0];
            float price = Float.parseFloat(formatter.format(Float.parseFloat(commaDivider[1])));
            list.add(new HistoryEntries(date, price));
        }

        // Sorting dates chronologically
        Collections.sort(list, new Comparator<HistoryEntries>()
        {
            @Override
            public int compare(HistoryEntries date2, HistoryEntries date1)
            {
                return  date2.getmDate().compareTo(date1.getmDate());
            }
        });
        return list;
    }


    public static String formatDate(long milliSeconds)
    {
        // Create a DateFormat object for displaying date in the locale's format
        DateFormat df = DateFormat
                .getDateInstance(DateFormat.SHORT, Locale.getDefault());
        // Create a calendar object to convert the date and time value from milliseconds to date
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(milliSeconds);
        return df.format(calendar.getTime());
    }

}
