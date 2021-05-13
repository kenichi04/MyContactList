package com.example.mycontactlist;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Intents;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    private static final int REQUEST_ADD_CONTACT = 1;

    public void addContact(View view) {
        // Creates a new Intent to insert a contact
        Intent intent = new Intent(ContactsContract.Intents.Insert.ACTION);
        // Sets the MIME type to match the Contacts Provider
        intent.setType(ContactsContract.RawContacts.CONTENT_TYPE);

        /*
        * Inserts new data into the Intent. This data is passed to the
        * contacts app's Insert screen
        */
        // Inserts an email address
        intent.putExtra(Intents.Insert.EMAIL, "takahashi@shiro.name")
                /*
                * In this example, sets the email type to be a work email.
                * You can set other email types as necessary.
                */
                .putExtra(Intents.Insert.EMAIL_TYPE, CommonDataKinds.Email.TYPE_WORK)
                // Inserts a phone number
                .putExtra(Intents.Insert.PHONE, "000-000-0004")
                /*
                * In this example, sets the phone type to be a work phone.
                * You can set other phone types as necessary.
                */
                .putExtra(Intents.Insert.PHONE_TYPE, Phone.TYPE_WORK);

        startActivityForResult(intent, REQUEST_ADD_CONTACT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        ContactsFragment fragment =
                (ContactsFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.contacts_fragment);
        fragment.loadView();
    }
}