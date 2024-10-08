package com.example.formregistrasi;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class VolleyMultipartRequest extends Request<NetworkResponse> {
    private final String twoHyphens = "--";
    private final String lineEnd = "\r\n";
    private final String boundary = "apiclient-" + System.currentTimeMillis();

    private final Response.Listener<NetworkResponse> mListener;
    private final Response.ErrorListener mErrorListener;
    private final Map<String, String> mHeaders;

    // Kelas untuk menyimpan data bagian (part) dari request multipart
    public static class DataPart {
        private String fileName;
        private byte[] content;
        private String type;

        public DataPart() {
        }

        public DataPart(String name, byte[] data) {
            fileName = name;
            content = data;
        }

        public String getFileName() {
            return fileName;
        }

        public byte[] getContent() {
            return content;
        }

        public String getType() {
            return type;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public void setContent(byte[] content) {
            this.content = content;
        }

        public void setType(String type) {
            this.type = type;
        }
    }

    // Konstruktor untuk membuat instance VolleyMultipartRequest
    public VolleyMultipartRequest(int method, String url,
                                  Response.Listener<NetworkResponse> listener,
                                  Response.ErrorListener errorListener) {
        super(method, url, errorListener);
        this.mListener = listener;
        this.mErrorListener = errorListener;
        this.mHeaders = new HashMap<>();
    }

    // Metode untuk mendapatkan header request
    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        return (mHeaders != null) ? mHeaders : super.getHeaders();
    }

    // Metode untuk menambahkan header ke request
    public void addHeader(String key, String value) {
        mHeaders.put(key, value);
    }

    // Metode untuk mengurai respons jaringan
    @Override
    protected Response<NetworkResponse> parseNetworkResponse(NetworkResponse response) {
        return Response.success(response, HttpHeaderParser.parseCacheHeaders(response));
    }

    // Metode untuk mengirimkan respons ke listener
    @Override
    protected void deliverResponse(NetworkResponse response) {
        mListener.onResponse(response);
    }

    // Metode untuk mengirimkan error ke error listener
    @Override
    public void deliverError(VolleyError error) {
        mErrorListener.onErrorResponse(error);
    }

    // Metode untuk mendapatkan parameter request
    @Override
    protected Map<String, String> getParams() throws AuthFailureError {
        return null;
    }

    // Metode untuk mendapatkan data byte dari request
    protected Map<String, DataPart> getByteData() throws AuthFailureError {
        return null;
    }

    // Metode untuk mendapatkan tipe konten body
    @Override
    public String getBodyContentType() {
        return "multipart/form-data; boundary=" + boundary;
    }

    // Metode untuk membuat body request
    @Override
    public byte[] getBody() throws AuthFailureError {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);

        try {
            // Menambahkan payload teks
            Map<String, String> params = getParams();
            if (params != null && !params.isEmpty()) {
                textParse(dos, params, getParamsEncoding());
            }

            // Menambahkan payload data byte
            Map<String, DataPart> data = getByteData();
            if (data != null && !data.isEmpty()) {
                dataParse(dos, data);
            }

            // Menutup data form multipart setelah teks dan data file
            dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

            return bos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Metode untuk mengurai dan menulis data teks ke output stream
    private void textParse(DataOutputStream dataOutputStream, Map<String, String> params, String encoding) throws IOException {
        try {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                buildTextPart(dataOutputStream, entry.getKey(), entry.getValue());
            }
        } catch (UnsupportedEncodingException uee) {
            throw new RuntimeException("Encoding tidak didukung: " + encoding, uee);
        }
    }

    // Metode untuk mengurai dan menulis data byte ke output stream
    private void dataParse(DataOutputStream dataOutputStream, Map<String, DataPart> data) throws IOException {
        for (Map.Entry<String, DataPart> entry : data.entrySet()) {
            buildDataPart(dataOutputStream, entry.getValue(), entry.getKey());
        }
    }

    // Metode untuk membangun bagian teks dari form multipart
    private void buildTextPart(DataOutputStream dataOutputStream, String parameterName, String parameterValue) throws IOException {
        dataOutputStream.writeBytes(twoHyphens + boundary + lineEnd);
        dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"" + parameterName + "\"" + lineEnd);
        dataOutputStream.writeBytes(lineEnd);
        dataOutputStream.writeBytes(parameterValue + lineEnd);
    }

    // Metode untuk membangun bagian data dari form multipart
    private void buildDataPart(DataOutputStream dataOutputStream, DataPart dataFile, String inputName) throws IOException {
        dataOutputStream.writeBytes(twoHyphens + boundary + lineEnd);
        dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"" +
                inputName + "\"; filename=\"" + dataFile.getFileName() + "\"" + lineEnd);
        if (dataFile.getType() != null && !dataFile.getType().trim().isEmpty()) {
            dataOutputStream.writeBytes("Content-Type: " + dataFile.getType() + lineEnd);
        }
        dataOutputStream.writeBytes(lineEnd);

        ByteArrayOutputStream fileOutputStream = new ByteArrayOutputStream();
        fileOutputStream.write(dataFile.getContent());
        fileOutputStream.writeTo(dataOutputStream);

        dataOutputStream.writeBytes(lineEnd);
    }
}