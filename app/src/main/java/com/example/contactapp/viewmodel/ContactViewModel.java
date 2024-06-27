package com.example.contactapp.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.contactapp.model.Contact;
import com.example.contactapp.repository.ContactRepository;

import java.util.List;

public class ContactViewModel extends AndroidViewModel {
    private final ContactRepository repository;
    private final LiveData<List<Contact>> allContacts;

    public ContactViewModel(@NonNull Application application) {
        super(application);
        repository = new ContactRepository(application);
        allContacts = repository.getAllContacts();
    }

    public LiveData<List<Contact>> getAllContacts() {
        return allContacts;
    }
    public LiveData<Contact> getContactById(int id) {
        return repository.getContactById(id);
    }

    public void insert(Contact contact) {
        repository.insert(contact);
    }

    public void update(Contact contact) {
        repository.update(contact);
    }

    public void delete(Contact contact) {
        repository.delete(contact);
    }
}

