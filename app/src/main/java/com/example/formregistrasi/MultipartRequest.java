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
     * Constructor for MultipartRequest.
     *
     * @param url           The URL to send the request to.
     * @param headers       A Map of request headers.
     * @param mimeType      The MIME type of the request body.
     * @param multipartBody The multipart request body as a byte array.
     * @param listener      Listener to receive the NetworkResponse.
     * @param errorListener Listener to receive any errors.
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

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        return (mHeaders != null) ? mHeaders : super.getHeaders();
    }

    @Override
    public String getBodyContentType() {
        return mMimeType;
    }

    @Override
    public byte[] getBody() throws AuthFailureError {
        return mMultipartBody;
    }

    @Override
    protected Response<NetworkResponse> parseNetworkResponse(NetworkResponse response) {
        return Response.success(response, HttpHeaderParser.parseCacheHeaders(response));
    }

    @Override
    protected void deliverResponse(NetworkResponse response) {
        mListener.onResponse(response);
    }

    @Override
    public void deliverError(VolleyError error) {
        mErrorListener.onErrorResponse(error);
    }

    /**
     * Returns a list of extra HTTP headers to go along with this request.
     * @return A Map of header names to header values.
     */
    @Override
    public Map<String, String> getParams() {
        return Collections.emptyMap();
    }

    /**
     * Returns the raw POST or PUT body to be sent.
     *
     * @throws AuthFailureError in the event of auth failure
     */
    @Override
    public byte[] getPostBody() throws AuthFailureError {
        return getBody();
    }

    /**
     * Returns the content type of the POST or PUT body.
     */
    @Override
    public String getPostBodyContentType() {
        return getBodyContentType();
    }
}