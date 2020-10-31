package com.emploai.apps;


import java.util.HashMap;
import java.util.Map;

import static spark.Spark.before;
import static spark.Spark.get;

import com.emploai.apps.util.JsonTransformer;
import com.emploai.apps.util.TikaLambdaHandler;
import com.emploai.apps.util.GateResumeParser;

import com.amazonaws.services.lambda.runtime.Context;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class SparkResources {

    // Initialize the Log4j logger.
    static final Logger logger = LogManager.getLogger(SparkResources.class);

    public static void defineResources() {
        before((request, response) -> response.type("application/json"));

        get("/ping", (req, res) -> {
            logger.debug("ping reqrest received");
            Map<String, String> pong = new HashMap<>();
            pong.put("pong", "Hello, World!");
            res.status(200);
            return pong;
        }, new JsonTransformer());


        get("/s3/parse", (req, res) -> {
            try {
                TikaLambdaHandler tikaHandler = new TikaLambdaHandler();
                String key = req.queryParams("key");
                if (key == null) {
                  Map<String, String> result = new HashMap<>();
                  result.put("message", "S3 Key missing");
            
                  res.status(404);
                  return result;
                }
                String content = tikaHandler.readFile(key);

                GateResumeParser gateParser = new GateResumeParser();
                HashMap<String, String> resume = gateParser.loadGateAndAnnie(content);
                //  System.out.println(" Resume " + resume);
                res.status(200);
                return resume;
            } catch (Exception e) {
              System.out.println("Exception " + e.getLocalizedMessage());
              res.status(500);
              return null;
            }
          
      }, new JsonTransformer());
    }
}