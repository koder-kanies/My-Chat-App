package com.koderkanies.mychatapp;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

public class ProfileActivity extends AppCompatActivity {

    private static final int RC_PHOTO_PICKER = 2;

    private FirebaseStorage mFirebaseStorage;
    private FirebaseDatabase mFirebaseDatabase;
    private StorageReference mProfilePhotoStorageReference;
    private DatabaseReference mProfileDatabaseReference;
    private FirebaseUser mFirebaseUser;
    private UserProfile userProfile;

    private String mUID;

    private ImageView mProfilePhoto;
    public String profileUrl;
    private TextView mUsername;
    private String usernameText;
    private TextView mEmail;
    private String emailText;
    private EditText mContact;
    private String contactText;
    private EditText mBio;
    private String bioText;
    private EditText mDOB;
    private String dobText;
    private EditText mGender;
    private String genderText;
    private RadioGroup mRadioGroup;

    private ImageView mEditIcon;
    private ImageView mSaveIcon;
    private ImageView mCancelIcon;

    public boolean isPhotoChanged;
    public boolean isPhotoExist;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        //To start this activity
        getIntent();

        //Firebase Storage access and instance
        mFirebaseStorage = FirebaseStorage.getInstance();
        mProfilePhotoStorageReference = mFirebaseStorage.getReference().child("profile_photos");

        //Firebase Realtime Database access and instance
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mProfileDatabaseReference = mFirebaseDatabase.getReference().child("profile");

        //Initialize firebase user to get user information
        mFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        if (mFirebaseUser != null) {

            //Get UID
            mUID = mFirebaseUser.getUid();

            //Initialize layout elements
            mProfilePhoto = findViewById(R.id.profileImage);
            mUsername = findViewById(R.id.username);
            mUsername.setText(mFirebaseUser.getDisplayName());
            mEmail = findViewById(R.id.email);
            mEmail.setText(mFirebaseUser.getEmail());
            mContact = findViewById(R.id.contact);
            mBio = findViewById(R.id.bio);
            mDOB = findViewById(R.id.DOB);
            mGender = findViewById(R.id.gender);
            mRadioGroup = findViewById(R.id.genderGroup);

            isPhotoChanged = false;

            mEditIcon = findViewById(R.id.editButton);
            mSaveIcon = findViewById(R.id.saveButton);
            mCancelIcon = findViewById(R.id.cancelButton);

            mProfileDatabaseReference.child(mUID).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    UserProfile mUserProfile = snapshot.getValue(UserProfile.class);
                    if(mUserProfile != null) {
                        //Log statements
                        Log.d("USER_D", "profile URL: " + mUserProfile.getPhotoUrl());
                        Log.d("USER_D", "username Text: " + mUserProfile.username);
                        Log.d("USER_D", "email Text: " + mUserProfile.getEmail());
                        Log.d("USER_D", "contact Text: " + mUserProfile.getContact());
                        Log.d("USER_D", "bio Text: " + mUserProfile.getBio());
                        Log.d("USER_D", "dob Text: " + mUserProfile.dob);
                        Log.d("USER_D", "gender Text: " + mUserProfile.gender);

                        //Load Attribute values
                        //load profile photo
                        if(mUserProfile.getPhotoUrl() != null)
                        {
                            Glide.with(getApplicationContext())
                                    .load(mUserProfile.getPhotoUrl())
                                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                                    .into(mProfilePhoto);

                            isPhotoExist = true;
                        }
                        else
                            isPhotoExist = false;
                        //Load username
                        if(mUserProfile.getUsername() != null)
                            mUsername.setText(mUserProfile.getUsername());

                        //Load email
                        if(mUserProfile.getEmail() != null)
                            mEmail.setText(mUserProfile.getEmail());

                        //Load Contact
                        if(mUserProfile.getContact() != null)
                            mContact.setText(mUserProfile.getContact());

                        //Load bio
                        if(mUserProfile.getBio() != null)
                            mBio.setText(mUserProfile.getBio());

                        //Load DOB
                        if (mUserProfile.getDob() != null)
                            mDOB.setText(mUserProfile.getDob());

                        //Load Gender
                        if(mUserProfile.getGender() != null)
                            mGender.setText(mUserProfile.getGender());

                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });

            mEditIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    //Enable and make required attributes enable and visible and vise versa
                    VisibleAndEnableSet(true);

                    //Change profile photo
                    mProfilePhoto.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            //Photo Picker
                            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                            intent.setType("image/*");
                            intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                            startActivityForResult(Intent.createChooser(intent, "Complete action using"), RC_PHOTO_PICKER);

                        }
                    });

                    //Group radio button listener
                    mRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(RadioGroup radioGroup, int i) {
                            Log.d("RADIO", "Radio id " + i);
                            if (i == R.id.male) {
                                String radioGender = "Male";
                                mGender.setText(radioGender);
                            } else if (i == R.id.female) {
                                String radioGender = "Female";
                                mGender.setText(radioGender);
                            }
                        }
                    });

                    //Save the now edited information
                    mSaveIcon.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {

                            //Set attributes enable and visible
                            VisibleAndEnableSet(false);

                            //get user details for push
                            userProfile = getUserValues();

                            //Push data to database
                            mProfileDatabaseReference
                                    .child(mUID)
                                    .setValue(userProfile);
                            Log.d("USER_NODE", "Updated data of pre-existed node is uploaded");
                        }
                    });

                    mCancelIcon.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            VisibleAndEnableSet(false);

                        }
                    });
                }
            });
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_PHOTO_PICKER && resultCode == RESULT_OK) {
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
        StorageReference photoRef = mProfilePhotoStorageReference.child(selectedImageUri.getLastPathSegment());

        //Upload file to firebase storage
        photoRef.putFile(selectedImageUri)
                .addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        progressDialog.dismiss();
                        Task<Uri> urlTask = taskSnapshot.getStorage().getDownloadUrl();
                        while (!urlTask.isSuccessful()) ;
                        Uri downloadUrl = urlTask.getResult();
                        if (downloadUrl != null) {
                            Glide.with(getApplicationContext())
                                    .load(downloadUrl)
                                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                                    .into(mProfilePhoto);
                            profileUrl = downloadUrl.toString();
                            /*UserProfile mUserProfile = new UserProfile(profileUrl,
                                    mUsername.getText().toString(),
                                    mEmail.getText().toString(),
                                    null, null, null, null);
                            FirebaseDatabase.getInstance().getReference()
                                    .child("profile/" + mUID)
                                    .setValue(mUserProfile);*/
                            isPhotoChanged = true;
                        } else {
                            Toast.makeText(ProfileActivity.this, "Photo Upload Failed!", Toast.LENGTH_SHORT).show();
                            isPhotoChanged = false;
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

    private void VisibleAndEnableSet(boolean bool) {

        mContact.setEnabled(bool);
        mBio.setEnabled(bool);
        mDOB.setEnabled(bool);
        mGender.setEnabled(bool);
        mProfilePhoto.setEnabled(bool);

        if (bool) {
            //when bool is true
            mEditIcon.setVisibility(View.GONE);
            mSaveIcon.setVisibility(View.VISIBLE);
            mCancelIcon.setVisibility(View.VISIBLE);
            mRadioGroup.setVisibility(View.VISIBLE);
        } else {
            //when bool is false
            mEditIcon.setVisibility(View.VISIBLE);
            mSaveIcon.setVisibility(View.GONE);
            mCancelIcon.setVisibility(View.GONE);
            mRadioGroup.setVisibility(View.GONE);
        }

    }

    private UserProfile getUserValues() {

        usernameText = mUsername.getText().toString();
        emailText = mEmail.getText().toString();

        /*if (isPhotoChanged) {
            FirebaseDatabase.getInstance().getReference().child("profile").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    for (DataSnapshot data : snapshot.getChildren()) {
                        if (data.getKey().equals("photoUrl")) {
                            profileUrl = data.getValue().toString();
                            Log.d("USER", "Profile Url:" + profileUrl);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        } */
        if(!isPhotoChanged && !isPhotoExist) {
            profileUrl = null;
            Log.d("USER", "Profile Url is NULL");
        }

        if (mBio.getText().toString().trim().length() == 0)
            bioText = null;
        else
            bioText = mBio.getText().toString();

        if (mContact.getText().toString().trim().length() == 0)
            contactText = null;
        else
            contactText = mContact.getText().toString();

        if (mDOB.getText().toString().trim().length() == 0)
            dobText = null;
        else
            dobText = mDOB.getText().toString();

        if (mGender.getText().toString().trim().length() == 0)
            genderText = null;
        else
            genderText = mGender.getText().toString();

        //Log statements
        Log.d("USER", "profile URL: " + profileUrl);
        Log.d("USER", "username Text: " + usernameText);
        Log.d("USER", "email Text: " + emailText);
        Log.d("USER", "contact Text: " + contactText);
        Log.d("USER", "bio Text: " + bioText);
        Log.d("USER", "dob Text: " + dobText);
        Log.d("USER", "gender Text: " + genderText);

        return new UserProfile(profileUrl, usernameText, emailText, contactText, bioText, dobText, genderText);
    }


}