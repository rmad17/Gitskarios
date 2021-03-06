package com.alorma.github.ui;

import com.alorma.github.dagger.BusModule;
import com.alorma.github.ui.activity.MainActivity;
import com.alorma.github.ui.adapter.NotificationsAdapter;
import com.alorma.github.ui.fragment.NotificationsFragment;
import com.alorma.github.ui.view.NotificationsActionProvider;

import dagger.Module;

/**
 * Created by Bernat on 01/03/2015.
 */
@Module(
        includes = {
                BusModule.class
        },
        injects = {
                NotificationsAdapter.class,
                NotificationsFragment.class,
                NotificationsActionProvider.class,
                MainActivity.class
        }
)
public class UiModule {
}
