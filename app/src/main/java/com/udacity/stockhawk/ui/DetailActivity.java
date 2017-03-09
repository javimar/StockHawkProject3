package com.udacity.stockhawk.ui;

import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.data.HistoryEntries;
import com.udacity.stockhawk.data.MyXaxisDateFormatter;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.udacity.stockhawk.Utils.formatHistory;
import static com.udacity.stockhawk.data.Contract.Quote;

public class DetailActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor>
{
    private String mSymbol;
    @BindView(R.id.tv_symbol)TextView mTvSymbol;
    @BindView(R.id.tv_stock_name)TextView mTvName;
    @BindView(R.id.tv_price)TextView mTvPrice;
    @BindView(R.id.tv_change)TextView mTvChange;
    @BindView(R.id.toolbar)Toolbar toolbar;
    @BindView(R.id.collapse_toolbar)CollapsingToolbarLayout collapsingToolbarLayout;

    // LineChart is initialized from xml
    @BindView(R.id.stockChart)LineChart mStockChart;

    private static final int DETAIL_STOCK_LOADER = 10;

    private List<HistoryEntries> mHistoryEntries = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.stock_main);
        ButterKnife.bind(this);

        // enable toolbar and disable collapsing toolbar title
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        collapsingToolbarLayout.setTitleEnabled(false);

        Bundle extras = getIntent().getExtras();

        if(extras != null)
        {
            mSymbol = extras.getString("stock");
            getSupportActionBar().setTitle(mSymbol);
            getSupportLoaderManager().initLoader(DETAIL_STOCK_LOADER, null, this);
        }
    }


    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args)
    {
        String selection = Quote.COLUMN_SYMBOL + "=?";
        String [] selectionArgs = new String[] { mSymbol };

        return new CursorLoader(
                this,
                Contract.Quote.URI,
                Contract.Quote.QUOTE_COLUMNS.toArray(new String[]{}),
                selection,
                selectionArgs,
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor)
    {
        // Bail early if the cursor is null or there is less than 1 row in the cursor
        if (cursor == null || cursor.getCount() < 1)
        {
            return;
        }
        // Proceed with moving to the first row of the cursor and reading data from it
        if (cursor.moveToFirst())
        {
            DecimalFormat priceFormat = (DecimalFormat) NumberFormat
                    .getCurrencyInstance(Locale.getDefault());
            priceFormat.setCurrency(Currency.getInstance(Locale.US));

            DecimalFormat dollarFormatWithPlus = (DecimalFormat) NumberFormat
                    .getCurrencyInstance(Locale.getDefault());
            dollarFormatWithPlus.setCurrency(Currency.getInstance(Locale.US));

            DecimalFormat percentageFormat = (DecimalFormat) NumberFormat
                    .getPercentInstance(Locale.getDefault());
            percentageFormat.setCurrency(Currency.getInstance(Locale.US));
            percentageFormat.setMaximumFractionDigits(2);
            percentageFormat.setMinimumFractionDigits(2);
            percentageFormat.setPositivePrefix("+");

            mTvSymbol.setText(mSymbol);
            mTvName.setText(cursor.getString(Quote.POSITION_NAME));
            getSupportActionBar().setSubtitle(cursor.getString(Quote.POSITION_NAME));
            mTvPrice.setText(priceFormat.format(cursor.getDouble(Quote.POSITION_PRICE)));

            double abs_change = cursor.getDouble(Quote.POSITION_PERCENTAGE_CHANGE)/100;
            mTvChange.setText(getString(R.string.change_label,
                    percentageFormat.format(abs_change),
                    dollarFormatWithPlus.format(cursor.getDouble(Quote.POSITION_ABSOLUTE_CHANGE))));

            if (abs_change > 0)
            {
                mTvChange.setTextColor(ContextCompat.getColor(this, R.color.material_green_700));
            }
            else
            {
                mTvChange.setTextColor(ContextCompat.getColor(this, R.color.material_red_700));
            }

           /** Take care of the history and place it in the chart */
            //mTvHistory.setText(formatHistory(cursor.getString(Quote.POSITION_HISTORY)));
            mHistoryEntries = formatHistory(cursor.getString(Quote.POSITION_HISTORY));

            setChart();
        }

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // is there anything to do here?
    }



    private void setChart()
    {
        String[] datesList = new String[mHistoryEntries.size()];
        ArrayList<Entry> yVals = new ArrayList<>();

        for (int i = 0; i < mHistoryEntries.size(); i++)
        {
            datesList[i] = mHistoryEntries.get(i).getmDate();
            yVals.add(new Entry(mHistoryEntries.get(i).getmPrice(), i));
        }

        XAxis xAxis = mStockChart.getXAxis();
        xAxis.setValueFormatter(new MyXaxisDateFormatter(datesList));
        xAxis.setTextSize(12f);
        xAxis.setTextColor(Color.BLACK);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);


        YAxis yAxis = mStockChart.getAxis(YAxis.AxisDependency.LEFT);
        yAxis.setTextSize(12f);
        yAxis.setTextColor(Color.BLUE);

        LineDataSet dataSet = new LineDataSet(yVals, "Stock history");
        LineData lineData = new LineData(dataSet);
        mStockChart.setData(lineData);
        mStockChart.invalidate(); // refresh

    }

}
