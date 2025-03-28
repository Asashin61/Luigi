package com.example.appli20240829;

import android.app.ProgressDialog;
import android.content.Intent;
import android.database.MatrixCursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class AfficherListeDvdsActivity extends AppCompatActivity {

    private static final String API_URL = "http://10.0.2.2:8080/toad/film/all";

    private SimpleCursorAdapter adapter;
    private MatrixCursor dvdCursor;
    private ListView listViewDvds;
    private ProgressDialog progressDialog;  // Ajout du dialogue de chargement

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_afficherlistedvds);

        // Initialisation des vues
        setupUI();

        // Récupération des films depuis l'API
        new FetchFilmListTask().execute(API_URL);
    }

    /**
     * Initialise l'interface utilisateur.
     */
    private void setupUI() {
        // Initialisation des boutons
        Button btnPanier = findViewById(R.id.btnNavigate);
        Button btnDeconnexion = findViewById(R.id.btnDeconnexion);

        btnPanier.setOnClickListener(v -> navigateTo(PanierActivity.class));
        btnDeconnexion.setOnClickListener(v -> logoutUser());

        // Initialisation de la liste des DVDs
        setupListView();
    }

    /**
     * Configure le ListView et l'adapter pour afficher les films.
     */
    private void setupListView() {
        String[] columns = {"_id", "title", "releaseYear"};
        dvdCursor = new MatrixCursor(columns);

        String[] from = {"title", "releaseYear"};
        int[] to = {R.id.filmName, R.id.filmDate};
        adapter = new SimpleCursorAdapter(this, R.layout.activity_afficherlisteitemsdvds, dvdCursor, from, to, 0);

        listViewDvds = findViewById(R.id.listView);
        listViewDvds.setAdapter(adapter);
        listViewDvds.setTextFilterEnabled(true);

        listViewDvds.setOnItemClickListener((parent, view, position, id) -> openFilmDetails(position));
    }

    /**
     * Ouvre l'écran des détails d’un film sélectionné.
     */
    private void openFilmDetails(int position) {
        dvdCursor.moveToPosition(position);
        int filmId = dvdCursor.getInt(dvdCursor.getColumnIndex("_id"));

        Intent intent = new Intent(AfficherListeDvdsActivity.this, FilmDetailsActivity.class);
        intent.putExtra("filmId", filmId);
        startActivity(intent);
    }

    /**
     * Redirige l'utilisateur vers une autre activité.
     */
    private void navigateTo(Class<?> destination) {
        Intent intent = new Intent(AfficherListeDvdsActivity.this, destination);
        startActivity(intent);
    }

    /**
     * Déconnecte l'utilisateur et retourne à l'écran de connexion.
     */
    private void logoutUser() {
        Intent intent = new Intent(AfficherListeDvdsActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    /**
     * Tâche asynchrone pour récupérer la liste des films depuis l'API.
     */
    private class FetchFilmListTask extends AsyncTask<String, Void, JSONArray> {

        @Override
        protected void onPreExecute() {
            // Affichage d’un message de chargement
            progressDialog = new ProgressDialog(AfficherListeDvdsActivity.this);
            progressDialog.setMessage("Chargement des films...");
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override
        protected JSONArray doInBackground(String... urls) {
            StringBuilder result = new StringBuilder();

            try {
                URL url = new URL(urls[0]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                Log.d("API_CALL", "Connexion à l'API réussie");

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                reader.close();

                Log.d("API_CALL", "Réponse API : " + result);

                return new JSONArray(result.toString());

            } catch (Exception e) {
                Log.e("API_CALL", "Erreur lors de l'appel API", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(JSONArray films) {
            // Ferme le dialogue de chargement
            if (progressDialog.isShowing()) {
                progressDialog.dismiss();
            }

            if (films == null) {
                Toast.makeText(AfficherListeDvdsActivity.this, "Erreur de connexion à l'API", Toast.LENGTH_SHORT).show();
                return;
            }

            populateListView(films);
        }
    }

    /**
     * Remplit la liste des DVDs avec les données récupérées.
     */
    private void populateListView(JSONArray films) {
        try {
            dvdCursor.close();
            dvdCursor = new MatrixCursor(new String[]{"_id", "title", "releaseYear"});

            for (int i = 0; i < films.length(); i++) {
                JSONObject film = films.getJSONObject(i);
                int filmId = film.getInt("filmId");
                String title = film.getString("title");
                String releaseYear = film.getString("releaseYear");

                dvdCursor.addRow(new Object[]{filmId, title, releaseYear});
            }

            adapter.changeCursor(dvdCursor);
            Log.d("API_CALL", "Liste mise à jour avec succès");

        } catch (JSONException e) {
            Log.e("API_CALL", "Erreur de parsing JSON", e);
        }
    }
}
