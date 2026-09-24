package com.nzr.corretor;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class MainActivity extends Activity {
    private EditText editor;
    private TextView keyboardStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateKeyboardStatus();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(18));
        root.setBackgroundColor(Color.rgb(247, 247, 250));

        TextView title = new TextView(this);
        title.setText("NZR Corretor");
        title.setTextSize(27);
        title.setTextColor(Color.rgb(76, 29, 149));
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Corrija a mensagem inteira antes de enviar no WhatsApp, Discord e outros apps.");
        subtitle.setTextSize(15);
        subtitle.setTextColor(Color.rgb(65, 65, 72));
        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subParams.topMargin = dp(6);
        subParams.bottomMargin = dp(14);
        root.addView(subtitle, subParams);

        keyboardStatus = new TextView(this);
        keyboardStatus.setTextSize(15);
        keyboardStatus.setPadding(dp(12), dp(10), dp(12), dp(10));
        keyboardStatus.setBackgroundColor(Color.WHITE);
        root.addView(keyboardStatus);

        Button enableKeyboard = new Button(this);
        enableKeyboard.setText("1. Ativar NZR Keyboard");
        enableKeyboard.setAllCaps(false);
        enableKeyboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS));
            }
        });
        LinearLayout.LayoutParams b1 = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52));
        b1.topMargin = dp(10);
        root.addView(enableKeyboard, b1);

        Button chooseKeyboard = new Button(this);
        chooseKeyboard.setText("2. Escolher NZR Keyboard");
        chooseKeyboard.setAllCaps(false);
        chooseKeyboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InputMethodManager imm =
                        (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null) imm.showInputMethodPicker();
            }
        });
        LinearLayout.LayoutParams b2 = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52));
        b2.topMargin = dp(8);
        root.addView(chooseKeyboard, b2);

        TextView help = new TextView(this);
        help.setText("Depois disso, abra qualquer conversa, digite normalmente e toque em “✓ Corrigir mensagem” na barra roxa do teclado antes de enviar.");
        help.setTextSize(14);
        help.setTextColor(Color.rgb(75, 75, 82));
        LinearLayout.LayoutParams helpParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        helpParams.topMargin = dp(10);
        helpParams.bottomMargin = dp(14);
        root.addView(help, helpParams);

        editor = new EditText(this);
        editor.setHint("Teste aqui: nao sei se vc vai conseguir corrigir isso mas quero tudo certo");
        editor.setGravity(Gravity.TOP | Gravity.START);
        editor.setTextSize(16);
        editor.setTextColor(Color.rgb(20, 20, 24));
        editor.setHintTextColor(Color.rgb(130, 130, 140));
        editor.setBackgroundColor(Color.WHITE);
        editor.setPadding(dp(12), dp(12), dp(12), dp(12));
        editor.setInputType(InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_FLAG_MULTI_LINE |
                InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        LinearLayout.LayoutParams editorParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        root.addView(editor, editorParams);

        Button correctButton = new Button(this);
        correctButton.setText("Corrigir tudo agora");
        correctButton.setAllCaps(false);
        correctButton.setTextSize(16);
        correctButton.setBackgroundColor(Color.rgb(124, 58, 237));
        correctButton.setTextColor(Color.WHITE);
        correctButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                correctEditor();
            }
        });
        LinearLayout.LayoutParams cb = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(54));
        cb.topMargin = dp(10);
        root.addView(correctButton, cb);

        TextView privacy = new TextView(this);
        privacy.setText("Correção local: o texto não é enviado para servidor.");
        privacy.setTextSize(12);
        privacy.setTextColor(Color.rgb(100, 100, 110));
        privacy.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pp.topMargin = dp(8);
        root.addView(privacy, pp);

        setContentView(root);
    }

    private void correctEditor() {
        String text = editor.getText().toString();
        if (text.trim().isEmpty()) {
            Toast.makeText(this, "Digite um texto primeiro.", Toast.LENGTH_SHORT).show();
            return;
        }

        LocalCorrectionEngine.Result result = LocalCorrectionEngine.correct(text);
        editor.setText(result.text);
        editor.setSelection(result.text.length());

        Toast.makeText(this,
                result.text.equals(text) ? "O texto já está corrigido." : "Texto corrigido.",
                Toast.LENGTH_SHORT).show();
    }

    private void updateKeyboardStatus() {
        if (keyboardStatus == null) return;

        if (isNzrKeyboardEnabled()) {
            keyboardStatus.setText("✓ NZR Keyboard está ativado");
            keyboardStatus.setTextColor(Color.rgb(21, 128, 61));
        } else {
            keyboardStatus.setText("NZR Keyboard ainda não está ativado");
            keyboardStatus.setTextColor(Color.rgb(185, 28, 28));
        }
    }

    private boolean isNzrKeyboardEnabled() {
        InputMethodManager imm =
                (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm == null) return false;

        List<InputMethodInfo> methods = imm.getEnabledInputMethodList();
        String packageName = getPackageName();
        for (InputMethodInfo info : methods) {
            if (packageName.equals(info.getPackageName()) &&
                    info.getServiceName().endsWith("NzrKeyboardService")) {
                return true;
            }
        }
        return false;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
