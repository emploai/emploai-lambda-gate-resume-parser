package com.emploai.apps.util;

import java.io.IOException;
import java.io.InputStream;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.ToXMLContentHandler;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;

import org.json.simple.JSONObject;

public class TikaLambdaHandler {

    public String readFile(String key) {
        try {
            
            String bucket = System.getenv("S3_BUCKET");
            
            AmazonS3 s3Client = new AmazonS3Client();
            S3Object s3Object = s3Client.getObject(new GetObjectRequest(bucket, key));
            //  System.out.println("-- got s3Client  ");
            try (InputStream objectData = s3Object.getObjectContent()) {
                String content = tikaParseToHTML(objectData); 
                objectData.close();
                return content;
            }
        } catch (IOException | SAXException e) {
            System.out.println("Exception: " + e.getLocalizedMessage());
            throw new RuntimeException(e);
        }
    }

    private String tikaParseToHTML(InputStream objectData) throws IOException, SAXException {
      //  System.out.println("Extracting contents with Tika");
      
      ContentHandler handler = new ToXMLContentHandler();
      AutoDetectParser parser = new AutoDetectParser();
      Metadata metadata = new Metadata();
      try {
        parser.parse(objectData, handler, metadata);
        return handler.toString();
      } catch (TikaException e) {
        System.out.println("Exception: " + e.getLocalizedMessage());
      }
      return null;
    }
}
