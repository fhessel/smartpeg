package de.tudarmstadt.ambientintelligence.smartpeg.app;


import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;

import javax.swing.*;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.json.simple.JSONObject;

import static org.glassfish.jersey.internal.guava.Predicates.equalTo;

/**
 * Hello world!
 *
 */
public class RestClient
{
    public static void main( String[] args ) throws IOException {

        //getExample();
        postExample();
    }

    private static void postExample() {
        CloseableHttpClient client = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost("http://localhost:8080/rest/smartpeg/peg/1/readings");

        try {
            String json = "{\"temperature\":23.12,\"humidity\":57.00,\"conductance\":1,\"timestamp\":\"2017-12-13 13:02:22\"}";
            StringEntity entity = new StringEntity(json);
            httpPost.setEntity(entity);
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");

            CloseableHttpResponse response = client.execute(httpPost);
            System.out.println(response.getStatusLine().getStatusCode());
            client.close();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private static void getExample() throws IOException {
        HttpClient client = new DefaultHttpClient();
        HttpGet request = new HttpGet("http://localhost:8080/rest/smartpeg/peg/1");
        HttpResponse response = client.execute(request);

        // Get the response
        BufferedReader rd = new BufferedReader
                (new InputStreamReader(
                        response.getEntity().getContent()));

        String line = "";

        while ((line = rd.readLine()) != null) {
            System.out.println(line);
        }
    }
}
