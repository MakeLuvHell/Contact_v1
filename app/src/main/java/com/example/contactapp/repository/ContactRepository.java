package com.example.contactapp.repository;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.example.contactapp.dao.ContactDao;
import com.example.contactapp.database.ContactDatabase;
import com.example.contactapp.model.Contact;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ContactRepository {
    private static final String TAG = "ContactRepository";
    private final ContactDao contactDao;
    private final LiveData<List<Contact>> allContacts;
    private final ExecutorService executorService;

    public ContactRepository(Application application) {
        ContactDatabase database = ContactDatabase.getInstance(application);
        contactDao = database.contactDao();
        allContacts = contactDao.getAllContacts();
        executorService = Executors.newFixedThreadPool(2);
    }

    public LiveData<List<Contact>> getAllContacts() {
        Log.d(TAG, "Fetching all contacts from database");
        return allContacts;
    }

    public LiveData<Contact> getContactById(int id) {
        Log.d(TAG, "Fetching contact with ID: " + id);
        return contactDao.getContactById(id);
    }

    public void insert(Contact contact) {
        Log.d(TAG, "Inserting new contact: " + contact.getName());
        executorService.execute(() -> {
            contactDao.insert(contact);
            Log.d(TAG, "Inserted contact: " + contact.getName());
        });
    }

    public void update(Contact contact) {
        Log.d(TAG, "Updating contact: " + contact.getName());
        executorService.execute(() -> {
            contactDao.update(contact);
            Log.d(TAG, "Updated contact: " + contact.getName());
        });
    }

    public void delete(Contact contact) {
        Log.d(TAG, "Deleting contact: " + contact.getName());
        executorService.execute(() -> {
            contactDao.delete(contact);
            Log.d(TAG, "Deleted contact: " + contact.getName());
        });
    }
}
