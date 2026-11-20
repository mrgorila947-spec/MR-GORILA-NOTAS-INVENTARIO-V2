package com.mrgorila.ventas;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.view.Window;
import android.widget.Toast;

public class MainActivity extends Activity {
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setDatabaseEnabled(true);

        webView.addJavascriptInterface(new PrintBridge(), "AndroidPrint");
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // Conecta window.print() de la aplicación con el servicio de impresión de Android.
                view.evaluateJavascript(
                    "(function(){window.print=function(){AndroidPrint.print();};})();",
                    null
                );
            }
        });
        webView.setWebChromeClient(new WebChromeClient());
        setContentView(webView);

        if (savedInstanceState == null) {
            webView.loadUrl("file:///android_asset/index.html");
        } else {
            webView.restoreState(savedInstanceState);
        }
    }

    private class PrintBridge {
        @JavascriptInterface
        public void print() {
            runOnUiThread(() -> openAndroidPrintDialog());
        }
    }

    private void openAndroidPrintDialog() {
        if (webView == null) return;

        try {
            PrintManager printManager =
                (PrintManager) getSystemService(Context.PRINT_SERVICE);

            if (printManager == null) {
                Toast.makeText(this, "No se encontró el servicio de impresión de Android.",
                    Toast.LENGTH_LONG).show();
                return;
            }

            String jobName = "MR-GORILA-" + System.currentTimeMillis();
            PrintAttributes attributes = new PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.NA_LETTER)
                .setResolution(new PrintAttributes.Resolution(
                    "mr_gorila_pdf", "MR GORILA PDF", 300, 300))
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .build();

            printManager.print(
                jobName,
                webView.createPrintDocumentAdapter(jobName),
                attributes
            );
        } catch (Exception e) {
            Toast.makeText(this, "No se pudo preparar el PDF: " + e.getMessage(),
                Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (webView != null) webView.saveState(outState);
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}
