
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.io.FileUtils;

public class EvaluationClassifiersMain {

	private static final String DATASET_NAME = "Avro";

	public static void main(String[] args) {

		Properties properties = new Properties();
		try {
			// Setting file di log
			properties.load(new FileInputStream("logFile_path.properties"));
			String logFilePath = properties.getProperty("PATH");
			System.setProperty("java.util.logging.config.file", logFilePath);

			// lettura del path della cartella root del progetto
			properties.load(new FileInputStream("root_path.properties"));
			String rootPath = properties.getProperty("PATH");
			String datasetPath = properties.getProperty("PATH") + DATASET_NAME + "_dataset.csv";

			// creazione della directory che contiene i file di training e di test
			// per la validazione del modello
			String trainingAndTestingDatasetsPath = rootPath + "training_and_testing_datasets/";
			// Se la cartella già esiste la eliminiamo con tutti i file che contiene
			File directory = new File(trainingAndTestingDatasetsPath);
			if (directory.isDirectory()) {
				FileUtils.deleteDirectory(new File(trainingAndTestingDatasetsPath));
			}
			new File(trainingAndTestingDatasetsPath).mkdir();

			WalkForward walkForward = new WalkForward(trainingAndTestingDatasetsPath);
			walkForward.walkForwardExecution(datasetPath);

			// Generazione file CSV con le metriche di validazione senza feature selection
			WriteCSVEvaluationMetrics.writeCsv(DATASET_NAME, walkForward.getClassifiersEvaluationInfo());
			// Generazione file CSV con le metriche di validazione con feature selection
			WriteCSVEvaluationMetrics.writeCsv(DATASET_NAME + "_with_feature_selection",
					walkForward.getClassifiersEvaluationInfoWithFeatureSelection());

		} catch (WekaException | IOException e) {
			// Logger.getLogger(WalkForward.class.getName()).log(Level.SEVERE, e.toString(),
			// e);
			e.printStackTrace();
		} catch (Exception e) {
			// Logger.getLogger(EvaluationClassifiersMain.class.getName()).log(Level.SEVERE,
			// e.toString(), e);
			e.printStackTrace();
		}
	}
}
