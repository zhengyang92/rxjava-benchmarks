package co.smartreceipts.android.filters;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import co.smartreceipts.android.model.Trip;

/**
 * A filter implementation of {@link AndFilter} for {@link Trip}
 * 
 * @author Will Baumann
 * @since July 08, 2014
 * 
 */
public class TripAndFilter extends AndFilter<Trip>{

	public TripAndFilter() {
		super();
	}
	
	public TripAndFilter(List<Filter<Trip>> filters) {
		super(filters);
	}
	
	protected TripAndFilter(JSONObject json) throws JSONException {
		super(json);
	}
	
	
	@Override
	Filter<Trip> getFilter(JSONObject json) throws JSONException {
		return FilterFactory.getTripFilter(json);
	}

}
