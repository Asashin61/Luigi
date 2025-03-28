package com.example.appli20240829;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText editTextEmail, editTextPassword;
    private Button buttonLogin;
    private OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        editTextEmail = findViewById(R.id.editTextUsername);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonLogin = findViewById(R.id.buttonLogin);

        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = editTextEmail.getText().toString();
                String password = editTextPassword.getText().toString();
                fetchCustomerByEmail(email, password);
            }
        });
    }

    private void fetchCustomerByEmail(String email, String password) {
        String url = "http://10.0.2.2:8080/toad/customer/getByEmail?email=" + email;

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(LoginActivity.this, "Erreur de connexion", Toast.LENGTH_SHORT).show());
                Log.e("LoginActivity", "Échec de la connexion : ", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String responseBody = response.body().string();
                        Log.d("LoginActivity", "Réponse du serveur : " + responseBody);

                        if (responseBody == null || responseBody.isEmpty()) {
                            runOnUiThread(() ->
                                    Toast.makeText(LoginActivity.this, "Utilisateur non trouvé", Toast.LENGTH_SHORT).show());
                            return;
                        }

                        JSONObject jsonObject = new JSONObject(responseBody);

                        if (jsonObject.getString("password").equals(password)) {
                            int userId = jsonObject.getInt("customerId");
                            runOnUiThread(() -> {
                                Toast.makeText(LoginActivity.this, "Connexion réussie", Toast.LENGTH_SHORT).show();

                                SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putInt("customerId", userId);
                                editor.apply();

                                Intent intent = new Intent(LoginActivity.this, AfficherListeDvdsActivity.class);
                                intent.putExtra("USER_ID", userId);
                                startActivity(intent);
                                finish();
                            });
                        } else {
                            runOnUiThread(() ->
                                    Toast.makeText(LoginActivity.this, "Mot de passe incorrect", Toast.LENGTH_SHORT).show());
                        }
                    } catch (Exception e) {
                        Log.e("LoginActivity", "Erreur lors du traitement des données", e);
                        runOnUiThread(() ->
                                Toast.makeText(LoginActivity.this, "Erreur lors du traitement des données", Toast.LENGTH_SHORT).show());
                    }
                } else {
                    runOnUiThread(() ->
                            Toast.makeText(LoginActivity.this, "Erreur serveur", Toast.LENGTH_SHORT).show());
                    Log.e("LoginActivity", "Erreur serveur : " + response.code());
                }
            }
        });
    }
}
