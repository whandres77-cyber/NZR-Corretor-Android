package com.nzr.corretor;

import android.graphics.Color;
import android.inputmethodservice.InputMethodService;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

public class NzrKeyboardService extends InputMethodService {
    private boolean shift = false;
    private Button correctButton;

    @Override
    public View onCreateInputView() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(4), dp(6), dp(4), dp(6));
        root.setBackgroundColor(Color.rgb(28, 28, 34));

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);

        correctButton = key("✓ Corrigir mensagem", 1.0f);
        correctButton.setTextColor(Color.WHITE);
        correctButton.setBackgroundColor(Color.rgb(124, 58, 237));
        correctButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { correctWholeMessage(); }
        });
        toolbar.addView(correctButton);

        Button switcher = key("⌨", 0.22f);
        switcher.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (android.os.Build.VERSION.SDK_INT >= 28) {
                    switchToNextInputMethod(false);
                } else {
                    android.view.inputmethod.InputMethodManager imm =
                            (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    if (imm != null) imm.showInputMethodPicker();
                }
            }
        });
        toolbar.addView(switcher);

        root.addView(toolbar, rowParams(dp(48)));

        addLetterRow(root, new String[]{"q","w","e","r","t","y","u","i","o","p"});
        addLetterRow(root, new String[]{"a","s","d","f","g","h","j","k","l"});

        LinearLayout row3 = new LinearLayout(this);
        row3.setOrientation(LinearLayout.HORIZONTAL);

        Button shiftButton = key("⇧", 0.9f);
        shiftButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                shift = !shift;
                Toast.makeText(NzrKeyboardService.this,
                        shift ? "Maiúsculas ativadas" : "Maiúsculas desativadas",
                        Toast.LENGTH_SHORT).show();
            }
        });
        row3.addView(shiftButton);

        for (String l : new String[]{"z","x","c","v","b","n","m"}) {
            row3.addView(letterKey(l));
        }

        Button backspace = key("⌫", 0.9f);
        backspace.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                InputConnection ic = getCurrentInputConnection();
                if (ic != null) ic.deleteSurroundingText(1, 0);
            }
        });
        row3.addView(backspace);
        root.addView(row3, rowParams(dp(48)));

        LinearLayout accents = new LinearLayout(this);
        accents.setOrientation(LinearLayout.HORIZONTAL);
        for (String s : new String[]{"á","é","í","ó","ú","ã","õ","ç"}) {
            final String value = s;
            Button b = key(s, 1f);
            b.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    commit(value);
                }
            });
            accents.addView(b);
        }
        root.addView(accents, rowParams(dp(42)));

        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.HORIZONTAL);

        Button comma = key(",", 0.6f);
        comma.setOnClickListener(v -> commit(","));
        bottom.addView(comma);

        Button space = key("espaço", 2.5f);
        space.setOnClickListener(v -> commit(" "));
        bottom.addView(space);

        Button period = key(".", 0.6f);
        period.setOnClickListener(v -> commit("."));
        bottom.addView(period);

        Button enter = key("↵", 0.8f);
        enter.setOnClickListener(v -> {
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) {
                ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
                ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER));
            }
        });
        bottom.addView(enter);

        root.addView(bottom, rowParams(dp(50)));

        return root;
    }

    private void addLetterRow(LinearLayout root, String[] letters) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        for (String letter : letters) row.addView(letterKey(letter));
        root.addView(row, rowParams(dp(48)));
    }

    private Button letterKey(final String letter) {
        Button b = key(letter, 1f);
        b.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                String out = shift ? letter.toUpperCase() : letter;
                commit(out);
                if (shift) shift = false;
            }
        });
        return b;
    }

    private Button key(String text, float weight) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(16);
        b.setTextColor(Color.WHITE);
        b.setBackgroundColor(Color.rgb(52, 52, 60));
        b.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.MATCH_PARENT, weight);
        lp.setMargins(dp(2), dp(2), dp(2), dp(2));
        b.setLayoutParams(lp);
        return b;
    }

    private LinearLayout.LayoutParams rowParams(int height) {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, height);
    }

    private void commit(String text) {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) ic.commitText(text, 1);
    }

    private void correctWholeMessage() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) {
            Toast.makeText(this, "Nenhum campo de texto ativo.", Toast.LENGTH_SHORT).show();
            return;
        }

        ExtractedTextRequest request = new ExtractedTextRequest();
        request.hintMaxChars = 10000;
        request.hintMaxLines = 100;
        ExtractedText extracted = ic.getExtractedText(request, 0);

        if (extracted == null || extracted.text == null) {
            Toast.makeText(this,
                    "Este aplicativo não liberou o texto inteiro para o teclado.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        String original = extracted.text.toString();
        if (original.trim().isEmpty()) {
            Toast.makeText(this, "Não há texto para corrigir.", Toast.LENGTH_SHORT).show();
            return;
        }

        LocalCorrectionEngine.Result result = LocalCorrectionEngine.correct(original);

        if (result.text.equals(original)) {
            Toast.makeText(this, "O texto já está corrigido.", Toast.LENGTH_SHORT).show();
            return;
        }

        ic.beginBatchEdit();
        ic.setSelection(0, original.length());
        ic.commitText(result.text, 1);
        ic.endBatchEdit();

        Toast.makeText(this, "Texto corrigido por inteiro.", Toast.LENGTH_SHORT).show();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
