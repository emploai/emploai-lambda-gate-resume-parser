package com.emploai.apps.util;

import static gate.Utils.stringFor;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;


import org.apache.commons.lang.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import gate.Annotation;
import gate.AnnotationSet;
import gate.Corpus;
import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.Gate;
import gate.util.GateException;

public class GateResumeParser {
    @SuppressWarnings("unchecked")
    public HashMap loadGateAndAnnie(String content) throws GateException, IOException, URISyntaxException {
        try {

            URI gatefile = GateResumeParser.class.getResource("/resources/GATEFiles/gate.xml").toURI();
            System.setProperty("gate.site.config", gatefile.getPath());
            if (Gate.getGateHome() == null)
              Gate.setGateHome(new File(GateResumeParser.class.getResource("/resources/GATEFiles").getFile()));
            if (Gate.getPluginsHome() == null)
              Gate.setPluginsHome(new File(GateResumeParser.class.getResource("/resources/GATEFiles/plugins").getFile()));
            Gate.init();
            
            //  System.out.println(" Gate initialized ");

            Annie annie = new Annie();
            annie.initAnnie();

            Corpus corpus = Factory.newCorpus("Annie corpus");
            
            FeatureMap params = Factory.newFeatureMap();
            
            Document resume = Factory.newDocument(content);
            corpus.add(resume); 
            annie.setCorpus(corpus);
            annie.execute();

            Iterator iter = corpus.iterator();
            JSONObject parsedJSON = new JSONObject();
            //  System.out.println("Started parsing...");
            if (iter.hasNext()) {
              JSONObject basicsJSON = new JSONObject();
              Document doc = (Document) iter.next();
              AnnotationSet defaultAnnotSet = doc.getAnnotations();

              AnnotationSet curAnnSet;
              Iterator it;
              Annotation currAnnot;

              // Name
              curAnnSet = defaultAnnotSet.get("NameFinder");
              if (curAnnSet.iterator().hasNext()) {
                currAnnot = (Annotation) curAnnSet.iterator().next();
                String gender = (String) currAnnot.getFeatures().get("gender");
                if (gender != null && gender.length() > 0) {
                  basicsJSON.put("gender", gender);
                }

                // Needed name Features
                String[] nameFeatures = new String[] { "firstName", "middleName", "surname" };
                String name = "";
                for (String feature : nameFeatures) {
                  String s = (String) currAnnot.getFeatures().get(feature);
                  if (s != null && s.length() > 0) {
                    basicsJSON.put(feature, s.trim());
                    name = name + " " + s;
                  }
                }
                basicsJSON.put("name", name.trim());
              }

              curAnnSet = defaultAnnotSet.get("TitleFinder");
              if (curAnnSet.iterator().hasNext()) {
                currAnnot = (Annotation) curAnnSet.iterator().next();
                String label = stringFor(doc, currAnnot);
                if (label != null && label.length() > 0) {
                  basicsJSON.put("label", label);
                }
              }

              String[] annSections = new String[] { "EmailFinder", "AddressFinder", "PhoneFinder", "URLFinder" };
              String[] annKeys = new String[] { "email", "address", "phone", "url" };
              for (short i = 0; i < annSections.length; i++) {
                String annSection = annSections[i];
                curAnnSet = defaultAnnotSet.get(annSection);
                it = curAnnSet.iterator();
                JSONArray sectionArray = new JSONArray();
                while (it.hasNext()) {
                  currAnnot = (Annotation) it.next();
                  String s = stringFor(doc, currAnnot);
                  if (s != null && s.length() > 0) {
                    sectionArray.add(s);
                  }
                }
                if (sectionArray.size() > 0) {
                  basicsJSON.put(annKeys[i], sectionArray);
                }
              }
              if (!basicsJSON.isEmpty()) {
                parsedJSON.put("basics", basicsJSON);
              }

              HashMap<String,String> sectionMap = new HashMap<String, String>();
              sectionMap.put("summary","summary");
              sectionMap.put("education_and_training","education");
              sectionMap.put("skills","skills");
              sectionMap.put("accomplishments","accomplishments");
              sectionMap.put("awards","awards");
              sectionMap.put("credibility","recommendations");
              sectionMap.put("extracurricular","extracurricular");
              sectionMap.put("misc","misc");
              
              List<String> otherSections = new ArrayList<String>(sectionMap.keySet()); 
              for (String otherSection : otherSections) {
                curAnnSet = defaultAnnotSet.get(otherSection);
                it = curAnnSet.iterator();
                JSONArray subSections = new JSONArray();
                while (it.hasNext()) {
                  JSONObject subSection = new JSONObject();
                  currAnnot = (Annotation) it.next();
                  String key = (String) currAnnot.getFeatures().get("sectionHeading");
                  String value = stringFor(doc, currAnnot);
                  if (!StringUtils.isBlank(key) && !StringUtils.isBlank(value)) {
                    subSection.put(key, value);
                  }
                  if (!subSection.isEmpty()) {
                    subSections.add(subSection);
                  }
                }
                if (!subSections.isEmpty()) {
                  parsedJSON.put(sectionMap.get(otherSection), subSections);
                }
              }

              curAnnSet = defaultAnnotSet.get("work_experience");
              it = curAnnSet.iterator();
              JSONArray workExperiences = new JSONArray();
              while (it.hasNext()) {
                JSONObject workExperience = new JSONObject();
                currAnnot = (Annotation) it.next();
                String key = (String) currAnnot.getFeatures().get("sectionHeading");
                if (key.equals("work_experience_marker")) {
                  // JSONObject details = new JSONObject();
                  //  String[] annotations = new String[] { "startDate", "endDate", "jobtitle", "organization" };
                  HashMap<String,String> annotationMap = new HashMap<String, String>();
                  annotationMap.put("startDate","startDate");
                  annotationMap.put("endDate","endDate");
                  annotationMap.put("jobtitle","jobtitle");
                  annotationMap.put("organization","company");
                  
                  List<String> annotations = new ArrayList<String>(annotationMap.keySet()); 
              
                  for (String annotation : annotations) {
                    String v = (String) currAnnot.getFeatures().get(annotation);
                    if (!StringUtils.isBlank(v)) {
                      workExperience.put(annotationMap.get(annotation), v);
                    }
                  }
                  key = "summary";

                }
                String value = stringFor(doc, currAnnot);
                if (!StringUtils.isBlank(key) && !StringUtils.isBlank(value)) {
                  workExperience.put(key, value);
                }
                if (!workExperience.isEmpty()) {
                  workExperiences.add(workExperience);
                }

              }
              if (!workExperiences.isEmpty()) {
                parsedJSON.put("work", workExperiences);
              }

            }
            //  System.out.println(" ---------- parsedJSON " + parsedJSON);
            return parsedJSON;
          
        } catch (Exception e) {
            System.out.println("EXception : " + e.toString());
            throw e;
        }
    }
}
