package es.javimar.stockhawk.widget;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Binder;
import android.util.Log;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import es.javimar.stockhawk.R;
import es.javimar.stockhawk.data.Contract.Quote;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;


public class StockWidgetRemoteViewsService extends RemoteViewsService
{
    private static final String[] STOCK_COLUMNS = {
            Quote.TABLE_NAME + "." + Quote._ID,
            Quote.COLUMN_SYMBOL,
            Quote.COLUMN_PRICE,
            Quote.COLUMN_ABSOLUTE_CHANGE,
            Quote.COLUMN_PERCENTAGE_CHANGE
    };
    // these indices must match the projection
    static final int INDEX_STOCK_ID = 0;
    static final int INDEX_STOCK_SYMBOL = 1;
    static final int INDEX_STOCK_PRICE = 2;
    static final int INDEX_STOCK_PERC_CHANGE = 4;


    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent)
    {
        return new RemoteViewsFactory()
        {
            private Cursor cursor = null;

            @Override
            public void onCreate() {
                // Nothing to do
            }

            @Override
            public void onDataSetChanged()
            {
                if (cursor != null) {
                    cursor.close();
                }
                // This method is called by the app hosting the widget (e.g., the launcher)
                // However, our ContentProvider is not exported so it doesn't have access to the
                // cursor. Therefore we need to clear (and finally restore) the calling identity so
                // that calls use our process and permission
                final long identityToken = Binder.clearCallingIdentity();
                Uri currentStockUri = Quote.URI;
                cursor = getContentResolver().query(
                        currentStockUri,
                        STOCK_COLUMNS,
                        null,
                        null,
                        Quote.COLUMN_SYMBOL + " ASC");
                Binder.restoreCallingIdentity(identityToken);
            }

            @Override
            public void onDestroy()
            {
                if (cursor != null)
                {
                    cursor.close();
                    cursor = null;
                }
            }

            @Override
            public int getCount()
            {
                return cursor == null ? 0 : cursor.getCount();
            }

            @Override
            public RemoteViews getViewAt(int position)
            {
                DecimalFormat percentageFormat, dollarFormat;

                percentageFormat = (DecimalFormat) NumberFormat
                        .getPercentInstance(Locale.getDefault());
                percentageFormat.setMaximumFractionDigits(2);
                percentageFormat.setMinimumFractionDigits(2);
                percentageFormat.setPositivePrefix("+");
                dollarFormat = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);


                if (position == AdapterView.INVALID_POSITION ||
                        cursor == null || !cursor.moveToPosition(position))
                {
                    return null;
                }
                RemoteViews views = new RemoteViews(getPackageName(),
                        R.layout.widget_detail_list_item);

                // set the values
                views.setTextViewText(R.id.widget_header_title,
                        getString(R.string.widget_title));

                String symbol = cursor.getString(INDEX_STOCK_SYMBOL);
                views.setTextViewText(R.id.widget_stock, symbol);

                float price = cursor.getFloat(INDEX_STOCK_PRICE);
                views.setTextViewText(R.id.widget_price,
                        String.valueOf(dollarFormat.format(price)));

                float change = cursor.getFloat(INDEX_STOCK_PERC_CHANGE);
                String percentage = percentageFormat.format(change / 100);
                views.setTextViewText(R.id.widget_change, percentage);
                if (change > 0)
                {
                    views.setTextColor(R.id.widget_change, Color.rgb(0x7c, 0xd0, 0x74));
                }
                else
                {
                    views.setTextColor(R.id.widget_change, Color.RED);
                }

                final Intent fillInIntent = new Intent();
                fillInIntent.putExtra("stock", symbol);
                views.setOnClickFillInIntent(R.id.widget_list_item, fillInIntent);
                return views;
            }

            @Override
            public RemoteViews getLoadingView() {
                return new RemoteViews(getPackageName(), R.layout.widget_detail_list_item);
            }

            @Override
            public int getViewTypeCount() {
                return 1;
            }

            @Override
            public long getItemId(int position)
            {
                if (cursor.moveToPosition(position))
                    return cursor.getLong(INDEX_STOCK_ID);
                return position;
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }
        };
    }

}