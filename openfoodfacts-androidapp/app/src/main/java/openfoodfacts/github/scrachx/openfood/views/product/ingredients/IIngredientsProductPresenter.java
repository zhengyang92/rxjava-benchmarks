package openfoodfacts.github.scrachx.openfood.views.product.ingredients;

import java.util.List;

import openfoodfacts.github.scrachx.openfood.models.AdditiveName;
import openfoodfacts.github.scrachx.openfood.utils.ProductInfoState;

/**
 * Created by Lobster on 17.03.18.
 */

public interface IIngredientsProductPresenter {

    interface Actions {
        void loadAdditives();

        void dispose();
    }

    interface View {
        void showAdditives(List<AdditiveName> additives);

        void showAdditivesState(@ProductInfoState String state);
    }

}