package com.example.demostracion;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;

import cz.msebera.android.httpclient.Header;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    GridLayoutManager lay;
    RecyclerView recycler;
    menuAdapter men;
    Button cargar,insert;
    ImageView img;
    private final int CODE_PERMISSIONS = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //seteamos los adaptadores ya creados menuadapter e item url

        recycler=findViewById(R.id.recyclerId);
        lay=new GridLayoutManager(this,2);
        recycler.setLayoutManager(lay);
        men=new menuAdapter(this);
        cargar();
        recycler.setAdapter(men);

        cargar=findViewById(R.id.cargarImagen);
        cargar.setOnClickListener(this);
        insert=findViewById(R.id.insertarImagen);

        insert.setVisibility(View.INVISIBLE);
        if(reviewPermissions()){
            insert.setVisibility(View.VISIBLE);
        }
        insert.setOnClickListener(this);

        img=findViewById(R.id.vistaImagenInsertar);

    }

    private void cargar() {
        AsyncHttpClient client=new AsyncHttpClient();
        client.get(ip.localhost+"image",null,new JsonHttpResponseHandler(){
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                super.onSuccess(statusCode, headers, response);
                for(int i=0;i<response.length();i++){
                    try {
                        JSONObject dat=response.getJSONObject(i);
                        String url=dat.getString("url");
                        men.add(new item(url));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                super.onSuccess(statusCode, headers, response);
            }
        });

    }
    private void enviarDatos() {
        AsyncHttpClient client=new AsyncHttpClient();
        RequestParams params=new RequestParams();
        if (path == null || path == ""){
            Toast.makeText(this, "Debe seleccionar una imagen", Toast.LENGTH_SHORT).show();
            return;
        }
        File file = new File(path);
        try {
            params.put("img", file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        Toast.makeText(getApplicationContext(),path,Toast.LENGTH_SHORT).show();
        client.post(ip.localhost+"image",params,new JsonHttpResponseHandler(){
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                super.onSuccess(statusCode, headers, response);
                try {
                    String mesagge=response.getString("message");
                    if(mesagge!=null){
                        Toast.makeText(getApplicationContext(),mesagge,Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        });
    }


    @Override
    public void onClick(View v) {
        if(v.getId()==R.id.cargarImagen){
            cargarImagen();
        }else if(v.getId()==R.id.insertarImagen){
            enviarDatos();
        }
    }


    // si es que hay permiso devuelve true;

    private boolean reviewPermissions() {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M){
            return true;
        }

        if(this.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
            return true;
        }
        requestPermissions(new String [] {Manifest.permission.CAMERA,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE},
                CODE_PERMISSIONS);
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (CODE_PERMISSIONS == requestCode) {
            if (permissions.length == 3) {
                insert.setVisibility(View.VISIBLE);

            }

        }
    }

    //opciones para acceder a la camara o galeria

    final int COD_GALERIA=10;
    final int COD_CAMERA=20;
    String path;
    private void cargarImagen() {
        final CharSequence[] opciones={"Tomar Foto","Cargar Imagen","Cancelar"};
        final AlertDialog.Builder alertOpciones=new AlertDialog.Builder(MainActivity.this);
        alertOpciones.setTitle("Seleccione una Opción");
        alertOpciones.setItems(opciones, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (opciones[i].equals("Tomar Foto")){
                    tomarFotografia();
                }else{
                    if (opciones[i].equals("Cargar Imagen")){
                        Intent intent=new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        intent.setType("image/");
                        startActivityForResult(intent.createChooser(intent,"Seleccione la Aplicación"),COD_GALERIA);
                    }else{
                        dialogInterface.dismiss();
                    }
                }
            }
        });
        alertOpciones.show();
    }

    private void tomarFotografia(){
        Intent camera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        FileAndPath fileAndPath= createFile(path,getApplicationContext());
        File file = fileAndPath.getFile();
        path = fileAndPath.getPath();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Uri fileuri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", file);
            camera.putExtra(MediaStore.EXTRA_OUTPUT, fileuri);
        } else {
            camera.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(file));
        }
        startActivityForResult(camera, COD_CAMERA);
    }

    //opciones para setear la imagen

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode==RESULT_OK){
            switch (requestCode){
                case COD_GALERIA:
                    Uri imgPath=data.getData();
                    img.setImageURI(imgPath);
                    path = getRealPathFromURI(this,imgPath);
                    Toast.makeText(MainActivity.this, path, Toast.LENGTH_SHORT).show();
                    break;
                case COD_CAMERA:
                    loadImageCamera();
            }
        }
    }
    private void loadImageCamera() {
        Bitmap imgag = BitmapFactory.decodeFile(path);
        if(img != null) {
            img.setImageBitmap(imgag);

        }
    }


    // aqui nos devolvera la direccion del file path

    public static class FileAndPath{
        File file;
        String path;

        public FileAndPath(File file, String path) {
            this.file = file;
            this.path = path;
        }
        public File getFile() {
            return file;
        }
        public String getPath() {
            return path;
        }
    }
    public static FileAndPath createFile(String path, Context c) {
        //Logica de creado
        File file = new File(Environment.getExternalStorageDirectory(), Environment.DIRECTORY_PICTURES);
        if (!file.exists()) {
            file.mkdirs();
            //Toast.makeText(c,"crea un archivo mkdirs",Toast.LENGTH_LONG).show();
        }

        //generar el nombre
        String name = "";
        if (file.exists()) {
            name = "IMG_" + System.currentTimeMillis() / 1000 + ".jpg";
        }
        path = file.getAbsolutePath() + File.separator + name;
        File fileimg = new File(path);
        //Toast.makeText(c,"este es el path "+path,Toast.LENGTH_LONG).show();
        //Toast.makeText(c,"este es el file img "+fileimg,Toast.LENGTH_LONG).show();
        return new FileAndPath(fileimg,path);
    }
    //Aqui recuperamos la url a partir de la imagen
    public static String getRealPathFromURI(Context context, Uri contentURI) {
        String result = null;
        Cursor cursor = context.getContentResolver().query(contentURI,
                null, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
            int idx = cursor
                    .getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            result = cursor.getString(idx);
            cursor.close();
        }
        return result;
    }

}
