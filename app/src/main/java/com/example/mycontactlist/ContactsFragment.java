package com.example.mycontactlist;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.QuickContactBadge;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

/* LoaderManager.LoaderCallbacks: 連絡先のデータを連絡先ローダで非同期で読み込んだ後に
   呼ばれるメソッドが定義されているインターフェース */
public class ContactsFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor>,
        AdapterView.OnItemClickListener {

    /*
    * Defines an array that contains column names to move from
    * the Cursor to the ListView.
    */
    // 連絡先プロバイダからUIに取得するデータを指定
    // アノテーションは、古いAndroid環境でAndroid Lintというチェックツールを掛けた際に警告を抑制する
    @SuppressLint("InlinedApi")
    private final static String[] FROM_COLUMNS = {
//            Build.VERSION.SDK_INT
//                    >= Build.VERSION_CODES.HONEYCOMB ?
//                    // いずれも連作先の表示名を表す
//                    Contacts.DISPLAY_NAME_PRIMARY :
//                    Contacts.DISPLAY_NAME
            Email.ADDRESS
    };
    /*
    * Defines an array that contains resource ids for the layout views
    * that get the Cursor column contents. The id is pre-defined in
    * the Android framework, so it is preface with "android.R.id"
    */
    // 後でSimpleCursorAdapterに渡すためのデータが読み込まれた時に渡すデータの格納先のUIを指定
    // ここではTextViewが利用されているのでそのIDを渡す
    private final static int[] TO_IDS = {
            android.R.id.text1
    };
    // Define global mutable variables
    // Define a ListView object
    // レイアウトxmlで定義されているListViewのフィールド
    ListView mContactsList;

    // Cursorを受け取り、そこから取得されるデータを自動先にUIに格納してくれるアダプタ
//    private SimpleCursorAdapter mCursorAdapter;
    private CursorAdapter mCursorAdapter;  // 独自のCursorAdapterを実装するため

    @SuppressLint("InlinedApi")
    // データ取得するデータ群. _IDとLOOKUP_KEYはデータにアクセスする際に利用する
    private static final String[] PROJECTION =
            {
//                    Contacts._ID,  // コンテンツプロバイダのデータの中で一意となるキー
//                    Contacts.LOOKUP_KEY,  // 特定の連絡先に対するパーマリンクとなるキー
//                    Build.VERSION.SDK_INT
//                            >= Build.VERSION_CODES.HONEYCOMB ?
//                            Contacts.DISPLAY_NAME_PRIMARY :
//                            Contacts.DISPLAY_NAME
                        Email._ID,
                        Email.ADDRESS,
                        Email.TYPE,
                        Email.LABEL,
                        Contacts._ID,
                        Contacts.LOOKUP_KEY,
                        Contacts.PHOTO_THUMBNAIL_URI
            };

    // Defines the text expression
    @SuppressLint("InlinedApi")
    // データを取得する際に利用されるクエリ
    private static final String SELECTION =
//            Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ?
//                    Contacts.DISPLAY_NAME_PRIMARY + " LIKE ?" :
//                    Contacts.DISPLAY_NAME + " LIKE ?";

            Email.ADDRESS + " LIKE ?" +
                    " AND " +
                    Data.MIMETYPE + " = " +
                    "'" + Email.CONTENT_ITEM_TYPE + "'";

    private static final String SORT_ORDER = Email.TYPE + " ASC ";

    // Defines a variable for the search string
    private String mSearchString;
    // Defines the array to hold values that replace the "?"
    // クエリに渡す文字列の配列.
    private String[] mSelectionArgs = { mSearchString };


    // Empty public constructor, required by the system
    public ContactsFragment() {}

    // A UI Fragment must inflate its View
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the fragment layout
        return inflater.inflate(R.layout.contacts_list_view,
                container, false);
    }

    // Request code for READ_CONTACTS. It can be any number > 0.
    private static final int PERMISSION_REQUEST_READ_CONTACTS = 100;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mSearchString = "ro";  // "ro of Taro/Jiro/Saburo "

        // Gets the ListView from the View list of the parent activity
        mContactsList =
                (ListView) getActivity().findViewById(R.id.contacts_fragment);

        // Gets a CursorAdapter
        /*
        mCursorAdapter = new SimpleCursorAdapter(
                getActivity(),
                R.layout.contacts_list_item,
                null,
                FROM_COLUMNS, TO_IDS,
                0);

        /*
         * Instantiates the subclass of CursorAdapter
         */
        // 上に変えて、独自のadapterをインスタンス化
        mCursorAdapter =
                new ContactsAdapter(getActivity());

        // Sets the adapter for the ListView
        mContactsList.setAdapter(mCursorAdapter);

        // Set the item click listener to be the current fragment.
        mContactsList.setOnItemClickListener(this);

        // Check the SDK version and whether the permission is already granted or not.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                getActivity().checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, PERMISSION_REQUEST_READ_CONTACTS);
            // After this point you wait for callback in onRequestPermissionResult(int, String[], int[]) overriden method
        } else {
            // 連絡先のデータ読み込み
            loadView();
        }
    }

    public void loadView() {
        // Initializes the loader
        // ContactsFragment自体を、LoaderManager.LoaderCallbacksのインスタンスとして、
        // 連絡先ローダを初期化
        getLoaderManager().initLoader(0, null, ContactsFragment.this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_READ_CONTACTS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission is granted
                loadView();
            } else {
                Toast.makeText(getActivity(), "Until you grant the permission, we cannot display the names",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    // initLoaderの呼び出しの後、非同期で呼ばれる（LoaderManager.LoaderCallbacksに定義されたメソッド）
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        /*
        * Makes search string into pattern and
        * stores it in the selection array
        */
        mSelectionArgs[0] = "%" + mSearchString + "%";  // "%"はLIKE句における、どの文字列にもマッチする
        // Starts the query
        // CursorLoaderインスタンスを作成、Contacts.CONTENT_URIからコンテンツURIを取得
        // 取得する列をPROJECTION,絞り込む条件をSELECTIONとmSelectionArgsで設定
//        return new CursorLoader(
//                getActivity(),
//                Contacts.CONTENT_URI,
//                PROJECTION,
//                SELECTION,
//                mSelectionArgs,
//                null
//        );

        return new CursorLoader(
                getActivity(),
                Data.CONTENT_URI,
                PROJECTION,
                SELECTION,
                mSelectionArgs,
                SORT_ORDER
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        // Put the result Cursor in the adapter for the ListView
        // CursorLoaderの読み込みが終了した際に、Cursorを入れ替える
        mCursorAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // Delete the reference to the existing Cursor
        // ローダがリセットされた際にCursorを取り除く
        mCursorAdapter.swapCursor(null);
    }

    public static final int REQUEST_EDIT_CONTACT = 2;

    @Override
    public void onItemClick(AdapterView<?> parent, View item, int position, long rowID) {
        Cursor mCursor = mCursorAdapter.getCursor();

        // Moves to the Cursor row corresponding to the ListView item that was clicked
        mCursor.moveToPosition(position);

        /*
        * Once the user has selected a contact to edit,
        * this gets the contact's lookup key and _ID values from the
        * cursor and creates the necessary URI.
        */
        // Gets the lookup key column index
        int mLookupKeyIndex = mCursor.getColumnIndex(Contacts.LOOKUP_KEY);
        // Gets the lookup key value
        String mCurrentLookupKey = mCursor.getString(mLookupKeyIndex);
        // Gets the _ID column index
        int mIdIndex = mCursor.getColumnIndex(Contacts._ID);
        long mCurrentId = mCursor.getLong(mIdIndex);
        Uri mSelectedContactUri =
                Contacts.getLookupUri(mCurrentId, mCurrentLookupKey);
        // Creates a new Intent to edit a contact
        Intent editIntent = new Intent(Intent.ACTION_EDIT);

        /*
         * Sets the contact URI to edit, and the data type that the
         * Intent must match
         */
        editIntent.setDataAndType(mSelectedContactUri, Contacts.CONTENT_ITEM_TYPE);

        // Sets the special extended data for navigation
        editIntent.putExtra("finishActivityOnSaveCompleted", true);

        // Sends the Intent
        startActivityForResult(editIntent, REQUEST_EDIT_CONTACT);
    }

    private class ContactsAdapter extends CursorAdapter {
        private LayoutInflater mInflater;

        public ContactsAdapter(Context context) {
            super(context , null, 0);

            /*
            * Gets an inflater that can instantiate
            * the ListView layout from the file.
            */
            mInflater = LayoutInflater.from(context);
        }

        /**
         * Defines a class that hold resource IDs of each item layout
         * row to prevent having to look them up each time data is
         * bound to a row.
         */
        // リストの項目に表示させるデータのUIを表すクラス
        public class ViewHolder {
            TextView email;
            QuickContactBadge quickcontact;
        }

        @Override
        public View newView(
                Context context,
                Cursor cursor,
                ViewGroup viewGroup) {
            /*
            * Inflates the item layout. Stores resource IDs
            * in a ViewHolder class to prevent having to look
            * them up each time bindView() is called.
            */
            final View itemView =
                    mInflater.inflate(
                            R.layout.contacts_list_item,
                            viewGroup,
                            false
                    );
            final ViewHolder holder = new ViewHolder();
            holder.email =
                    (TextView) itemView.findViewById(R.id.text1);
            holder.quickcontact =
                    (QuickContactBadge)itemView.findViewById(R.id.quickbadge);
            // 関連するタグデータをViewに関連付け
            itemView.setTag(holder);
            return itemView;
        }

        // itemViewに対して呼ばれる度にcursorからデータを取得し、
        // TextView（メールアドレス）とQuickContactBadge（サムネイルURIとコンテンツURI）を
        // それぞれ設定
        @Override
        public void bindView(View view,
                             Context context,
                             Cursor cursor) {

            final ViewHolder holder = (ViewHolder) view.getTag();
            final String photoUri = cursor.getString(6);  // PHOTO_THUMBNAIL_URI

            final String email = cursor.getString(1);  // ADDRESS
            holder.email.setText(email);

            // Gets the lookup key column index
            int mLookupKeyIndex = cursor.getColumnIndex(Contacts.LOOKUP_KEY);
            // Gets the _ID column index
            int mIdIndex = cursor.getColumnIndex(Contacts._ID);

            /*
            * Generates a contact URI for the QuickContactsBadge.
            */
            final Uri contactUri = Contacts.getLookupUri(
                    cursor.getLong(mIdIndex),
                    cursor.getString(mLookupKeyIndex));
            holder.quickcontact.assignContactUri(contactUri);
            if (photoUri != null) {
                holder.quickcontact.setImageURI(Uri.parse(photoUri));
            }

        }
    }
}
