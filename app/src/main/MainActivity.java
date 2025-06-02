package com.example.smsserver;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONObject;
import fi.iki.elonen.NanoHTTPD;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import android.util.Log;


public class MainActivity extends AppCompatActivity {
    SmsHttpServer server;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, 1);
        }

        try {
            server = new SmsHttpServer(8088, this);
            server.start();
            Log.i("SmsServer", "Servidor iniciado com sucesso na porta 8088");
            Toast.makeText(this, "Servidor iniciado na porta 8088", Toast.LENGTH_LONG).show();
            Toast.makeText(this, "---Samuel melo---",Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(this, "Erro ao iniciar servidor", Toast.LENGTH_LONG).show();
        }
    }

    static class SmsHttpServer extends NanoHTTPD {
        private final AppCompatActivity context;

        public SmsHttpServer(int port, AppCompatActivity context) {
            super(port);
            this.context = context;
        }

        public SmsHttpServer(String hostname, int port, AppCompatActivity context) {
            super(hostname, port);
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

                    return newFixedLengthResponse("SMS enviado para " + number);
                } catch (Exception e) {
                    return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Erro: " + e.getMessage());
                }
            }
            return newFixedLengthResponse("Servidor ativo");
        }
    }
}
