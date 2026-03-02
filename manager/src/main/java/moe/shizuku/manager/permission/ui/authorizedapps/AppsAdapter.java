package moe.shizuku.manager.permission.ui.authorizedapps;

import android.content.pm.PackageInfo;

import java.util.List;

import moe.shizuku.manager.permission.ui.authorizedapps.components.AppViewHolder;
import moe.shizuku.manager.permission.ui.authorizedapps.components.EmptyViewHolder;
import moe.shizuku.manager.permission.ui.authorizedapps.components.ToggleAllViewHolder;
import rikka.recyclerview.BaseRecyclerViewAdapter;
import rikka.recyclerview.ClassCreatorPool;

public class AppsAdapter extends BaseRecyclerViewAdapter<ClassCreatorPool> {

    public AppsAdapter() {
        super();

        getCreatorPool().putRule(HeaderMarker.class, ToggleAllViewHolder.CREATOR);
        getCreatorPool().putRule(PackageInfo.class, AppViewHolder.CREATOR);
        getCreatorPool().putRule(Object.class, EmptyViewHolder.CREATOR);
        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        return getItemAt(position).hashCode();
    }

    @Override
    public ClassCreatorPool onCreateCreatorPool() {
        return new ClassCreatorPool();
    }

    public void updateData(List<PackageInfo> data) {
        getItems().clear();
        if (data.isEmpty()) {
            getItems().add(new Object());
        } else {
            getItems().add(new HeaderMarker());
            getItems().addAll(data);
        }
        notifyDataSetChanged();
    }

    public static final class HeaderMarker {
    }
}
