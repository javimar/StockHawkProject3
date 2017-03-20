package es.javimar.stockhawk.sync;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import es.javimar.stockhawk.R;
import es.javimar.stockhawk.data.Contract;
import es.javimar.stockhawk.data.PrefUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import timber.log.Timber;
import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.histquotes.HistoricalQuote;
import yahoofinance.histquotes.Interval;
import yahoofinance.quotes.stock.StockQuote;

import static es.javimar.stockhawk.Utils.isNetworkAvailable;

public final class QuoteSyncJob
{
    private static final int ONE_OFF_ID = 2;
    public static final String ACTION_DATA_UPDATED = "es.javimar.stockhawk.ACTION_DATA_UPDATED";
    private static final int PERIOD = 300000;  // 5 minutes
    private static final int INITIAL_BACKOFF = 10000;
    private static final int PERIODIC_ID = 1;
    private static final int YEARS_OF_HISTORY = 1; // set to 1 only

    private QuoteSyncJob() {}

    static void getQuotes(Context context)
    {
        Calendar from = Calendar.getInstance();
        Calendar to = Calendar.getInstance();
        // get quote from 1 year of history
        from.add(Calendar.YEAR, -YEARS_OF_HISTORY);

        try
        {
            Set<String> stockPref = PrefUtils.getStocks(context);
            Set<String> stockCopy = new HashSet<>();
            stockCopy.addAll(stockPref);
            String[] stockArray = stockPref.toArray(new String[stockPref.size()]);

            if (stockArray.length == 0)
            {
                return;
            }

            /**
             * Sends a basic quotes request to Yahoo Finance. This will return a Map object
             * that links the symbols to their respective Stock objects. The Stock objects
             * have their StockQuote, StockStats and StockDividend member fields filled in
             * with the available data.
             */
            Map<String, Stock> quotes = YahooFinance.get(stockArray);
            Iterator<String> iterator = stockCopy.iterator();

            ArrayList<ContentValues> quoteCVs = new ArrayList<>();

            while (iterator.hasNext())
            {
                String symbol = iterator.next();

                Stock stock = quotes.get(symbol);
                String stock_name = stock.getName();
                // check if it is a valid one
                if(null != stock_name)
                {
                    StockQuote quote = stock.getQuote();

                    float price = quote.getPrice().floatValue();
                    float change = quote.getChange().floatValue();
                    float percentChange = quote.getChangeInPercent().floatValue();

                    // WARNING! Don't request historical data for a stock that doesn't exist!
                    // The request will hang forever X_x
                    List<HistoricalQuote> history = stock.getHistory(from, to, Interval.WEEKLY);

                    StringBuilder historyBuilder = new StringBuilder();

                    for (HistoricalQuote it : history) {

                        historyBuilder.append(it.getDate().getTimeInMillis());
                        historyBuilder.append(",");
                        historyBuilder.append(it.getClose());
                        historyBuilder.append("\n");
                    }
                    ContentValues quoteCV = new ContentValues();
                    quoteCV.put(Contract.Quote.COLUMN_SYMBOL, symbol);
                    quoteCV.put(Contract.Quote.COLUMN_PRICE, price);
                    quoteCV.put(Contract.Quote.COLUMN_PERCENTAGE_CHANGE, percentChange);
                    quoteCV.put(Contract.Quote.COLUMN_ABSOLUTE_CHANGE, change);
                    quoteCV.put(Contract.Quote.COLUMN_HISTORY, historyBuilder.toString());
                    quoteCV.put(Contract.Quote.COLUMN_NAME, stock_name);

                    quoteCV.put(Contract.Quote.COLUMN_STOCKEXCHANGE, stock.getStockExchange());
                    quoteCV.put(Contract.Quote.COLUMN_PREVCLOSE,
                            String.valueOf(quote.getPreviousClose()));
                    quoteCV.put(Contract.Quote.COLUMN_BID, String.valueOf(quote.getBid()));
                    quoteCV.put(Contract.Quote.COLUMN_YIELD,
                            String.valueOf((stock.getDividend().getAnnualYieldPercent())));
                    quoteCV.put(Contract.Quote.COLUMN_OPEN, String.valueOf(quote.getOpen()));

                    quoteCVs.add(quoteCV);
                }
                else
                {
                    // bad stock, delete it from preferences and warn user
                    String message = context
                            .getString(R.string.error_nonvalid_stock_async, symbol);
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show();
                    PrefUtils.removeStock(context, symbol);
                }
            }

            context.getContentResolver()
                    .bulkInsert(
                            Contract.Quote.URI,
                            quoteCVs.toArray(new ContentValues[quoteCVs.size()]));

            Intent dataUpdatedIntent = new Intent(ACTION_DATA_UPDATED);
            context.sendBroadcast(dataUpdatedIntent);

        }
        catch (IOException exception) {
            Timber.e(exception, "Error fetching stock quotes");
        }
    }

    private static void schedulePeriodic(Context context)
    {
        JobInfo.Builder builder = new JobInfo.Builder(PERIODIC_ID,
                new ComponentName(context, QuoteJobService.class));


        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(PERIOD)
                .setBackoffCriteria(INITIAL_BACKOFF, JobInfo.BACKOFF_POLICY_EXPONENTIAL);


        JobScheduler scheduler = (JobScheduler) context
                .getSystemService(Context.JOB_SCHEDULER_SERVICE);

        scheduler.schedule(builder.build());
    }


    public static synchronized void initialize(final Context context)
    {
        schedulePeriodic(context);
        syncImmediately(context);
    }


    public static synchronized void syncImmediately(Context context)
    {
        if (isNetworkAvailable(context))
        {
            Intent nowIntent = new Intent(context, QuoteIntentService.class);
            context.startService(nowIntent);
        }
        else
        {
            JobInfo.Builder builder = new JobInfo.Builder(ONE_OFF_ID,
                    new ComponentName(context, QuoteJobService.class));

            builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setBackoffCriteria(INITIAL_BACKOFF, JobInfo.BACKOFF_POLICY_EXPONENTIAL);

            JobScheduler scheduler = (JobScheduler)
                    context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

            scheduler.schedule(builder.build());
        }
    }


}
