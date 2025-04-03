package com.example.appli20240829;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

public class FilmDetailsActivity extends AppCompatActivity {

    private static final String TAG = "FilmDetailsActivity"; // Logger Tag

    private TextView movieTitle, movieDescription, movieReleaseYear, movieLength, movieRating, movieSpecialFeatures;
    private Button btnAjouterPanier;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_film_details);

        initUI();

        int filmId = getIntent().getIntExtra("filmId", -1);
        if (filmId == -1) {
            showToast("ID du film manquant");
            Log.e(TAG, "ID du film est -1, fermeture de l'activité.");
            finish();
            return;
        }

        Log.d(TAG, "Film ID reçu: " + filmId);
        fetchFilmDetails(filmId);
        checkFilmAvailability(filmId);
    }

    private void initUI() {
        movieTitle = findViewById(R.id.movie_title);
        movieDescription = findViewById(R.id.movie_description);
        movieReleaseYear = findViewById(R.id.movie_release_year);
        movieLength = findViewById(R.id.movie_length);
        movieRating = findViewById(R.id.movie_rating);
        movieSpecialFeatures = findViewById(R.id.movie_special_features);
        btnAjouterPanier = findViewById(R.id.btnAjouterPanier);
    }

    private void fetchFilmDetails(int filmId) {
        String url = DonneesPartagees.getURLConnexion() + "/toad/film/getById?id=" + filmId;
        Log.d(TAG, "URL de récupération du film: " + url);

        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    Log.d(TAG, "Réponse JSON film: " + response.toString());
                    updateFilmDetailsUI(response);
                },
                error -> handleVolleyError(error, "Erreur de connexion lors de la récupération des détails du film"));

        queue.add(request);
    }

    private void updateFilmDetailsUI(JSONObject response) {
        movieTitle.setText("Titre : " + response.optString("title", "N/A"));
        movieDescription.setText("Description : " + response.optString("description", "N/A"));
        movieReleaseYear.setText("Année de sortie : " + response.optInt("releaseYear", 0));
        movieLength.setText("Durée : " + response.optInt("length", 0) + " minutes");
        movieRating.setText("Classification : " + response.optString("rating", "N/A"));
        movieSpecialFeatures.setText("Fonctionnalités spéciales : " + response.optString("specialFeatures", "N/A"));
    }

    private void checkFilmAvailability(int filmId) {
        String url = DonneesPartagees.getURLConnexion() + "/toad/inventory/available/getById?id=" + filmId;
        Log.d(TAG, "URL de vérification disponibilité: " + url);

        RequestQueue queue = Volley.newRequestQueue(this);

        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    Log.d(TAG, "Réponse brute de l'API: " + response);  // Afficher la réponse brute du serveur

                    // Vérifier si la réponse n'est pas vide
                    if (response == null || response.trim().isEmpty()) {
                        Log.e(TAG, "Réponse vide ou invalide du serveur.");
                        disableAddToCartButton();
                        return;
                    }

                    try {
                        // Vérifier si la réponse est un nombre valide
                        int inventoryId = Integer.parseInt(response.trim());
                        if (inventoryId > 0) {
                            enableAddToCartButton();
                        } else {
                            disableAddToCartButton();
                        }
                    } catch (NumberFormatException e) {
                        // Si la réponse n'est pas un entier valide, loguer l'erreur
                        Log.e(TAG, "Erreur lors de la conversion de la réponse en entier: " + e.getMessage());
                        disableAddToCartButton();
                    }
                },
                error -> {
                    // Log l'erreur Volley
                    Log.e(TAG, "Erreur de réseau: " + error.getMessage());
                    handleVolleyError(error, "Erreur de connexion lors de la vérification de la disponibilité");
                });

        queue.add(request);
    }




    private void handleAvailabilityResponse(JSONObject response) {
        try {
            if (response.has("inventoryId")) {
                int inventoryId = response.getInt("inventoryId");
                Log.d(TAG, "Inventory ID reçu: " + inventoryId);

                if (inventoryId > 0) {
                    enableAddToCartButton();
                } else {
                    disableAddToCartButton();
                }
            } else {
                Log.e(TAG, "Clé 'inventoryId' absente dans la réponse JSON.");
                disableAddToCartButton();
            }
        } catch (JSONException e) {
            Log.e(TAG, "Erreur JSON lors de la lecture de 'inventoryId': " + e.getMessage());
            disableAddToCartButton();
        }
    }

    private void enableAddToCartButton() {
        btnAjouterPanier.setEnabled(true);
        btnAjouterPanier.setAlpha(1.0f);
        btnAjouterPanier.setText("Ajouter au panier");
        btnAjouterPanier.setOnClickListener(v -> ajouterFilmAuPanier());
    }

    private void disableAddToCartButton() {
        btnAjouterPanier.setEnabled(false);
        btnAjouterPanier.setAlpha(0.5f);
        btnAjouterPanier.setText("Dvd non disponible");
    }

    private void ajouterFilmAuPanier() {
        String title = movieTitle.getText().toString().replace("Titre :", "").trim();
        String releaseYear = movieReleaseYear.getText().toString().replace("Année de sortie :", "").trim();
        int filmId = getIntent().getIntExtra("filmId", -1); // Récupération de l'ID du film

        if (filmId == -1) {
            Log.e(TAG, "ID du film invalide, impossible d'ajouter au panier.");
            showToast("Erreur : ID du film invalide.");
            return;
        }

        Log.d(TAG, "Ajout au panier: " + title + " (" + releaseYear + "), Film ID: " + filmId);
        PanierActivity.ajouterFilmAuPanier(title, releaseYear, String.valueOf(filmId));
        startActivity(new Intent(FilmDetailsActivity.this, PanierActivity.class));
    }


    private void handleVolleyError(VolleyError error, String message) {
        Log.e(TAG, message, error);

        if (error.networkResponse != null) {
            Log.e(TAG, "Code HTTP: " + error.networkResponse.statusCode);
            Log.e(TAG, "Réponse serveur: " + new String(error.networkResponse.data));
        } else {
            Log.e(TAG, "Aucune réponse du serveur.");
        }

        showToast("Erreur de connexion");
        disableAddToCartButton();
    }

    private void showToast(String message) {
        Toast.makeText(FilmDetailsActivity.this, message, Toast.LENGTH_SHORT).show();
    }
}
