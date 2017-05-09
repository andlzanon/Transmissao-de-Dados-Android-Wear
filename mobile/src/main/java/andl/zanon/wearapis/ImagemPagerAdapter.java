package andl.zanon.wearapis;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

/**
 * Created by Andre on 28/04/2017.
 */

public class ImagemPagerAdapter extends FragmentPagerAdapter {

    public ImagemPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        ImagemFragment imagemFragment = new ImagemFragment();

        switch (position) {
            case 0:
                return imagemFragment.novaInstancia("Palmeiras_enea.jpg");

            case 1:
                return imagemFragment.novaInstancia("Palmeiras_enea2.jpg");

            case 2:
                return imagemFragment.novaInstancia("Palmeiras_enea3.jpg");

            case 3:
                return imagemFragment.novaInstancia("Palmeiras_enea4.jpg");

            default:
                return imagemFragment.novaInstancia("Palmeiras_enea.jpg");
        }
    }

    @Override
    public int getCount() {
        return Constantes.TOTAL_IMAGENS;
    }
}
