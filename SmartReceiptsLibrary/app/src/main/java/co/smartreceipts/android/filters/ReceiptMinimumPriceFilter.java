package co.smartreceipts.android.filters;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import co.smartreceipts.android.R;
import co.smartreceipts.android.model.Receipt;

public class ReceiptMinimumPriceFilter implements Filter<Receipt> {

	private final static String MIN_PRICE = "minprice";
	private final static String CURRENCY_CODE = "currencycode";

	private final float mMinPrice;
	private final String mCurrencyCode;

	public ReceiptMinimumPriceFilter(float minPrice, String currencyCode) {
		if (currencyCode == null)
			throw new IllegalArgumentException(
					"ReceiptMinPriceFilter requires non-null currencyCode");

		mMinPrice = minPrice;
		mCurrencyCode = currencyCode;
	}

	public ReceiptMinimumPriceFilter(JSONObject json) throws JSONException {
		this.mMinPrice = (float) json.getDouble(MIN_PRICE);
		this.mCurrencyCode = json.getString(CURRENCY_CODE);
	}

	@Override
	public boolean accept(Receipt t) {
		return t.getPrice().getPriceAsFloat() >= mMinPrice
				&& t.getPrice().getCurrencyCode().equalsIgnoreCase(mCurrencyCode);
	}

	@Override
	public JSONObject getJsonRepresentation() throws JSONException {
		final JSONObject json = new JSONObject();
		json.put(FilterFactory.CLASS_NAME, this.getClass().getName());
		json.put(MIN_PRICE, mMinPrice);
		json.put(CURRENCY_CODE, mCurrencyCode);
		return json;
	}

	@Override
	public List<Filter<Receipt>> getChildren() {
		return null;
	}

	@Override
	public int getNameResource() {
		return R.string.filter_name_receipt_min_price;
	}

	@Override
	public FilterType getType() {
		return FilterType.Float;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((mCurrencyCode == null) ? 0 : mCurrencyCode.hashCode());
		result = prime * result + Float.floatToIntBits(mMinPrice);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;

		if (obj == null)
			return false;

		if (getClass() != obj.getClass())
			return false;

		ReceiptMinimumPriceFilter other = (ReceiptMinimumPriceFilter) obj;

		if (mCurrencyCode == null) {
			if (other.mCurrencyCode != null)
				return false;
		} else if (!mCurrencyCode.equals(other.mCurrencyCode))
			return false;

		if (Float.floatToIntBits(mMinPrice) != Float
				.floatToIntBits(other.mMinPrice))
			return false;

		return true;
	}
}
