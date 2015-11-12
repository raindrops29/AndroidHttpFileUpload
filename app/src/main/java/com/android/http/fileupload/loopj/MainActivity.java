package com.android.http.fileupload.loopj;

import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {
    private Button senderBtn;
    private Button phoneGalleryBtn;
    private EditText memberName;
    private ImageView memberImage;

    private static final int IMAGE_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        memberName = (EditText) findViewById(R.id.memberName);
        memberImage = (ImageView) findViewById(R.id.memberImage);
        senderBtn = (Button) findViewById(R.id.send);
        phoneGalleryBtn = (Button) findViewById(R.id.btn_phone_gallery);

        phoneGalleryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_GET_CONTENT);
                //intent.setData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                //startActivityForResult(intent, IMAGE_REQUEST_CODE);
                intent.setType("image/*");
                startActivityForResult(
                        Intent.createChooser(intent, "파일을 선택하라"), IMAGE_REQUEST_CODE);
            }
        });
        senderBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File upLoadFile = new File(fileLocation);
                String member = memberName.getText().toString().trim();
                if (upLoadFile.isFile()) {
                    new FileUpLoadAsyncTask(member).execute(upLoadFile);
                } else {
                    Toast.makeText(MainActivity.this,
                            "읽어올 파일이 존재하지 않음", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

        });
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == this.RESULT_OK) {
            if (requestCode == IMAGE_REQUEST_CODE)
                if (Build.VERSION.SDK_INT < 19) {
                    Uri selectedImage = data.getData();
                    // System.out.println("selectedImage "+selectedImage);
                    String[] filePathColumn = {MediaStore.Images.Media.DATA};
                    Cursor cursor = getContentResolver().query(
                            selectedImage, filePathColumn, null, null, null);
                    cursor.moveToFirst();
                    int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                    fileLocation = cursor.getString(columnIndex);
                    cursor.close();
                    System.out.println("fileLocation" + fileLocation);
                    memberImage.setImageBitmap(BitmapFactory.decodeFile(fileLocation));

                } else {
                    try {
                        InputStream imInputStream =
                                getContentResolver().openInputStream(data.getData());
                        Bitmap bitmap = BitmapFactory.decodeStream(imInputStream);
                        fileLocation = saveGalaryImageOnLitkat(bitmap);
                        memberImage.setImageBitmap(BitmapFactory.decodeFile(fileLocation));
                        imInputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
        }
    }
    private final int COMPRESS = 100;
    private String fileLocation = "";//선택한 파일의 절대주소

    private String saveGalaryImageOnLitkat(Bitmap bitmap) {
        try {
            File cacheDir;
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
                cacheDir = new File(Environment.getExternalStorageDirectory(),
                        "temporaryDir");
            else
                cacheDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            if (!cacheDir.exists())
                cacheDir.mkdirs();
            String filename = System.currentTimeMillis() + ".jpeg";
            File file = new File(cacheDir, filename);

            file.createNewFile();
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESS, out);
            out.close();
            return file.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;

    }


    /*protected void onActivityResult(int requestCode,
                                    int resultCode, Intent data) {
        Uri returedImgURI = null;
        if (requestCode == IMAGE_REQUEST_CODE && resultCode ==
                Activity.RESULT_OK) {
            returedImgURI = data.getData();
            Log.e("aaaaaaaaaaa", returedImgURI.toString());
            Bitmap bm = null;
            Cursor cursor = null;
            try {

                *//*bm = MediaStore.Images.Media.getBitmap(getContentResolver(),
                        returedImgURI);
                memberImage.setImageBitmap(bm);*//*

                cursor = getContentResolver().query(returedImgURI,
                        new String[]{
                                MediaStore.MediaColumns.DATA}, null, null, null);
                cursor.moveToFirst();

                fileLocation = cursor.getString(0);

                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 2;

                memberImage.setImageBitmap(BitmapFactory.decodeFile(fileLocation, options));

            } catch (Exception e) {
                Log.e("onActivityResult", "이미지 요청시 에러! ", e);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
    }*/

    private class FileUpLoadAsyncTask extends AsyncTask<File, Void, String> {
        ProgressDialog dialog;
        private String member;
        public FileUpLoadAsyncTask(){}
        public FileUpLoadAsyncTask(String member){
            this.member = member;
        }
        @Override
        public void onPreExecute() {
            super.onPreExecute();
            dialog = new ProgressDialog(MainActivity.this);
            dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            dialog.setMessage("업로드 중~~~");
            dialog.show();
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
        }

        private String setQueryValue(String name, String value) {
            return "Content-Disposition: form-data; name=\""
                    + name + "\"r\n\r\n" + value;
        }

        private String setFileValue(String name, String fileName) {
            return "Content-Disposition: form-data; name=\"" + name
                    + "\";filename=\"" + fileName + "\"\r\n";
        }

        @Override
        protected String doInBackground(File... params) {

            HttpURLConnection connection = null;
            DataOutputStream toServer = null;
            FileInputStream inFile = null;
            String boundary = "^*^*^*^*^";
            int maxBufferSize = 1024;
            try {

                URL targetURL = new URL(FileUpLoadConstant.UP_LOAD_URL);
                connection = (HttpURLConnection) targetURL.openConnection();
                connection.setDoInput(true);
                connection.setDoOutput(true);
                connection.setUseCaches(false);
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Connection", "Keep-Alive");
                //connection.setRequestProperty("ENCTYPE", "multipart/form-data");
                connection.setRequestProperty("Content-Type",
                        "multipart/form-data;boundary=" + boundary);

                toServer = new DataOutputStream(connection.getOutputStream());

                StringBuilder upLoadRFPRole = new StringBuilder();

                upLoadRFPRole
                        .append("--" + boundary + "\r\n")
                        .append(setQueryValue("memberName",member ))
                        .append("\r\n")
                        .append("--" + boundary + "\r\n")
                        .append(setFileValue("picture", params[0].getName()))
                        .append("\r\n");
                toServer.write(upLoadRFPRole.toString().getBytes("UTF-8"));

                inFile = new FileInputStream(params[0]);
                //int bufSize = Math.min(inFile.available(), maxBufferSize);
                byte[] realBuf = new byte[maxBufferSize];


                int dataCount = inFile.read(realBuf, 0, maxBufferSize);

                while (dataCount > 0) {
                    toServer.write(realBuf, 0, dataCount);
                    dataCount = inFile.read(realBuf, 0, maxBufferSize);
                }
                toServer.writeBytes("\r\n");
                toServer.writeBytes("--"+boundary+"--"+"\r\n"); // 반드시 작성해야 한다.
                toServer.flush();
                toServer.close();
                inFile.close();


                connection.getInputStream();

            } catch (IOException ioe) {
                Log.e("업로드 에러", ioe.toString());
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
            return null;
        }
    }
}
