package com.nzr.corretor;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

public class ProcessTextActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent incoming = getIntent();
        if (!Intent.ACTION_PROCESS_TEXT.equals(incoming.getAction())) {
            finish();
            return;
        }

        CharSequence selected = incoming.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT);
        String text = selected == null ? "" : selected.toString();
        boolean readOnly = incoming.getBooleanExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, false);

        if (readOnly) {
            Toast.makeText(this,
                    "O aplicativo de origem não permite substituir o texto selecionado.",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        LocalCorrectionEngine.Result result = LocalCorrectionEngine.correct(text);

        Intent response = new Intent();
        response.putExtra(Intent.EXTRA_PROCESS_TEXT, result.text);
        setResult(RESULT_OK, response);

        Toast.makeText(this,
                result.text.equals(text) ? "O texto já está corrigido." : "Texto corrigido.",
                Toast.LENGTH_SHORT).show();
        finish();
    }
}
