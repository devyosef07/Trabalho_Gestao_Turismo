import com.turismo.api.dao.TuristaDAO;
import com.turismo.api.model.Turista;
import java.util.List;

public class TestTuristaDAO {
    public static void main(String[] args) {
        TuristaDAO dao = new TuristaDAO();
        try {
            List<Turista> turistas = dao.getAllTuristas();
            System.out.println("Number of turistas found: " + turistas.size());
            if (turistas.isEmpty()) {
                System.out.println("Table TURISTA exists but is empty.");
            } else {
                for (Turista t : turistas) {
                    System.out.println(t);
                }
            }
        } catch (Exception e) {
            System.err.println("Error querying turistas: " + e.getMessage());
            e.printStackTrace();
        }
    }
}