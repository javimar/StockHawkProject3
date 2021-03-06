package es.javimar.stockhawk.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import es.javimar.stockhawk.R;

import butterknife.BindView;
import butterknife.ButterKnife;


public class AddStockDialog extends DialogFragment
{
    @BindView(R.id.dialog_stock)EditText stock;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = LayoutInflater.from(getActivity());
        @SuppressLint("InflateParams") View custom = inflater.inflate(R.layout.add_stock_dialog, null);

        ButterKnife.bind(this, custom);

        stock.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                addStock();
                return true;
            }
        });
        builder.setView(custom);

        builder.setMessage(getString(R.string.dialog_title));
        builder.setPositiveButton(getString(R.string.dialog_add),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        addStock();
                    }
                });
        builder.setNegativeButton(getString(R.string.dialog_cancel), null);

        Dialog dialog = builder.create();

        Window window = dialog.getWindow();
        if (window != null) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }

        return dialog;
    }

    private void addStock()
    {
        Activity parent = getActivity();
        if (parent instanceof MainActivity)
        {
            String alpha = stock.getText().toString().trim();
            if(isAlpha(alpha))
                ((MainActivity) parent).addStock(alpha);
            else
                Toast.makeText(parent, getString(R.string.error_nonvalid_chars),
                        Toast.LENGTH_LONG).show();
        }
        dismissAllowingStateLoss();
    }


    private boolean isAlpha(String stock)
    {
        Pattern pattern = Pattern.compile("^[a-zA-Z]+$");
        Matcher matcher = pattern.matcher(stock);
        return matcher.matches();
    }

}
