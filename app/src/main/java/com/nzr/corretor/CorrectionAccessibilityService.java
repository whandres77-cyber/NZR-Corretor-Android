package com.nzr.corretor;

import android.accessibilityservice.AccessibilityService;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.Toast;

public class CorrectionAccessibilityService extends AccessibilityService {
    private WindowManager windowManager;
    private Button bubble;
    private boolean bubbleAdded = false;
    private AdvancedCorrectionEngine activeEngine;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        createBubble();
        refreshBubble();
    }

    private void createBubble() {
        bubble = new Button(this);
        bubble.setText("✓ Corrigir");
        bubble.setAllCaps(false);
        bubble.setTextSize(14);
        bubble.setTextColor(Color.WHITE);
        bubble.setBackgroundColor(Color.rgb(124, 58, 237));
        bubble.setPadding(dp(12), dp(6), dp(12), dp(6));
        bubble.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                correctFocusedField();
            }
        });
    }

    private WindowManager.LayoutParams makeParams() {
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.END | Gravity.BOTTOM;
        params.x = dp(10);
        params.y = dp(92);
        return params;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        refreshBubble();
    }

    private void refreshBubble() {
        AccessibilityNodeInfo node = getFocusedEditable();
        boolean shouldShow = node != null && node.isEditable() && node.isEnabled();

        if (shouldShow && !bubbleAdded) {
            try {
                windowManager.addView(bubble, makeParams());
                bubbleAdded = true;
            } catch (Exception ignored) {
            }
        } else if (!shouldShow && bubbleAdded) {
            try {
                windowManager.removeView(bubble);
            } catch (Exception ignored) {
            }
            bubbleAdded = false;
        }
    }

    private AccessibilityNodeInfo getFocusedEditable() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return null;

        AccessibilityNodeInfo focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
        if (focused != null && focused.isEditable()) return focused;

        return null;
    }

    private void correctFocusedField() {
        final AccessibilityNodeInfo node = getFocusedEditable();
        if (node == null || node.getText() == null) {
            Toast.makeText(this, "Toque primeiro no campo onde você está escrevendo.", Toast.LENGTH_SHORT).show();
            return;
        }

        final String original = node.getText().toString();
        if (original.trim().isEmpty()) {
            Toast.makeText(this, "Não há texto para corrigir.", Toast.LENGTH_SHORT).show();
            return;
        }

        bubble.setEnabled(false);
        bubble.setText("Corrigindo…");

        if (activeEngine != null) activeEngine.cancel();
        activeEngine = new AdvancedCorrectionEngine(this);
        activeEngine.correct(original, new AdvancedCorrectionEngine.Callback() {
            @Override
            public void onSuccess(String correctedText, int replacements, boolean online) {
                AccessibilityNodeInfo current = getFocusedEditable();
                if (current == null) {
                    restoreButton();
                    return;
                }

                Bundle args = new Bundle();
                args.putCharSequence(
                        AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                        correctedText);
                boolean ok = current.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);

                if (ok) {
                    Bundle selection = new Bundle();
                    selection.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, correctedText.length());
                    selection.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, correctedText.length());
                    current.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, selection);

                    Toast.makeText(
                            CorrectionAccessibilityService.this,
                            online ? "Texto revisado." : "Texto corrigido no modo local.",
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(
                            CorrectionAccessibilityService.this,
                            "Este aplicativo não permitiu substituir o texto automaticamente.",
                            Toast.LENGTH_LONG).show();
                }
                restoreButton();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(CorrectionAccessibilityService.this, message, Toast.LENGTH_LONG).show();
                restoreButton();
            }
        });
    }

    private void restoreButton() {
        bubble.setEnabled(true);
        bubble.setText("✓ Corrigir");
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public void onDestroy() {
        if (activeEngine != null) activeEngine.cancel();
        if (bubbleAdded && windowManager != null && bubble != null) {
            try {
                windowManager.removeView(bubble);
            } catch (Exception ignored) {
            }
        }
        bubbleAdded = false;
        super.onDestroy();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
