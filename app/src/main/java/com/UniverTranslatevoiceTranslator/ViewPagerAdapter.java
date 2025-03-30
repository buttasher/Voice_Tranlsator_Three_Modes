package com.UniverTranslatevoiceTranslator;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.UniverTranslatevoiceTranslator.fragments.CameraTranslator;
import com.UniverTranslatevoiceTranslator.fragments.ConversationTranslator;
import com.UniverTranslatevoiceTranslator.fragments.VoiceTranslator;

public class ViewPagerAdapter extends FragmentStateAdapter {
    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new CameraTranslator();
            case 1:
                return new VoiceTranslator();
            case 2:
                return new ConversationTranslator();
            default:
                return new CameraTranslator();
        }
    }

    @Override
    public int getItemCount() {
        return 3; // Number of fragments
    }
}
