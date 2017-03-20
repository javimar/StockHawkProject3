package es.javimar.stockhawk.ui;

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
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import es.javimar.stockhawk.R;
import es.javimar.stockhawk.data.Contract;
import es.javimar.stockhawk.data.HistoryEntries;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;

import static es.javimar.stockhawk.Utils.formatDate;
import static es.javimar.stockhawk.Utils.formatHistory;
import static es.javimar.stockhawk.data.Contract.Quote;

public class DetailActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor>
{
    private String mSymbol;
    @BindView(R.id.tv_price)TextView mTvPrice;
    @BindView(R.id.tv_change)TextView mTvChange;
    @BindView(R.id.tv_stock_exchange)TextView mTvStockExchange;
    @BindView(R.id.tv_annual_yield)TextView mTvAnnualYield;
    @BindView(R.id.tv_prev_close)TextView mTvPrevClose;
    @BindView(R.id.tv_open)TextView TvOpen;
    @BindView(R.id.tv_bid)TextView TvBid;
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
        // get the selected stock from the DB
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

            getSupportActionBar().setSubtitle(cursor.getString(Quote.POSITION_NAME));
            mTvPrice.setText(priceFormat.format(cursor.getDouble(Quote.POSITION_PRICE)));

            double abs_change = cursor.getDouble(Quote.POSITION_PERCENTAGE_CHANGE)/100;
            mTvChange.setText(getString(R.string.detail_change_label,
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

           /* Format the history to place it in the chart */
            mHistoryEntries = formatHistory(cursor.getString(Quote.POSITION_HISTORY));

            mTvStockExchange.setText(getString(R.string.detail_market_label,
                    cursor.getString(Quote.POSITION_STOCKEXCHANGE)));

            float yield = cursor.getFloat(Quote.POSITION_YIELD);
            if(yield == 0)
            {
                mTvAnnualYield.setText(getString(R.string.detail_annualyield_label, "N/A"));
            }
            else
            {
                yield = yield / 100;
                mTvAnnualYield.setText(getString(R.string.detail_annualyield_label,
                        percentageFormat.format(yield)));
            }
            mTvPrevClose.setText(getString(R.string.detail_prevlose_label,
                    priceFormat.format(cursor.getDouble(Quote.POSITION_PREVCLOSE))));
            TvOpen.setText(getString(R.string.detail_open_label,
                    priceFormat.format(cursor.getDouble(Quote.POSITION_OPEN))));
            TvBid.setText(getString(R.string.detail_bid_label,
                    priceFormat.format(cursor.getDouble(Quote.POSITION_BID))));

            // draw chart
            setChart();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // is there anything to do here?
    }



    private void setChart()
    {
        List<Entry> entries = new ArrayList<>();
        final String [] formattedDates = new String[mHistoryEntries.size()];

        for (int i = 0; i < mHistoryEntries.size(); i++)
        {
            formattedDates[i] =
                    formatDate(Long.parseLong(mHistoryEntries.get(i).getmDate()));
            entries.add(new Entry(i, mHistoryEntries.get(i).getmPrice()));
        }

        // interface to return the value of the formatted date for each index
        IAxisValueFormatter formatter = new IAxisValueFormatter()
        {
            @Override
            public String getFormattedValue(float value, AxisBase axis)
            {
                // This will return the formatted date for the corresponding index
                return formattedDates[(int) value];
            }
        };

        XAxis xAxis = mStockChart.getXAxis();
        xAxis.setValueFormatter(formatter);
        xAxis.setTextSize(12f);
        xAxis.setTextColor(Color.BLACK);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawLabels(true);

        YAxis yAxis = mStockChart.getAxis(YAxis.AxisDependency.LEFT);
        yAxis.setTextSize(12f);
        yAxis.setTextColor(Color.BLUE);
        yAxis.setDrawGridLines(false);
        yAxis.setDrawLabels(true);

        LineDataSet dataSet = new LineDataSet(entries, getString(R.string.char_name_points));
        LineData lineData = new LineData(dataSet);
        mStockChart.setAutoScaleMinMaxEnabled(true);
        mStockChart.setData(lineData);
        mStockChart.invalidate(); // refresh
    }

}
