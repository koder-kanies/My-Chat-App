package com.koderkanies.mychatapp;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.data.model.User;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    public static final String ANONYMOUS = "anonymous";
    public static final int DEFAULT_MSG_LENGTH_LIMIT = 1000;
    private static final int RC_SIGN_IN = 1;
    private static final int RC_PHOTO_PICKER = 2;

    private EditText mMessageEditText;
    private String mUsername = ANONYMOUS;

    private MessageAdapter mMessageAdapter;
    private UserProfile mUserProfile;

    //to access database
    private FirebaseDatabase mFirebaseDatabase;
    //for each individual message reference
    private DatabaseReference mMessagesDatabaseReference;
    //To read from database
    private ChildEventListener mChildEventListener;
    //For authenticating users
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    //For storing files in storage

    private FirebaseStorage mFirebaseStorage;
    private StorageReference mChatPhotoStorageReference;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Accessing database and message reference
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseStorage = FirebaseStorage.getInstance();

        mMessagesDatabaseReference = mFirebaseDatabase.getReference().child("messages");
        mChatPhotoStorageReference = mFirebaseStorage.getReference().child("chat_photos");

        if(FirebaseAuth.getInstance().getCurrentUser() != null)
        {
            //Check if profile database of account already exist or not (i.e., first time login)
            FirebaseDatabase.getInstance()
                    .getReference("profile/" + FirebaseAuth.getInstance().getCurrentUser().getUid())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if(!snapshot.exists()) {
                        Log.d("USER_NODE", "Node does not exist!");
                        createNewProfileNode();
                    }
                    else {
                        Log.d("USER_NODE", "Node already exist!");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.d("USER_NODE", "ERROR!! Node Creation Terminated!");
                }
            });
        }
        else {
            Toast.makeText(MainActivity.this, "USER does not Exist!", Toast.LENGTH_SHORT).show();
        }

        // Initialize references to views
        ListView mMessageListView = findViewById(R.id.messageListView);
        ImageButton mPhotoPickerButton = findViewById(R.id.photoPickerButton);
        mMessageEditText = findViewById(R.id.messageEditText);
        ImageButton mSendButton = findViewById(R.id.sendButton);

        // Initialize message ListView and its adapter
        List<ChatMessage> chatMessages = new ArrayList<>();
        mMessageAdapter = new MessageAdapter(this, R.layout.item_message, chatMessages);
        mMessageListView.setAdapter(mMessageAdapter);

        // ImagePickerButton shows an image picker to upload a image for a message
        mPhotoPickerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO: Fire an intent to show an image picker

                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                startActivityForResult(Intent.createChooser(intent, "Complete action using"), RC_PHOTO_PICKER);
            }
        });

        // Enable Send button when there's text to send
        mMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0) {

                    //mSendButton.setClickable(true);
                    //mSendButton.setBackgroundColor(Color.GRAY);
                } else {

                    //mSendButton.setClickable(false);
                    //mSendButton.setBackgroundColor(Color.GREEN);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(DEFAULT_MSG_LENGTH_LIMIT)});

        // Send button sends a message and clears the EditText
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO: Send messages on click
                    ChatMessage chatMessage = new ChatMessage(mMessageEditText.getText().toString(), mUsername, null);
                    if(mMessageEditText.getText().toString().trim().length() != 0)
                    {
                        mMessagesDatabaseReference.push().setValue(chatMessage);
                        Log.d("STORE", "onClick: Message sent for storage");

                    }
                    // Clear input box
                    mMessageEditText.setText("");
            }
        });


        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    //user signed in
                    onSignedInInitialize(user.getDisplayName());
                } else {
                    //user signed out
                    onSignedOutCleanup();
                    List<AuthUI.IdpConfig> mAuthProviders = Arrays.asList(
                            new AuthUI.IdpConfig.EmailBuilder().build(),
                            new AuthUI.IdpConfig.GoogleBuilder().build()
                    );
                    startActivityForResult(AuthUI.getInstance().createSignInIntentBuilder()
                            .setIsSmartLockEnabled(false).setAvailableProviders(mAuthProviders).build(), RC_SIGN_IN);
                }
            }
        };
    }

    private void createNewProfileNode() {
        //new child node creation code fragment
        mUserProfile = new UserProfile(null,
                FirebaseAuth.getInstance().getCurrentUser().getDisplayName(),
                FirebaseAuth.getInstance().getCurrentUser().getEmail(),
                null,null,null,null);
        FirebaseDatabase.getInstance().getReference()
                .child("profile/" + FirebaseAuth.getInstance().getCurrentUser().getUid())
                .setValue(mUserProfile);

        Log.d("USER_NODE", "New Node created!");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(MainActivity.this, "You're now signed in.", Toast.LENGTH_SHORT).show();
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(MainActivity.this, "Your signed in canceled.", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else if (requestCode == RC_PHOTO_PICKER && resultCode == RESULT_OK) {
            Log.d("STORE", "Upload if code running");
            Uri selectedImageUri = data.getData();

            //Upload image in firebase storage and show in message view
            imageUpload(selectedImageUri);
        }
    }

    private void imageUpload(Uri selectedImageUri) {

        //Upload progress dialog
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Uploading Image...");
        progressDialog.show();

        //Get a reference to a specific photo in chat_photos
        StorageReference photoRef = mChatPhotoStorageReference.child(selectedImageUri.getLastPathSegment());

        //Upload file to firebase storage
        photoRef.putFile(selectedImageUri)
                .addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        progressDialog.dismiss();
                        Task<Uri> urlTask = taskSnapshot.getStorage().getDownloadUrl();
                        while (!urlTask.isSuccessful()) ;
                        Uri downloadUrl = urlTask.getResult();
                        //ChatMessage chatMessage;
                        if (downloadUrl != null) {
                            ChatMessage chatMessage = new ChatMessage(null, mUsername, downloadUrl.toString());
                            Log.d("URL", "PhotoURL: " + downloadUrl.toString());
                            mMessagesDatabaseReference.push().setValue(chatMessage);
                        } else {
                            Toast.makeText(MainActivity.this, "Photo Upload Failed!", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        Toast.makeText(getApplicationContext(), "Failed to upload", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {
                        double progressPercentage = (100.00 * snapshot.getBytesTransferred() / snapshot.getTotalByteCount());
                        progressDialog.setMessage("Uploaded: " + (int) progressPercentage + "%");
                    }
                });
    }

    private void attachDatabaseReadListener() {
        if (mChildEventListener == null) {
            mChildEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                    ChatMessage chatMessage = snapshot.getValue(ChatMessage.class);
                    mMessageAdapter.add(chatMessage);
                }

                @Override
                public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                }

                @Override
                public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                }

                @Override
                public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                }
            };
            //Give reference to the database of the listener
            mMessagesDatabaseReference.addChildEventListener(mChildEventListener);
        }
    }

    private void detachDatabaseReadListener() {
        if (mChildEventListener != null) {
            mMessagesDatabaseReference.removeEventListener(mChildEventListener);
            mChildEventListener = null;
        }
    }

    private void onSignedInInitialize(String username) {
        mUsername = username;
        attachDatabaseReadListener();
    }

    private void onSignedOutCleanup() {
        mUsername = ANONYMOUS;
        mMessageAdapter.clear();
        detachDatabaseReadListener();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.sign_out_menu) {
            //sign out
            AuthUI.getInstance().signOut(this);
            return true;
        }
        else if (item.getItemId() == R.id.profile_menu) {
            //profile activity
            startActivity(new Intent(MainActivity.this, ProfileActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onResume() {
        super.onResume();
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        detachDatabaseReadListener();
        mMessageAdapter.clear();
    }
}