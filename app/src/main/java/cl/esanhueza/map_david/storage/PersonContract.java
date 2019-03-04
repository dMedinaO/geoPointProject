package cl.esanhueza.map_david.storage;

import android.provider.BaseColumns;

public final class PersonContract {

    // To prevent someone from accidentally instantiating the contract class,
    // make the constructor private.
    private PersonContract() {}

    /* Inner class that defines the table contents */
    public static class PersonEntry implements BaseColumns {
        public static final String TABLE_NAME = "persons";
        public static final String COLUMN_NAME_PERSON_ID = "person";
        public static final String COLUMN_NAME_POLL_ID = "poll";
        public static final String COLUMN_NAME_DATE = "date";
    }

    public static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + PersonEntry.TABLE_NAME + " (" +
                    PersonEntry._ID + " INTEGER PRIMARY KEY," +
                    PersonEntry.COLUMN_NAME_PERSON_ID + " TEXT," +
                    PersonEntry.COLUMN_NAME_POLL_ID + " TEXT," +
                    PersonEntry.COLUMN_NAME_DATE + " TEXT)";

    public static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + PersonEntry.TABLE_NAME;

}
