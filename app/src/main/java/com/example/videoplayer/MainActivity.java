package com.example.videoplayer;

import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.WindowManager;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.ui.PlayerView;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private ExoPlayer player;
    private PlayerView playerView;
    private RequestQueue requestQueue;
    private static final String API_URL = "https://ujoniobvifxkbhhfopwu.supabase.co/rest/v1/room";
    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InVqb25pb2J2aWZ4a2JoaGZvcHd1Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3Mzk4ODU2NDEsImV4cCI6MjA1NTQ2MTY0MX0.aWD395KJsOXsEZKoV-xWx512ypVkSMkMPhFVOa7IYhc";
    private Handler handler;
    private static final long UPDATE_INTERVAL = 1000; // 1 second
    private AudioManager audioManager;
    private WindowManager.LayoutParams layoutParams;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize ExoPlayer
        player = new ExoPlayer.Builder(this).build();
        playerView = findViewById(R.id.playerView);
        playerView.setPlayer(player);

        // Initialize Volley
        requestQueue = Volley.newRequestQueue(this);

        // Initialize system services
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        layoutParams = getWindow().getAttributes();

        // Initialize handler for periodic updates
        handler = new Handler(Looper.getMainLooper());

        fetchVideoData();
        startPeriodicUpdates();
    }

    private void startPeriodicUpdates() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                fetchVideoData();
                handler.postDelayed(this, UPDATE_INTERVAL);
            }
        }, UPDATE_INTERVAL);
    }

    private void fetchVideoData() {
        String url = API_URL + "?room_id=eq.1";

        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        try {
                            if (response.length() > 0) {
                                JSONObject roomData = response.getJSONObject(0);
                                
                                // Handle video status
                                int videoStatus = roomData.getInt("video_status");
                                if (videoStatus == 1 && !player.isPlaying()) {
                                    player.play();
                                } else if (videoStatus == 0 && player.isPlaying()) {
                                    player.pause();
                                }
                                
                                // Update brightness
                                int light = roomData.getInt("light");
                                float brightness = light / 100f;
                                layoutParams.screenBrightness = brightness;
                                getWindow().setAttributes(layoutParams);
                                
                                // Update volume
                                int sound = roomData.getInt("sound");
                                int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                                int volume = (int) ((sound / 100f) * maxVolume);
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
                                
                                // Set video URL if not already set
                                if (player.getMediaItemCount() == 0) {
                                    String videoUrl = roomData.getString("movie_url");
                                    MediaItem mediaItem = MediaItem.fromUri(videoUrl);
                                    player.setMediaItem(mediaItem);
                                    player.prepare();
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("Prefer", "return=representation");
                headers.put("apikey", API_KEY);
                return headers;
            }
        };

        requestQueue.add(request);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
        }
        handler.removeCallbacksAndMessages(null);
    }
}