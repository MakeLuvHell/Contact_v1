package com.example.contactapp.repository;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.example.contactapp.dao.GroupDao;
import com.example.contactapp.database.ContactDatabase;
import com.example.contactapp.model.Group;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GroupRepository {
    private static final String TAG = "GroupRepository";
    private final GroupDao groupDao;
    private final LiveData<List<Group>> allGroups;
    private final ExecutorService executorService;

    public GroupRepository(Application application) {
        ContactDatabase database = ContactDatabase.getInstance(application);
        groupDao = database.groupDao();
        allGroups = groupDao.getAllGroups();
        executorService = Executors.newFixedThreadPool(2);
    }

    public LiveData<List<Group>> getAllGroups() {
        Log.d(TAG, "Fetching all groups from database");
        return allGroups;
    }

    public void insert(Group group) {
        Log.d(TAG, "Inserting new group: " + group.getName());
        executorService.execute(() -> {
            groupDao.insert(group);
            Log.d(TAG, "Inserted group: " + group.getName());
        });
    }

    public void update(Group group) {
        Log.d(TAG, "Updating group: " + group.getName());
        executorService.execute(() -> {
            groupDao.update(group);
            Log.d(TAG, "Updated group: " + group.getName());
        });
    }

    public void delete(Group group) {
        Log.d(TAG, "Deleting group: " + group.getName());
        executorService.execute(() -> {
            groupDao.delete(group);
            Log.d(TAG, "Deleted group: " + group.getName());
        });
    }
}
