package co.smartreceipts.android.filters;

import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Date;
import java.util.List;
import java.util.TimeZone;

import co.smartreceipts.android.R;
import co.smartreceipts.android.model.Trip;

public class TripEndsOnOrBeforeDayFilter implements Filter<Trip> {

	private final static String DATE = "date";
	private final static String TIMEZONE = "timezone";

	private final Date mDate;
	private final TimeZone mTimeZone;

	public TripEndsOnOrBeforeDayFilter(Date date, TimeZone timeZone) {
		if (date == null || timeZone == null)
			throw new IllegalArgumentException(
					"ReceiptOnOrAfterDayFilter requires non-null date and timezone");

		mDate = date;
		mTimeZone = timeZone;
	}

	public TripEndsOnOrBeforeDayFilter(JSONObject json) throws JSONException {
		mDate = new Date(json.getLong(DATE));
		mTimeZone = TimeZone.getTimeZone(json.getString(TIMEZONE));
	}

	@Override
	public boolean accept(Trip t) {
		return FilterUtils.isOnOrBefore(t.getEndDate(), t.getEndTimeZone(), mDate, mTimeZone);
	}

	@Override
	public JSONObject getJsonRepresentation() throws JSONException {
		final JSONObject json = new JSONObject();
		json.put(FilterFactory.CLASS_NAME, this.getClass().getName());
		json.put(DATE, mDate.getTime());
		json.put(TIMEZONE, mTimeZone.getID());
		return json;
	}

	@Override
	public List<Filter<Trip>> getChildren() {
		return null;
	}

	@Override
	public int getNameResource() {
		return R.string.filter_name_trip_ends_on_or_before;
	}

	@Override
	public FilterType getType() {
		return FilterType.Date;
	}

	@Override
	public int hashCode() {
		final int dtHash = (mDate == null) ? 0 : mDate.hashCode();
		final int tzHash = (mTimeZone == null) ? 0 : mTimeZone.getID().hashCode();
		final int prime = 31;
		int result = 1;
		result = prime * result + dtHash;
		result = prime * result + tzHash;
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

		TripEndsOnOrBeforeDayFilter other = (TripEndsOnOrBeforeDayFilter) obj;

		if (mDate == null) {
			if (other.mDate != null)
				return false;
		} else if (!mDate.equals(other.mDate))
			return false;

		if (mTimeZone == null) {
			if (other.mTimeZone != null)
				return false;
		} else if (!mTimeZone.equals(other.mTimeZone))
			return false;

		return true;
	}

}
