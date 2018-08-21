package openfoodfacts.github.scrachx.openfood.models;

import java.util.ArrayList;
import java.util.Map;

/**
 * Intermediate class between {@link openfoodfacts.github.scrachx.openfood.models.AllergensWrapper} and {@link openfoodfacts.github.scrachx.openfood.models.Allergen}
 *
 * @author Lobster 2018-03-04
 * @author ross-holloway94 2018-03-14
 */

public class AllergenResponse {

    private String uniqueAllergenID;

    private Map<String, String> names;

    /**
     * Constructor.
     *
     * @param uniqueAllergenId Unique ID of the allergen
     * @param names            Map of key: Country code, value: Translated name of allergen.
     */
    public AllergenResponse(String uniqueAllergenId, Map<String, String> names) {
        this.uniqueAllergenID = uniqueAllergenId;
        this.names = names;
    }

    /**
     * Converts an AllergenResponse object into a new Allergen object.
     *
     * @return The newly constructed Allergen object.
     */
    public Allergen map() {
        Allergen allergen = new Allergen(uniqueAllergenID, new ArrayList<>());
        for (Map.Entry<String, String> name : names.entrySet()) {
            allergen.getNames().add(new AllergenName(allergen.getTag(), name.getKey(), name.getValue()));
        }

        return allergen;
    }

    public String getUniqueAllergenID() {
        return uniqueAllergenID;
    }

    public void setUniqueAllergenID(String uniqueAllergenID) {
        this.uniqueAllergenID = uniqueAllergenID;
    }

    public Map<String, String> getNames() {
        return names;
    }

    public void setNames(Map<String, String> names) {
        this.names = names;
    }

}
