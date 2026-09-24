package com.nzr.corretor;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class ProcessTextActivity extends Activity {
    private SpellCorrectionEngine engine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        int p = dp(28);
        root.setPadding(p, p, p, p);
        root.setBackgroundColor(Color.rgb(247, 247, 250));

        ProgressBar progress = new ProgressBar(this);
        root.addView(progress, new LinearLayout.LayoutParams(dp(48), dp(48)));

        TextView label = new TextView(this);
        label.setText("Corrigindo ortografia inteira…");
        label.setTextSize(18);
        label.setTextColor(Color.rgb(30, 30, 35));
        label.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        labelParams.topMargin = dp(18);
        root.addView(label, labelParams);

        setContentView(root);

        Intent incoming = getIntent();
        if (!Intent.ACTION_PROCESS_TEXT.equals(incoming.getAction())) {
            finish();
            return;
        }

        CharSequence selected = incoming.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT);
        final String text = selected == null ? "" : selected.toString();
        final boolean readOnly = incoming.getBooleanExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, false);

        engine = new SpellCorrectionEngine(this);
        engine.correct(text, new SpellCorrectionEngine.Callback() {
            @Override
            public void onSuccess(String correctedText, int replacements) {
                if (readOnly) {
                    Toast.makeText(ProcessTextActivity.this,
                            "O aplicativo de origem não permite substituir o texto selecionado.",
                            Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }

                Intent result = new Intent();
                result.putExtra(Intent.EXTRA_PROCESS_TEXT, correctedText);
                setResult(RESULT_OK, result);
                Toast.makeText(ProcessTextActivity.this,
                        replacements == 0 ? "Nenhuma correção necessária." : replacements + " correção(ões) aplicada(s).",
                        Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onUnavailable(String reason) {
                Toast.makeText(ProcessTextActivity.this, reason, Toast.LENGTH_LONG).show();
                setResult(RESULT_CANCELED);
                finish();
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (engine != null) engine.cancel();
        super.onDestroy();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
