
import java.util.ArrayList;
import java.util.List;

/**
 * Classe che modella le informazioni relative alle metriche per la validazione
 * di un classificatore
 * 
 * @author jhaloyn
 *
 */

public class EvaluationInfo {

	private ArrayList<Double> aucValues;
	private ArrayList<Double> kappaValues;
	private ArrayList<Double> precisionValues;
	private ArrayList<Double> recallValues;

	public EvaluationInfo() {

		aucValues = new ArrayList<>();
		kappaValues = new ArrayList<>();
		precisionValues = new ArrayList<>();
		recallValues = new ArrayList<>();
	}

	public List<Double> getAucValues() {
		return aucValues;
	}

	public void addAucValue(Double aucValue) {
		this.aucValues.add(aucValue);
	}

	public List<Double> getKappaValues() {
		return kappaValues;
	}

	public void addKappaValues(Double kappaValue) {
		this.kappaValues.add(kappaValue);
	}

	public List<Double> getPrecisionValues() {
		return precisionValues;
	}

	public void addPrecisionValues(Double precisionValue) {
		this.precisionValues.add(precisionValue);
	}

	public List<Double> getRecallValues() {
		return recallValues;
	}

	public void addRecallValues(Double recallValue) {
		this.recallValues.add(recallValue);
	}
}
