package com.bank.bayan.transaction;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "transfer_app.db";
    private static final int DATABASE_VERSION = 1;

    // Table name
    private static final String TABLE_CONTACTS = "contacts";

    // Column names
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_ACCOUNT_NUMBER = "account_number";
    private static final String COLUMN_BALANCE = "balance";
    private static final String COLUMN_IS_FAVORITE = "is_favorite";

    // Create table SQL
    private static final String CREATE_TABLE_CONTACTS =
            "CREATE TABLE " + TABLE_CONTACTS + "(" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    COLUMN_NAME + " TEXT NOT NULL," +
                    COLUMN_ACCOUNT_NUMBER + " TEXT NOT NULL," +
                    COLUMN_BALANCE + " REAL DEFAULT 0," +
                    COLUMN_IS_FAVORITE + " INTEGER DEFAULT 0" +
                    ")";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_CONTACTS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CONTACTS);
        onCreate(db);
    }

    public long addContact(TransactionActivity.Contact contact) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, contact.getName());
        values.put(COLUMN_ACCOUNT_NUMBER, contact.getAccountNumber());
        values.put(COLUMN_BALANCE, contact.getBalance());
        values.put(COLUMN_IS_FAVORITE, contact.isFavorite() ? 1 : 0);

        long id = db.insert(TABLE_CONTACTS, null, values);
        db.close();

        return id;
    }

    public List<TransactionActivity.Contact> getAllContacts() {
        List<TransactionActivity.Contact> contactList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_CONTACTS + " ORDER BY " + COLUMN_NAME;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                String name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME));
                String accountNumber = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ACCOUNT_NUMBER));
                double balance = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_BALANCE));
                boolean isFavorite = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_FAVORITE)) == 1;

                TransactionActivity.Contact contact = new TransactionActivity.Contact(name, accountNumber, balance, isFavorite);
                contactList.add(contact);

            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();

        return contactList;
    }

    public List<TransactionActivity.Contact> getFavoriteContacts() {
        List<TransactionActivity.Contact> favoriteList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_CONTACTS +
                " WHERE " + COLUMN_IS_FAVORITE + " = 1" +
                " ORDER BY " + COLUMN_NAME;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                String name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME));
                String accountNumber = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ACCOUNT_NUMBER));
                double balance = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_BALANCE));

                TransactionActivity.Contact contact = new TransactionActivity.Contact(name, accountNumber, balance, true);
                favoriteList.add(contact);

            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();

        return favoriteList;
    }

    public List<TransactionActivity.Contact> searchContactsByName(String searchTerm) {
        List<TransactionActivity.Contact> searchResults = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_CONTACTS +
                " WHERE " + COLUMN_NAME + " LIKE ?";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, new String[]{"%" + searchTerm + "%"});

        if (cursor.moveToFirst()) {
            do {
                String name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME));
                String accountNumber = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ACCOUNT_NUMBER));
                double balance = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_BALANCE));
                boolean isFavorite = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_FAVORITE)) == 1;

                TransactionActivity.Contact contact = new TransactionActivity.Contact(name, accountNumber, balance, isFavorite);
                searchResults.add(contact);

            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();

        return searchResults;
    }

    public int updateContactBalance(String accountNumber, double newBalance) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COLUMN_BALANCE, newBalance);

        int rowsAffected = db.update(TABLE_CONTACTS, values,
                COLUMN_ACCOUNT_NUMBER + " = ?",
                new String[]{accountNumber});
        db.close();

        return rowsAffected;
    }

    public void deleteAllContacts() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_CONTACTS, null, null);
        db.close();
    }

    public int getContactCount() {
        String countQuery = "SELECT COUNT(*) FROM " + TABLE_CONTACTS;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);

        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }

        cursor.close();
        db.close();

        return count;
    }
}