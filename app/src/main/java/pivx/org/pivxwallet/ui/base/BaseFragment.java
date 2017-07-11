package pivx.org.pivxwallet.ui.base;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import pivx.org.pivxwallet.PivxApplication;
import pivx.org.pivxwallet.module.PivxModule;

/**
 * Created by furszy on 6/29/17.
 */

public class BaseFragment extends Fragment {

    protected PivxApplication pivxApplication;
    protected PivxModule pivxModule;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pivxApplication = PivxApplication.getInstance();
        pivxModule = pivxApplication.getModule();
    }
}
