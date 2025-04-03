package com.example.appli20240829;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class PanierActivity extends AppCompatActivity {

    private static ArrayList<HashMap<String, String>> panierList = new ArrayList<>();
    private SimpleAdapter adapter;
    private ListView listViewPanier;
    private Button btnConfirmerPanier;
    private int customerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.panier_activity);

        // Récupérer le customerId de SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        customerId = sharedPreferences.getInt("customerId", -1);

        if (customerId == -1) {
            Toast.makeText(this, "Erreur : ID client non trouvé", Toast.LENGTH_SHORT).show();
        }

        initUI();
        setupListView();
        setupConfirmButton();
    }

    private void initUI() {
        listViewPanier = findViewById(R.id.listViewPanier);
        btnConfirmerPanier = findViewById(R.id.btnConfirmerPanier);
    }

    private void setupListView() {
        adapter = new SimpleAdapter(
                this,
                panierList,
                R.layout.panier_activity_item,
                new String[]{"title", "releaseYear"},
                new int[]{R.id.filmName, R.id.filmDate}
        );
        listViewPanier.setAdapter(adapter);
        listViewPanier.setOnItemClickListener((parent, view, position, id) -> supprimerFilmDuPanier(position));
    }

    private void setupConfirmButton() {
        btnConfirmerPanier.setOnClickListener(v -> {
            if (panierList.isEmpty()) {
                Toast.makeText(this, "Votre panier est vide", Toast.LENGTH_SHORT).show();
            } else {
                enregistrerLocations();
            }
        });
    }

    public static void ajouterFilmAuPanier(String title, String releaseYear, String filmId) {
        HashMap<String, String> film = new HashMap<>();
        film.put("title", title);
        film.put("releaseYear", releaseYear);
        film.put("filmId", filmId);
        panierList.add(film);
    }

    private void supprimerFilmDuPanier(int position) {
        if (position >= 0 && position < panierList.size()) {
            panierList.remove(position);
            adapter.notifyDataSetChanged();
            Toast.makeText(this, "Film supprimé du panier", Toast.LENGTH_SHORT).show();
        }
    }

    private void enregistrerLocations() {
        if (customerId == -1) {
            Toast.makeText(this, "Erreur : ID client non trouvé", Toast.LENGTH_SHORT).show();
            return;
        }

        RequestQueue queue = Volley.newRequestQueue(this);
        String currentDateTime = getCurrentDateTime();
        String returnDate = getReturnDate();

        for (HashMap<String, String> film : panierList) {
            String filmId = film.get("filmId");
            getInventoryId(queue, filmId, (inventoryId) -> {
                if (inventoryId != null) {
                    sendRentalRequest(queue, inventoryId, currentDateTime, returnDate);
                } else {
                    Toast.makeText(this, "Stock non disponible pour " + film.get("title"), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void getInventoryId(RequestQueue queue, String filmId, final InventoryCallback callback) {
        String url = DonneesPartagees.getURLConnexion() + "/toad/inventory/available/getById?id=" + filmId;

        Log.d("API_CALL", "Requête envoyée: " + url);

        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    Log.d("API_RESPONSE", "Réponse de getInventoryId: " + response);
                    try {
                        int inventoryId = Integer.parseInt(response.trim()); // Conversion directe
                        callback.onSuccess(inventoryId);
                    } catch (NumberFormatException e) {
                        Log.e("API_ERROR", "Erreur conversion int: " + e.getMessage());
                        callback.onSuccess(null);
                    }
                },
                error -> {
                    Log.e("API_ERROR", "Erreur de requête: " + error.getMessage());
                    callback.onSuccess(null);
                });

        queue.add(request);
    }

    private void sendRentalRequest(RequestQueue queue, int inventoryId, String currentDateTime, String returnDate) {
        String url = DonneesPartagees.getURLConnexion() + "/toad/rental/add";

        StringRequest request = new StringRequest(
                Request.Method.POST, url,
                response -> {
                    Toast.makeText(this, "Location enregistrée", Toast.LENGTH_SHORT).show();
                    panierList.clear();
                    adapter.notifyDataSetChanged();
                },
                error -> {
                    Toast.makeText(this, "Erreur lors de l'enregistrement", Toast.LENGTH_SHORT).show();
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("rental_date", currentDateTime);
                params.put("inventory_id", String.valueOf(inventoryId));
                params.put("customer_id", String.valueOf(customerId));
                params.put("return_date", returnDate);
                params.put("staff_id", "1");
                params.put("last_update", currentDateTime);
                return params;
            }
        };

        queue.add(request);
    }

    private String getCurrentDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }

    private String getReturnDate() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.DAY_OF_YEAR, 2);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(calendar.getTime());
    }

    interface InventoryCallback {
        void onSuccess(Integer inventoryId);
    }
}