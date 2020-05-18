
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import weka.attributeSelection.CfsSubsetEval;
import weka.attributeSelection.GreedyStepwise;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.lazy.IBk;
import weka.classifiers.trees.RandomForest;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.Filter;
import weka.filters.supervised.attribute.AttributeSelection;

/**
 * Questa classe si occupa di creare i dataset di training e di testing, e di
 * eseguire la tecnica di validazione WalkForward per i classificatori: Naive
 * Bayes, Random Forest e IBk. La validazione viene effettuata sia senza feature
 * selection, che con l'uso della feature selection
 * 
 * @author jhaloyn
 *
 */
public class WalkForward {

	private int totalReleases;
	private List<EvaluationInfo> classifiersEvalInfo = new ArrayList<>();
	private List<EvaluationInfo> classifiersEvalInfoWithFeatureSelection = new ArrayList<>();
	private File trainingFile;
	private File testingFile;
	private String trainingAndTestingDatasetsPath;

	public WalkForward(String trainingAndTestingDatasetsPath) {
		this.totalReleases = 1;
		this.trainingAndTestingDatasetsPath = trainingAndTestingDatasetsPath;

		for (int i = 0; i < 3; i++) {
			classifiersEvalInfo.add(new EvaluationInfo());
			classifiersEvalInfoWithFeatureSelection.add(new EvaluationInfo());
		}
	}

	/**
	 * Metodo che si occupa di creare i file di training e di testing per ogni run
	 * del Walk Forward
	 * 
	 * @throws IOException
	 */
	private void createTrainingAndTestFile(int numberOfTrainingReleases, String datasetPath) throws IOException {

		String line = "";
		String cvsSplitBy = ",";

		trainingFile = new File(
				trainingAndTestingDatasetsPath + "trainingDataset" + numberOfTrainingReleases + ".arff");
		testingFile = new File(trainingAndTestingDatasetsPath + "testingDataset" + numberOfTrainingReleases + ".arff");

		try (BufferedReader br = new BufferedReader(new FileReader(datasetPath));
				FileWriter fwTraining = new FileWriter(trainingFile, true);
				FileWriter fwTesting = new FileWriter(testingFile, true)) {

			createHeaderFileArff(fwTraining);
			createHeaderFileArff(fwTesting);

			// Saltiamo la prima riga del dataset che contiene i nomi delle colonne
			line = br.readLine();

			while ((line = br.readLine()) != null) {

				String[] fields = line.split(cvsSplitBy);
				int releaseNumber = Integer.parseInt(fields[0]);

				// Mettiamo nel file .arff di training solo le istanze (release-classe) che nel
				// corrente run devono essere usate come istanze di training.
				if (releaseNumber <= numberOfTrainingReleases) {
					List<String> trainingLine = new ArrayList<>();

					// Per ogni campo del file csv si crea una nuova linea con solo le metriche da
					// inserire nel file di training
					trainingLine = Arrays.asList(fields).subList(2, fields.length);

					fwTraining.append(String.join(",", trainingLine));
					fwTraining.append("\n");
					// La release successiva all'ultima di training è quella usata per il dataset di
					// test
				} else if (releaseNumber == numberOfTrainingReleases + 1) {

					List<String> testingLine = new ArrayList<>();

					// Per ogni campo del file csv si crea una nuova linea con solo le metriche da
					// inserire nel file di testing
					testingLine = Arrays.asList(fields).subList(2, fields.length);

					fwTesting.append(String.join(",", testingLine));
					fwTesting.append("\n");
				} else {
					break;
				}
			}

		}
	}

	/**
	 * Metodo che si occupa di formattare correttamente l'header dei file di testing
	 * e di training, secondo il formato .arff previsto da weka
	 * 
	 * @param fw
	 * @throws IOException
	 */
	private void createHeaderFileArff(FileWriter fw) throws IOException {

		fw.write("@RELATION buggy\n\n");
		fw.write("@ATTRIBUTE LOC_Touched  NUMERIC\n");
		fw.write("@ATTRIBUTE LOC_Added  NUMERIC\n");
		fw.write("@ATTRIBUTE MaxLOC_Added  NUMERIC\n");
		fw.write("@ATTRIBUTE AvgLOC_Added  NUMERIC\n");
		fw.write("@ATTRIBUTE Churn  NUMERIC\n");
		fw.write("@ATTRIBUTE Max_Churn  NUMERIC\n");
		fw.write("@ATTRIBUTE Avg_Churn  NUMERIC\n");
		fw.write("@ATTRIBUTE NR  NUMERIC\n");
		fw.write("@ATTRIBUTE NFix  NUMERIC\n");
		fw.write("@ATTRIBUTE Buggy  {Yes,No}\n\n");
		fw.write("@DATA\n");
	}

	/**
	 * Metodo che calcola il numero totale delle release contenute nel dataset
	 * 
	 * @param datasetPath
	 * @throws IOException
	 */
	private void findTotalNumberOfReleases(String datasetPath) throws IOException {

		String line = "";
		String cvsSplitBy = ",";

		try (BufferedReader br = new BufferedReader(new FileReader(datasetPath))) {

			// Saltiamo la prima riga del dataset che contiene i nomi delle colonne
			line = br.readLine();

			while ((line = br.readLine()) != null) {

				String[] fields = line.split(cvsSplitBy);
				int releaseNumber = Integer.parseInt(fields[0]);

				// Contiamo quante sono le release totali
				if (releaseNumber != totalReleases) {
					totalReleases++;
				}
			}
		}

	}

	/**
	 * Metodo che esegue la tecnica di validazio Walk Forward per i classificatori:
	 * Naive Bayes, Random Forest ed IBk. La validazione viene fatta sia senza
	 * feature selection che con la feature selection
	 * 
	 * @param datasetPath
	 * @throws IOException
	 * @throws WekaException
	 */
	public void walkForwardExecution(String datasetPath) throws IOException, WekaException {

		findTotalNumberOfReleases(datasetPath);

		// Si compie un numero di run pari al numero delle release-1. Per ogni run si
		// validano tutti i classificatori
		for (int i = 1; i < totalReleases; i++) {

			createTrainingAndTestFile(i, datasetPath);
			try {

				DataSource sourceTraining = new DataSource(trainingAndTestingDatasetsPath + trainingFile.getName());
				Instances training = sourceTraining.getDataSet();
				DataSource sourceTesting = new DataSource(trainingAndTestingDatasetsPath + testingFile.getName());
				Instances testing = sourceTesting.getDataSet();

				// Si crea il filtro per la AttributeSelection
				AttributeSelection filter = new AttributeSelection();
				// Si impostano l'evaluator e l'algoritmo di ricerca per il filtro

				// Valuta gli attributi considerando la loro singola capacità predittiva e
				// considerando il grado di ridondanza tra tutti gli attributi
				// Sono preferiti sottoinsiemi di attributi altamente correlati con l'attributo
				// da predire, ma poco correlati tra loro
				CfsSubsetEval eval = new CfsSubsetEval();
				// Si imposta l'algoritmo per la ricerca backward
				GreedyStepwise search = new GreedyStepwise();
				search.setSearchBackwards(true);
				filter.setEvaluator(eval);
				filter.setSearch(search);
				// Si specifica il dataset da filtrare
				filter.setInputFormat(training);
				// Si applica il filtro per la feature selection
				Instances filteredTraining = Filter.useFilter(training, filter);
				Instances filteredTesting = Filter.useFilter(testing, filter);

				// Si dice a WEKA di non usare l'ultimo attributo perchè è quello che vogliamo
				// stimare
				int numAttr = training.numAttributes();
				training.setClassIndex(numAttr - 1);
				testing.setClassIndex(numAttr - 1);
				int numAttrFiltered = filteredTraining.numAttributes();
				filteredTraining.setClassIndex(numAttrFiltered - 1);
				filteredTesting.setClassIndex(numAttrFiltered - 1);

				System.out.println("RUN " + i);
				System.out.println("NUmero attributi senza FS: " + numAttr);
				System.out.println("NUmero attributi con FS: " + numAttrFiltered);

				System.out.println("Set the evaluator : " + eval.toString());
				System.out.println("Set the search : " + search.toString());
				System.out.println("-------------------------------------------\n");

				Evaluation evalClassifier = new Evaluation(training);
				Evaluation evalClassifierFiltered = new Evaluation(filteredTraining);

				// Usiamo il classificatore NaiveBayes
				NaiveBayes naiveBayesClassifier = new NaiveBayes();
				// Validazione senza feature selection
				naiveBayesClassifier.buildClassifier(training);
				evalClassifier.evaluateModel(naiveBayesClassifier, testing);
				addEvaluationMetrics(classifiersEvalInfo.get(0), evalClassifier);
				// Validazione con feature selection
				naiveBayesClassifier.buildClassifier(filteredTraining);
				evalClassifierFiltered.evaluateModel(naiveBayesClassifier, filteredTesting);
				addEvaluationMetrics(classifiersEvalInfoWithFeatureSelection.get(0), evalClassifierFiltered);

				// Usiamo il classificatore RandomForest
				RandomForest randomForestClassifier = new RandomForest();
				// Validazione senza feature selection
				randomForestClassifier.buildClassifier(training);
				evalClassifier.evaluateModel(randomForestClassifier, testing);
				addEvaluationMetrics(classifiersEvalInfo.get(1), evalClassifier);
				// Validazione con feature selection
				randomForestClassifier.buildClassifier(filteredTraining);
				evalClassifierFiltered.evaluateModel(randomForestClassifier, filteredTesting);
				addEvaluationMetrics(classifiersEvalInfoWithFeatureSelection.get(1), evalClassifierFiltered);

				// Usiamo il classificatore IBk
				IBk ibkClassifier = new IBk();
				// Validazione senza feature selection
				ibkClassifier.buildClassifier(training);
				evalClassifier.evaluateModel(ibkClassifier, testing);
				addEvaluationMetrics(classifiersEvalInfo.get(2), evalClassifier);
				// Validazione con feature selection
				ibkClassifier.buildClassifier(filteredTraining);
				evalClassifierFiltered.evaluateModel(ibkClassifier, filteredTesting);
				addEvaluationMetrics(classifiersEvalInfoWithFeatureSelection.get(2), evalClassifierFiltered);

			} catch (Exception e) {
				throw new WekaException("Eccezione liberie WEKA");
			}
		}
	}

	/**
	 * Metodo che restituisce una lista con tutte le metriche di ogni
	 * classificatore, raccolte a seguito dell'applicazione della tecnica di
	 * validazione Walk Forward
	 * 
	 * @return
	 */
	public List<EvaluationInfo> getClassifiersEvaluationInfo() {
		return classifiersEvalInfo;
	}

	/**
	 * Metodo che restituisce una lista con tutte le metriche di ogni
	 * classificatore, raccolte a seguito dell'applicazione della tecnica di
	 * validazione Walk Forward e della feature selection
	 * 
	 * @return
	 */
	public List<EvaluationInfo> getClassifiersEvaluationInfoWithFeatureSelection() {
		return classifiersEvalInfoWithFeatureSelection;
	}

	/**
	 * Metodo che ad ogni run aggiunge le metriche di validazione dei classificatori
	 * alla lista delle metriche costruita per ogni classificatore
	 * 
	 * @param classifierEvaluationInfo
	 * @param evalClassifier
	 */
	private void addEvaluationMetrics(EvaluationInfo classifierEvaluationInfo, Evaluation evalClassifier) {

		// Passando come parametro 0 ai vari metodi di evalClassifier selezioniamo il
		// primo valore previsto per la variabile da stimare. Con 1 passiamo il secondo
		// valore

		classifierEvaluationInfo.addAucValue((evalClassifier.areaUnderROC(0)));
		classifierEvaluationInfo.addKappaValues(evalClassifier.kappa());
		classifierEvaluationInfo.addPrecisionValues(evalClassifier.precision(0));
		classifierEvaluationInfo.addRecallValues(evalClassifier.recall(0));

	}

}
