
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WriteCSVEvaluationMetrics {

	private WriteCSVEvaluationMetrics() {

	}

	/**
	 * Metodo che prepara le singole linee da scrivere nel file CSV
	 * 
	 * @param evaluationData
	 * @param indexRelease
	 * @param classifier
	 * @param datasetName
	 * @param classifierName
	 */
	private static void preparedLine(List<String> evaluationData, int indexRelease, EvaluationInfo classifier,
			String datasetName, String classifierName) {

		evaluationData.add(datasetName);
		evaluationData.add(String.valueOf(indexRelease + 1));
		evaluationData.add(classifierName);
		evaluationData.add(String.valueOf(classifier.getPrecisionValues().get(indexRelease)));
		evaluationData.add(String.valueOf(classifier.getRecallValues().get(indexRelease)));
		evaluationData.add(String.valueOf(classifier.getAucValues().get(indexRelease)));
		evaluationData.add(String.valueOf(classifier.getKappaValues().get(indexRelease)));
	}

	/**
	 * Metodo che scrive su un file csv le metriche per la validazione dei
	 * classificatori
	 * 
	 * @throws IOException
	 * 
	 * 
	 */
	public static void writeCsv(String datasetName, List<EvaluationInfo> classifiers) throws IOException {

		try (FileWriter csvWriter = new FileWriter(datasetName + "_evaluation_metrics.csv")) {

			csvWriter.append("Dataset");
			csvWriter.append(",");
			csvWriter.append("#TrainingRelease");
			csvWriter.append(",");
			csvWriter.append("Classifier");
			csvWriter.append(",");
			csvWriter.append("Precision");
			csvWriter.append(",");
			csvWriter.append("Recall");
			csvWriter.append(",");
			csvWriter.append("AUC");
			csvWriter.append(",");
			csvWriter.append("Kappa");
			csvWriter.append("\n");

			int totalNumberOfReleases = classifiers.get(0).getAucValues().size();

			// Si parte da 1 in modo da saltare le metriche del primo run perchè non ci sono
			// dati di training
			for (int i = 0; i < totalNumberOfReleases; i++) {

				// Metriche relative a Naive Bayes
				ArrayList<String> evaluationData = new ArrayList<>();
				preparedLine(evaluationData, i, classifiers.get(0), datasetName, "NaiveBayes");

				csvWriter.append(String.join(",", evaluationData));
				csvWriter.append("\n");

				// Metriche relative a Random Forest
				evaluationData = new ArrayList<>();
				preparedLine(evaluationData, i, classifiers.get(1), datasetName, "RandomForest");

				csvWriter.append(String.join(",", evaluationData));
				csvWriter.append("\n");

				// Metriche relative a IBk
				evaluationData = new ArrayList<>();
				preparedLine(evaluationData, i, classifiers.get(2), datasetName, "IBk");

				csvWriter.append(String.join(",", evaluationData));
				csvWriter.append("\n");
			}

			csvWriter.flush();
		}

	}

}
