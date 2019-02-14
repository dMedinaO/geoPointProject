package cl.esanhueza.map_david.storage;

import android.provider.BaseColumns;

public final class ResponseContract {

    // To prevent someone from accidentally instantiating the contract class,
    // make the constructor private.
    private ResponseContract() {}

    /* Inner class that defines the table contents */
    public static class ResponseEntry implements BaseColumns {
        public static final String TABLE_NAME = "responses";
        public static final String COLUMN_NAME_POLL_ID = "poll";
        public static final String COLUMN_NAME_QUESTION_ID = "question";
        public static final String COLUMN_NAME_CONTENT = "content";
        public static final String COLUMN_NAME_PERSON_ID = "person";
    }

    public static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + ResponseEntry.TABLE_NAME + " (" +
                    ResponseEntry._ID + " INTEGER PRIMARY KEY," +
                    ResponseEntry.COLUMN_NAME_POLL_ID + " TEXT," +
                    ResponseEntry.COLUMN_NAME_QUESTION_ID + " TEXT," +
                    ResponseEntry.COLUMN_NAME_PERSON_ID + " TEXT," +
                    ResponseEntry.COLUMN_NAME_CONTENT + " TEXT)";

    public static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + ResponseEntry.TABLE_NAME;

}