package com.example.manavdutta1.myapplication;

import com.firebase.client.Firebase;

/**
 * Created by manavdutta1 on 2/20/16.
 */
public class FirebaseHelper {
    private Firebase myRef;
    public FirebaseHelper(String url){
        myRef = new Firebase(url);
    }
    public void saveArray(int[] array) {
        //DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        //Date date = new Date();
        myRef.child("engagement").setValue(array[0]);
        myRef.child("attention").setValue(array[1]);
    }
}