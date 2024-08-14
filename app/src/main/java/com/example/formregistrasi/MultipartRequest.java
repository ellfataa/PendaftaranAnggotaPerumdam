package com.example.formregistrasi;

import androidx.annotation.Nullable;
import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MultipartRequest extends Request<NetworkResponse> {
    private final Response.Listener<NetworkResponse> mListener;
    private final Response.ErrorListener mErrorListener;
    private final Map<String, String> mHeaders;
    private final String mMimeType;
    private final byte[] mMultipartBody;

    /**
     * Konstruktor untuk MultipartRequest.
     * Ini adalah metode utama untuk membuat objek MultipartRequest.
     */
    public MultipartRequest(String url, @Nullable Map<String, String> headers, String mimeType, byte[] multipartBody,
                            Response.Listener<NetworkResponse> listener, Response.ErrorListener errorListener) {
        super(Method.POST, url, errorListener);
        this.mListener = listener;
        this.mErrorListener = errorListener;
        this.mHeaders = headers != null ? new HashMap<>(headers) : null;
        this.mMimeType = mimeType;
        this.mMultipartBody = multipartBody;
    }

    /**
     * Mendapatkan header untuk request.
     * Metode ini dipanggil oleh Volley untuk mendapatkan header tambahan.
     */
    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        return (mHeaders != null) ? mHeaders : super.getHeaders();
    }

    /**
     * Mendapatkan tipe konten body.
     * Metode ini menentukan MIME type dari body request.
     */
    @Override
    public String getBodyContentType() {
        return mMimeType;
    }

    /**
     * Mendapatkan body request.
     * Metode ini mengembalikan data multipart yang akan dikirim.
     */
    @Override
    public byte[] getBody() throws AuthFailureError {
        return mMultipartBody;
    }

    /**
     * Memproses response dari network.
     * Metode ini dipanggil ketika response diterima dari server.
     */
    @Override
    protected Response<NetworkResponse> parseNetworkResponse(NetworkResponse response) {
        return Response.success(response, HttpHeaderParser.parseCacheHeaders(response));
    }

    /**
     * Mengirimkan response ke listener.
     * Metode ini dipanggil setelah response berhasil diproses.
     */
    @Override
    protected void deliverResponse(NetworkResponse response) {
        mListener.onResponse(response);
    }

    /**
     * Mengirimkan error ke error listener.
     * Metode ini dipanggil jika terjadi error selama proses request.
     */
    @Override
    public void deliverError(VolleyError error) {
        mErrorListener.onErrorResponse(error);
    }

    /**
     * Mendapatkan parameter tambahan.
     * Metode ini mengembalikan map kosong karena parameter sudah termasuk dalam multipart body.
     */
    @Override
    protected Map<String, String> getParams() throws AuthFailureError {
        return Collections.emptyMap();
    }

    /**
     * Mendapatkan body untuk metode POST atau PUT.
     * Metode ini memanggil getBody() untuk konsistensi.
     */
    @Override
    public byte[] getPostBody() throws AuthFailureError {
        return getBody();
    }

    /**
     * Mendapatkan tipe konten untuk body POST atau PUT.
     * Metode ini memanggil getBodyContentType() untuk konsistensi.
     */
    @Override
    public String getPostBodyContentType() {
        return getBodyContentType();
    }
}