package com.example.mycontactlist;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract.Contacts;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
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
            Build.VERSION.SDK_INT
                    >= Build.VERSION_CODES.HONEYCOMB ?
                    // いずれも連作先の表示名を表す
                    Contacts.DISPLAY_NAME_PRIMARY :
                    Contacts.DISPLAY_NAME
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
    private SimpleCursorAdapter mCursorAdapter;

    @SuppressLint("InlinedApi")
    // データ取得するデータ群. _IDとLOOKUP_KEYはデータにアクセスする際に利用する
    private static final String[] PROJECTION =
            {
                    Contacts._ID,  // コンテンツプロバイダのデータの中で一意となるキー
                    Contacts.LOOKUP_KEY,  // 特定の連絡先に対するパーマリンクとなるキー
                    Build.VERSION.SDK_INT
                            >= Build.VERSION_CODES.HONEYCOMB ?
                            Contacts.DISPLAY_NAME_PRIMARY :
                            Contacts.DISPLAY_NAME
            };

    // Defines the text expression
    @SuppressLint("InlinedApi")
    // データを取得する際に利用されるクエリ
    private static final String SELECTION =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ?
                    Contacts.DISPLAY_NAME_PRIMARY + " LIKE ?" :
                    Contacts.DISPLAY_NAME + " LIKE ?";

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

        mSearchString = "Hoge";  // "ro of Taro/Jiro/Saburo "

        // Gets the ListView from the View list of the parent activity
        mContactsList =
                (ListView) getActivity().findViewById(R.id.contacts_fragment);
        // Gets a CursorAdapter
        mCursorAdapter = new SimpleCursorAdapter(
                getActivity(),
                R.layout.contacts_list_item,
                null,
                FROM_COLUMNS, TO_IDS,
                0);
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

    private void loadView() {
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
        return new CursorLoader(
                getActivity(),
                Contacts.CONTENT_URI,
                PROJECTION,
                SELECTION,
                mSelectionArgs,
                null
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

    @Override
    public void onItemClick(AdapterView<?> parent, View item, int position, long rowID) {

    }
}
