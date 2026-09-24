package com.nzr.corretor;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private EditText editor;
    private Button correctButton;
    private SpellCorrectionEngine engine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(24), dp(20), dp(20));
        root.setBackgroundColor(Color.rgb(247, 247, 250));

        TextView title = new TextView(this);
        title.setText("NZR Corretor");
        title.setTextSize(26);
        title.setTextColor(Color.rgb(76, 29, 149));
        title.setGravity(Gravity.START);
        root.addView(title);

        TextView description = new TextView(this);
        description.setText("Digite ou cole um texto e toque em Corrigir tudo. Você também pode selecionar texto em outros aplicativos e escolher “Corrigir ortografia inteira”.");
        description.setTextSize(15);
        description.setTextColor(Color.rgb(70, 70, 78));
        LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        descParams.topMargin = dp(10);
        descParams.bottomMargin = dp(16);
        root.addView(description, descParams);

        editor = new EditText(this);
        editor.setHint("Escreva seu texto aqui…");
        editor.setGravity(Gravity.TOP | Gravity.START);
        editor.setTextSize(17);
        editor.setTextColor(Color.rgb(20, 20, 24));
        editor.setHintTextColor(Color.rgb(130, 130, 140));
        editor.setBackgroundColor(Color.WHITE);
        editor.setPadding(dp(14), dp(14), dp(14), dp(14));
        editor.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        LinearLayout.LayoutParams editorParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        root.addView(editor, editorParams);

        correctButton = new Button(this);
        correctButton.setText("Corrigir tudo");
        correctButton.setAllCaps(false);
        correctButton.setTextSize(17);
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(56));
        buttonParams.topMargin = dp(14);
        root.addView(correctButton, buttonParams);

        correctButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                correctAll();
            }
        });

        setContentView(root);
    }

    private void correctAll() {
        final String text = editor.getText().toString();
        if (text.trim().isEmpty()) {
            Toast.makeText(this, "Digite um texto primeiro.", Toast.LENGTH_SHORT).show();
            return;
        }

        correctButton.setEnabled(false);
        correctButton.setText("Corrigindo…");
        engine = new SpellCorrectionEngine(this);
        engine.correct(text, new SpellCorrectionEngine.Callback() {
            @Override
            public void onSuccess(String correctedText, int replacements) {
                editor.setText(correctedText);
                editor.setSelection(correctedText.length());
                correctButton.setEnabled(true);
                correctButton.setText("Corrigir tudo");
                Toast.makeText(MainActivity.this,
                        replacements == 0 ? "Nenhuma correção necessária." : replacements + " correção(ões) aplicada(s).",
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onUnavailable(String reason) {
                correctButton.setEnabled(true);
                correctButton.setText("Corrigir tudo");
                Toast.makeText(MainActivity.this, reason, Toast.LENGTH_LONG).show();
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
