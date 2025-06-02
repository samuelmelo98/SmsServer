package com.example.smsserver;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.text.format.Formatter;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class MainActivity extends AppCompatActivity {
    private SmsHttpServer server;
    private TextView ipText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ipText = findViewById(R.id.ipText);

        // Exibe o IP local na tela
        showDeviceIp();

        // Solicita permissão de envio de SMS, se necessário
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS}, 1);
        }

        // Inicia o serviço em foreground para manter app vivo em background
        Intent intent = new Intent(this, SmsService.class);
        ContextCompat.startForegroundService(this, intent);

        // Inicia o servidor HTTP
        try {
            server = new SmsHttpServer(8088, this);
            server.start();
            Log.i("SmsServer", "Servidor iniciado com sucesso na porta 8088");
            Toast.makeText(this, "Servidor HTTP na porta 8088", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(this, "Erro ao iniciar servidor HTTP: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e("SmsServer", "Erro ao iniciar servidor", e);
        }
    }

    private void showDeviceIp() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_WIFI_STATE)
                == PackageManager.PERMISSION_GRANTED) {
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null) {
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                String ip = Formatter.formatIpAddress(wifiInfo.getIpAddress());
                ipText.setText("Servidor: http://" + ip + ":8088");
            } else {
                ipText.setText("Erro ao obter IP");
            }
        } else {
            ipText.setText("Permissão de rede não concedida");
        }
    }

    static class SmsHttpServer extends NanoHTTPD {
        private final Context context;

        public SmsHttpServer(int port, Context context) {
            super(port);
            this.context = context;
        }

        @Override
        public Response serve(IHTTPSession session) {
            if (Method.POST.equals(session.getMethod()) && "/send-sms".equals(session.getUri())) {
                try {
                    Map<String, String> body = new HashMap<>();
                    session.parseBody(body);
                    String payload = body.get("postData");

                    JSONObject json = new JSONObject(payload);
                    String number = json.getString("number");
                    String message = json.getString("message");

                    SmsManager sms = SmsManager.getDefault();
                    sms.sendTextMessage(number, null, message, null, null);

                    return newFixedLengthResponse("✅ SMS enviado para " + number);
                } catch (Exception e) {
                    return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT,
                            "❌ Erro: " + e.getMessage());
                }
            }
            return newFixedLengthResponse("Servidor ativo");
        }
    }
}
