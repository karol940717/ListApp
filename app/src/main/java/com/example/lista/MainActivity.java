package com.example.lista;

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "list_prefs";
    private static final String WORDS_KEY = "words_list";
    private ArrayList<String> words;
    private SharedPreferences prefs;
    ArrayAdapter<String> adapter;
    DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
    DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FirebaseApp.initializeApp(this);
        readFromFirebase();

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        words = new ArrayList<>();
        //Odzyskaj zapisaną listę słów
        words.addAll(prefs.getStringSet(WORDS_KEY, new HashSet<>()));
        adapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, words);


        ListView listView = findViewById(R.id.listView);
        FloatingActionButton fab = findViewById(R.id.floatingActionButton);
        FloatingActionButton fab2 = findViewById(R.id.floatingActionButton2);

        listView.setAdapter(adapter);

        listView.setOnItemLongClickListener((adapterView, view, position, id) -> {
            String value = adapter.getItem(position);
            adapter.remove(value);
            makeToast("Usunięto: " + value);
            // Pobierz referencję do węzła zawierającego elementy listy
            DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("words");
            reference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                        if (Objects.equals(childSnapshot.getValue(), value)) {
                            childSnapshot.getRef().removeValue();
                            adapter.remove(value);
                            adapter.notifyDataSetChanged();
                            break;
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                }
            });
            return true;
        });

        fab.setOnClickListener(view -> {
            // Wyświetl pole tekstowe
            final EditText editText = new EditText(MainActivity.this);
            // Otwórz klawiaturę
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
            // Ustaw kursor w polu tekstowym
            editText.requestFocus();

            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Wpisz wyraz")
                    .setView(editText)
                    .setPositiveButton("OK", (dialogInterface, i) -> {
                        // Pobierz wpisany wyraz
                        if (String.valueOf(editText.getText()).isEmpty()) {
                            InputMethodManager imm1 = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm1.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
                            makeToast("Wpisz produkt");
                        } else {
                            words.add(String.valueOf(editText.getText()));
                            // Odśwież widok listy
                            adapter.notifyDataSetChanged();
                            // Zamknij klawiaturę
                            InputMethodManager imm1 = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm1.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
                            makeToast("Dodano: " + editText.getText());
                        }
                        saveToFirebase(String.valueOf(editText.getText()));

                        InputMethodManager imm1 = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm1.hideSoftInputFromWindow(editText.getWindowToken(), 0);
                    })
                    .setNegativeButton("Anuluj", (dialogInterface, i) -> {
                        // Zamknij klawiaturę
                        InputMethodManager imm12 = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm12.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
                    })
                    .show();
        });
        fab2.setOnClickListener(view -> {
            adapter.notifyDataSetChanged();
            rootRef.removeValue();
            words.clear();
            makeToast("Wyczyszczono listę");
        });
    }

    private void saveToFirebase(final String word) {
        mDatabase.child("words").push().setValue(word);
    }

    private void readFromFirebase() {
        mDatabase.child("words").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                words.clear();
                for (DataSnapshot wordSnapshot : dataSnapshot.getChildren()) {
                    String word = wordSnapshot.getValue(String.class);
                    words.add(word);
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "Błąd odczytu.", error.toException());
            }
        });
    }

    Toast toast;

    private void makeToast(String s) {
        if (toast != null) toast.cancel();
        toast = Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT);
        toast.show();
    }

    public void saveList() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putStringSet(WORDS_KEY, new HashSet<>(words));
        editor.apply();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Zapisz listę słów
        saveList();

    }
}