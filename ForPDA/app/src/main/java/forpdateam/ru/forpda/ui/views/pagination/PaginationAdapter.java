package forpdateam.ru.forpda.ui.views.pagination;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.Locale;

import forpdateam.ru.forpda.App;
import forpdateam.ru.forpda.R;

/**
 * Created by radiationx on 26.10.16.
 */
public class PaginationAdapter extends BaseAdapter {
    private final String page = App.get().getString(R.string.pagination_page_number);
    private final LayoutInflater inflater;
    private final int[] data;

    public PaginationAdapter(Context context, int[] data) {
        this.data = data;
        inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return data.length;
    }

    @Override
    public Object getItem(int i) {
        return data[i];
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(android.R.layout.simple_list_item_single_choice, parent, false);
            holder = new ViewHolder();
            assert convertView != null;
            holder.text = (TextView) convertView.findViewById(android.R.id.text1);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }


        holder.text.setText(String.format(Locale.getDefault(), page, (int) getItem(position)));
        return convertView;
    }

    private class ViewHolder {
        public TextView text;
    }
}