package com.uetm.firebase.realtimedatabase;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
    RecyclerView recyclerView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar =findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getResources().getString(R.string.app_name));
        toolbar.setTitleTextColor(Color.WHITE);
        FloatingActionButton floatingActionButton = findViewById(R.id.floatingActionButton);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveToFirebase();
            }
        });
         recyclerView = findViewById(R.id.recyclerView);
        retreiveDataFromFirebase();
    }

    void saveToFirebase(){
        // view is created for dialogue
        View view1 = LayoutInflater.from(MainActivity.this).inflate(R.layout.add_dilogue, null);
        TextInputLayout layoutTitle, layoutContent;
        layoutTitle = view1.findViewById(R.id.title_layout);
        layoutContent = view1.findViewById(R.id.content_layout);
        TextInputEditText title_input, content_input;
        title_input = view1.findViewById(R.id.title_input);
        content_input = view1.findViewById(R.id.content_input);
        // AlertDialog object is created and dialogue is showed
        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this)
                .setTitle("Add Notes").setView(view1)
                .setPositiveButton("Submit", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // error is showed if the entered data is empty.
                        if (Objects.requireNonNull(title_input.getText().toString().isEmpty())) {
                            layoutTitle.setError("Title is required.");
                        } else if (Objects.requireNonNull(content_input.getText().toString().isEmpty())) {
                            layoutContent.setError("Content is required.");
                        } else {
                            // if the fields have data the it will proceed
                            ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);
                            progressDialog.setMessage("Please wait!");
                            progressDialog.show();
                            NoteUtil note = new NoteUtil();
                            note.setTitle(title_input.getText().toString());
                            note.setContent(content_input.getText().toString());
                            // data is pushed to firebase database
                            firebaseDatabase.getReference().child("Notes").push().setValue(note).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void unused) {
                                    // if data is saved successfully then it will dismiss dialogue and show toast
                                    progressDialog.dismiss();
                                    dialog.dismiss();
                                    Toast.makeText(MainActivity.this, "Data saved!", Toast.LENGTH_SHORT).show();
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    // if failed then dismiss dialogue and show error in toast
                                    progressDialog.dismiss();
                                    dialog.dismiss();
                                    Toast.makeText(MainActivity.this, "Error occurred. Please try again!", Toast.LENGTH_SHORT).show();

                                }
                            });
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    // negative button cancel dialogue
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create();
        alertDialog.show();
    }
    void retreiveDataFromFirebase(){
        //firebase Database: to get values
        firebaseDatabase.getReference().child("Notes").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) { // if any value is changed this method is called
                // Note object is set to ArrayList below
                ArrayList<NoteUtil> noteArrayList= new ArrayList<>();
                for (DataSnapshot dataSnapshot: snapshot.getChildren()){
                    NoteUtil note=dataSnapshot.getValue(NoteUtil.class);
                    Objects.requireNonNull(note).setKey(dataSnapshot.getKey());
                    noteArrayList.add(note);
                }
                // adapter is created and Arraylist is passed
                NoteAdapter adapter = new NoteAdapter(MainActivity.this, noteArrayList);
                //adapter is set to recyclerview
                recyclerView.setAdapter(adapter);
                recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
                adapter.setOnItemClickListener(new NoteAdapter.OnItemClickListener() {
                    // when an item is clicked in a recyclerview-list this method is called
                    @Override
                    public void OnClick(NoteUtil note) {
                        // create a view for alertDilogue to edit data
                        View view1 = LayoutInflater.from(MainActivity.this).inflate(R.layout.add_dilogue, null);
                        TextInputLayout layoutTitle, layoutContent;
                        layoutTitle=view1.findViewById(R.id.title_layout);
                        layoutContent=view1.findViewById(R.id.content_layout);
                        TextInputEditText title_input, content_input;
                        title_input=view1.findViewById(R.id.title_input);
                        content_input=view1.findViewById(R.id.content_input);
                        title_input.setText(note.getTitle());
                        content_input.setText(note.getContent());

                        // ProgressDialog to show processing
                        ProgressDialog progressDialog=new ProgressDialog(MainActivity.this);

                        // initiating AlertDialog object and setting the view above
                        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this)
                                .setTitle("Edit Notes").setView(view1)
                                .setPositiveButton("Submit", new DialogInterface.OnClickListener() {
                                    // this is submit button listener to save data to firebase
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        //check for empty values if no title or content is entered show error
                                        if (Objects.requireNonNull(title_input.getText().toString().isEmpty())){
                                            layoutTitle.setError("Title is required.");
                                        }else if (Objects.requireNonNull(content_input.getText().toString().isEmpty())){
                                            layoutContent.setError("Content is required.");
                                        }else{
                                            // else part is executed when the values are entered
                                            progressDialog.setMessage("Saving!");
                                            progressDialog.show();
                                            // create Note object and set values to variables
                                            NoteUtil note1= new NoteUtil();
                                            note1.setTitle(title_input.getText().toString());
                                            note1.setContent(content_input.getText().toString());
                                            // call FirebaseDatabase and submit data to firebasedatabase
                                            firebaseDatabase.getReference().child("Notes").child(note.getKey()).setValue(note1).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void unused) {
                                                    // if value is successfully entered then distroy the Dialogs
                                                    progressDialog.dismiss();
                                                    dialog.dismiss();
                                                    Toast.makeText(MainActivity.this, "Data saved successfully!", Toast.LENGTH_SHORT).show();
                                                }
                                            }).addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    // if data is failed then show error message and destroy the dialogues
                                                    progressDialog.dismiss();
                                                    dialog.dismiss();
                                                    Toast.makeText(MainActivity.this, "Error occurred. Please try again!", Toast.LENGTH_SHORT).show();

                                                }
                                            });
                                        }
                                    }
                                })
                                .setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
                                    // Neutral button cancels dialogue
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                })
                                .setNegativeButton("Delete", new DialogInterface.OnClickListener() {
                                    // Negative Button is used to delete data from firebase
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        progressDialog.setMessage("Deleting...");
                                        progressDialog.show();
                                        //firebaseDatabase removeValue method delete a node with note.getKey()(key for each node)
                                        firebaseDatabase.getReference().child("Notes").child(note.getKey()).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void unused) {
                                                //if successful dismiss dialogue
                                                progressDialog.dismiss();
                                                dialog.dismiss();
                                                Toast.makeText(MainActivity.this, "Deleted successfully.", Toast.LENGTH_SHORT).show();

                                            }
                                        }).addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                // if failed show error and dismiss dialogue
                                                progressDialog.dismiss();
                                                dialog.dismiss();
                                                Toast.makeText(MainActivity.this, "Error occurred while Deleting.", Toast.LENGTH_SHORT).show();

                                            }
                                        });
                                    }
                                })
                                .create();
                        alertDialog.show();

                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

}


}

