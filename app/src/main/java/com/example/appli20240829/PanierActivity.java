package com.example.appli20240829;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

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
    private Button btnRetourListe;
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

        btnRetourListe.setOnClickListener(v -> {
            startActivity(new Intent(PanierActivity.this, AfficherListeDvdsActivity.class));
            finish();
        });
    }

    private void initUI() {
        listViewPanier      = findViewById(R.id.listViewPanier);
        btnConfirmerPanier  = findViewById(R.id.btnConfirmerPanier);
        btnRetourListe      = findViewById(R.id.btnRetourListe);
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

    /** On reçoit maintenant aussi inventoryId **/
    public static void ajouterFilmAuPanier(String title, String releaseYear, String filmId, String inventoryId) {
        HashMap<String, String> film = new HashMap<>();
        film.put("title", title);
        film.put("releaseYear", releaseYear);
        film.put("filmId", filmId);
        film.put("inventoryId", inventoryId);
        panierList.add(film);
    }

    private void supprimerFilmDuPanier(int position) {
        if (position >= 0 && position < panierList.size()) {
            new AlertDialog.Builder(this)
                    .setTitle("Confirmation")
                    .setMessage("Voulez-vous vraiment supprimer ce DVD du panier ?")
                    .setPositiveButton("Oui", (dialog, which) -> {
                        panierList.remove(position);
                        adapter.notifyDataSetChanged();
                        Toast.makeText(this, "Film supprimé du panier", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Non", null)
                    .show();
        }
    }

    private void enregistrerLocations() {
        if (customerId == -1) {
            Toast.makeText(this, "Erreur : ID client non trouvé", Toast.LENGTH_SHORT).show();
            return;
        }

        RequestQueue queue         = Volley.newRequestQueue(this);
        String currentDateTime     = getCurrentDateTime();
        String returnDate          = getReturnDate();

        for (HashMap<String, String> film : panierList) {
            int inventoryId = Integer.parseInt(film.get("inventoryId"));
            sendRentalRequest(queue, inventoryId, currentDateTime, returnDate);
        }
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
                error -> Toast.makeText(this, "Erreur lors de l'enregistrement", Toast.LENGTH_SHORT).show()
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("rental_date",   currentDateTime);
                params.put("inventory_id",  String.valueOf(inventoryId));
                params.put("customer_id",   String.valueOf(customerId));
                params.put("return_date",   returnDate);
                params.put("staff_id",      "1");
                params.put("last_update",   currentDateTime);
                return params;
            }
        };

        queue.add(request);
    }

    private String getCurrentDateTime() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date());
    }

    private String getReturnDate() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, 2);
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(calendar.getTime());
    }
}
