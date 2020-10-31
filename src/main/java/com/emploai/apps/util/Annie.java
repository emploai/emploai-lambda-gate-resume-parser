package com.emploai.apps.util;

import java.io.File;
import java.io.IOException;

import gate.Gate;
import gate.Corpus;
import gate.CorpusController;
import gate.util.GateException;
import gate.util.persistence.PersistenceManager;

public class Annie {

	private CorpusController annieController;

	public void initAnnie() throws GateException, IOException {
		File gateHome = Gate.getGateHome();
		File annieGapp = new File(gateHome, "ANNIEResumeParser.gapp");
		annieController = (CorpusController) PersistenceManager.loadObjectFromFile(annieGapp);
	}

	public void setCorpus(Corpus corpus) {
		annieController.setCorpus(corpus);
	}

	public void execute() throws GateException {
		annieController.execute();
	}
}
