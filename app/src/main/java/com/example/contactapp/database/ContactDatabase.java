package com.example.contactapp.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.contactapp.model.Contact;
import com.example.contactapp.model.Group;
import com.example.contactapp.dao.ContactDao;
import com.example.contactapp.dao.GroupDao;

@Database(entities = {Contact.class, Group.class}, version = 7, exportSchema = false)
public abstract class ContactDatabase extends RoomDatabase {
    private static volatile ContactDatabase instance;
    public abstract ContactDao contactDao();
    public  abstract GroupDao groupDao();

    // 使用synchronized保证线程安全
    public static synchronized ContactDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            ContactDatabase.class, "contact_database")
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }

}
