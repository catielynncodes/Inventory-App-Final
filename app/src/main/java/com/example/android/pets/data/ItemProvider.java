package com.example.android.pets.data;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import static com.example.android.pets.data.ItemContract.CONTENT_AUTHORITY;
import static com.example.android.pets.data.ItemContract.PATH_ITEMS;

/**
 * {@link ContentProvider} for inventory app.
 */
public class ItemProvider extends ContentProvider {

    /** Tag for the log messages */
    public static final String LOG_TAG = ItemProvider.class.getSimpleName();

    /** Database helper object **/
    private ItemDbHelper mDbHelper;

    /**
     * Initialize the provider and the database helper object.
     */
    @Override
    public boolean onCreate() {
        mDbHelper = new ItemDbHelper(getContext());
        return true;
    }

    /** URI matcher code for the content URI for the items table */
    private static final int ITEMS = 100;

    /** URI matcher code for the content URI for a single item in the items table */
    private static final int ITEM_ID = 101;

    /**
     * UriMatcher object to match a content URI to a corresponding code.
     * The input passed into the constructor represents the code to return for the root URI.
     * It's common to use NO_MATCH as the input for this case.
     */
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    // Static initializer. This is run the first time anything is called from this class.
    static {
        // The calls to addURI() go here, for all of the content URI patterns that the provider
        // should recognize. All paths added to the UriMatcher have a corresponding code to return
        // when a match is found.

        sUriMatcher.addURI(CONTENT_AUTHORITY, PATH_ITEMS, ITEMS);
        sUriMatcher.addURI(CONTENT_AUTHORITY, PATH_ITEMS + "/#", ITEM_ID);
    }

    // The MIME type of the {@link #CONTENT_URI} for a list of items.
    public static final String CONTENT_LIST_TYPE =
            ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_ITEMS;

    // The MIME type of the {@link #CONTENT_URI} for a single item.
    public static final String CONTENT_ITEM_TYPE =
            ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_ITEMS;


    // Perform the query for the given URI. Use the given projection, selection, selection arguments, and sort order.
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {

        // Get readable database
        SQLiteDatabase database = mDbHelper.getReadableDatabase();

        // This cursor will hold the result of the query
        Cursor cursor;

        // Figure out if the URI matcher can match the URI to a specific code
        int match = sUriMatcher.match(uri);
        switch (match) {
            case ITEMS:
                // For the ITEMS code, query the items table directly with the given
                // projection, selection, selection arguments, and sort order. The cursor
                // could contain multiple rows of the pets table.
                cursor = database.query(ItemContract.ItemEntry.TABLE_NAME, projection, selection,
                        selectionArgs, null, null, sortOrder);
                break;
            case ITEM_ID:
                // For the ITEM_ID code, extract out the ID from the URI.
                // For an example URI such as "content://com.example.android.pets/pets/3",
                // the selection will be "_id=?" and the selection argument will be a
                // String array containing the actual ID of 3 in this case.
                //
                // For every "?" in the selection, we need to have an element in the selection
                // arguments that will fill in the "?". Since we have 1 question mark in the
                // selection, we have 1 String in the selection arguments' String array.
                selection = ItemContract.ItemEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };

                // This will perform a query on the pets table where the _id equals 3 to return a
                // Cursor containing that row of the table.
                cursor = database.query(ItemContract.ItemEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Cannot query unknown URI " + uri);
        }
        return cursor;
    }

    // Insert new data into the provider with the given ContentValues.
    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        final int match = sUriMatcher.match(uri);
        switch (match) {

            case ITEMS:
                return insertItem(uri, contentValues);
            default:
                throw new IllegalArgumentException("Insertion is not supported for " + uri);
        }
    }

    /**
     * Insert an item into the database with the given content values. Return the new content URI
     * for that specific row in the database.
     */
    private Uri insertItem(Uri uri, ContentValues values) {
        // Check that the name is not null
        String name = values.getAsString(ItemContract.ItemEntry.COLUMN_ITEM_NAME);
        if (name==null || name.isEmpty()) {
            throw new IllegalArgumentException("Item requires a name");
        }

        // Check that the price is not null (not able to have negative input)
        String price = values.getAsString(ItemContract.ItemEntry.COLUMN_ITEM_PRICE);
        if (price==null || price.isEmpty()) {
            throw new IllegalArgumentException("Item requires a price amount");
        }

        // Check that the quantity is not null
        String quantity = values.getAsString(ItemContract.ItemEntry.COLUMN_ITEM_QUANTITY);
        if (quantity==null || quantity.isEmpty()) {
            throw new IllegalArgumentException("Item requires a quantity amount");
        }

        // Check that the supplierName is not null
        String supplierName = values.getAsString(ItemContract.ItemEntry.COLUMN_ITEM_SUPPLIER_NAME);
        if (supplierName==null || supplierName.isEmpty()) {
            throw new IllegalArgumentException("Item requires a supplier name");
        }

        // Check that the supplierPhone is not null
        String supplierPhone = values.getAsString(ItemContract.ItemEntry.COLUMN_ITEM_SUPPLIER_PHONE);
        if (supplierPhone==null || supplierPhone.isEmpty()) {
            throw new IllegalArgumentException("Item requires a supplier phone number");
        }

        // Get writable database
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        // Insert the new item with the given values
        long id = database.insert(ItemContract.ItemEntry.TABLE_NAME, null, values);

        // If the ID is -1, then the insertion failed. Log an error and return null.
        if (id == -1) {
            Log.e(LOG_TAG, "Failed to insert row for " + uri);
            return null;
        }

        // return the new URI with the ID (of newly inserted row) appended to the end of it
        return ContentUris.withAppendedId(uri, id);
    }

    // Updates the data at the given selection and selection arguments, with the new ContentValues.
    @Override
    public int update(Uri uri, ContentValues contentValues, String selection,
                      String[] selectionArgs) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case ITEMS:
                return updateItem(uri, contentValues, selection, selectionArgs);
            case ITEM_ID:
                // For the ITEM_ID code, extract out the ID from the URI,
                // so we know which row to update. Selection will be "_id=?" and selection
                // arguments will be a String array containing the actual ID.
                selection = ItemContract.ItemEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };
                return updateItem(uri, contentValues, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Update is not supported for " + uri);
        }
    }

    /**
     * Update pets in the database with the given content values. Apply the changes to the rows
     * specified in the selection and selection arguments (which could be 0 or 1 or more items).
     * Return the number of rows that were successfully updated.
     */
    private int updateItem(Uri uri, ContentValues values, String selection, String[] selectionArgs) {

        // If the {@link ItemEntry#COLUMN_ITEM_NAME} key is present,
        // check that the name value is not null.
        if (values.containsKey(ItemContract.ItemEntry.COLUMN_ITEM_NAME)) {
            String name = values.getAsString(ItemContract.ItemEntry.COLUMN_ITEM_NAME);
            if (name == null) {
                throw new IllegalArgumentException("Item requires a name");
            }
        }

        // If the {@link ItemEntry#COLUMN_ITEM_PRICE} key is present,
        // check that the price value is valid.
        if (values.containsKey(ItemContract.ItemEntry.COLUMN_ITEM_PRICE)) {
            String price = values.getAsString(ItemContract.ItemEntry.COLUMN_ITEM_PRICE);
            if (price==null) {
                throw new IllegalArgumentException("Item requires a price amount");
            }
        }

        // If the {@link ItemEntry#COLUMN_ITEM_QUANTITY} key is present,
        // check that the quantity value is valid.
        if (values.containsKey(ItemContract.ItemEntry.COLUMN_ITEM_QUANTITY)) {
            String quantity = values.getAsString(ItemContract.ItemEntry.COLUMN_ITEM_QUANTITY);
            if (quantity==null) {
                throw new IllegalArgumentException("Item requires a quantity amount");
            }
        }

        // If the {@link ItemEntry#COLUMN_ITEM_SUPPLIER_NAME} key is present,
        // check that the supplier name value is valid.
        if (values.containsKey(ItemContract.ItemEntry.COLUMN_ITEM_SUPPLIER_NAME)) {
            String supplierName = values.getAsString(ItemContract.ItemEntry.COLUMN_ITEM_SUPPLIER_NAME);
            if (supplierName==null) {
                throw new IllegalArgumentException("Item requires a supplier name");
            }
        }

        // If the {@link ItemEntry#COLUMN_ITEM_SUPPLIER_PHONE} key is present,
        // check that the supplier phone value is valid.
        if (values.containsKey(ItemContract.ItemEntry.COLUMN_ITEM_SUPPLIER_PHONE)) {
            String supplierPhone = values.getAsString(ItemContract.ItemEntry.COLUMN_ITEM_SUPPLIER_NAME);
            if (supplierPhone==null) {
                throw new IllegalArgumentException("Item requires a supplier phone");
            }
        }

        // If there are no values to update, then don't try to update the database
        if (values.size() == 0) {
            return 0;
        }// Otherwise, get writable database to update the data
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        // Returns the number of database rows affected by the update statement
        return database.update(ItemContract.ItemEntry.TABLE_NAME, values, selection, selectionArgs);
    }

    // Delete the data at the given selection and selection arguments.
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // Get writeable database
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        final int match = sUriMatcher.match(uri);
        switch (match) {
            case ITEMS:
                // Delete all rows that match the selection and selection args
                return database.delete(ItemContract.ItemEntry.TABLE_NAME, selection, selectionArgs);
            case ITEM_ID:
                // Delete a single row given by the ID in the URI
                selection = ItemContract.ItemEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                return database.delete(ItemContract.ItemEntry.TABLE_NAME, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Deletion is not supported for " + uri);
        }
    }

    // Returns the MIME type of data for the content URI.
    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case ITEMS:
                return CONTENT_LIST_TYPE;
            case ITEM_ID:
                return CONTENT_ITEM_TYPE;
            default:
                throw new IllegalStateException("Unknown URI " + uri + " with match " + match);
        }
    }

}