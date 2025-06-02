package com.example.smsserver;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import org.json.JSONObject;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import fi.iki.elonen.NanoHTTPD;

public class SmsService extends Service {
    private SmsHttpServer server;

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            server = new SmsHttpServer(8088);
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Cria canal de notificação (Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "sms_channel",
                    "Canal de envio de SMS",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        Notification notification = new NotificationCompat.Builder(this, "sms_channel")
                .setContentTitle("Servidor SMS ativo")
                .setContentText("Escutando na porta 8088")
                .setSmallIcon(R.drawable.sms)
                .build();

        startForeground(1, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        if (server != null) {
            server.stop();
        }
        super.onDestroy();
    }

    // NanoHTTPD interno
    private static class SmsHttpServer extends NanoHTTPD {
        public SmsHttpServer(int port) {
            super(port);
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

                    android.telephony.SmsManager sms = android.telephony.SmsManager.getDefault();
                    sms.sendTextMessage(number, null, message, null, null);

                    return newFixedLengthResponse("SMS enviado para " + number);
                } catch (Exception e) {
                    return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Erro: " + e.getMessage());
                }
            }
            return newFixedLengthResponse("Servidor SMS ativo");
        }
    }
}

