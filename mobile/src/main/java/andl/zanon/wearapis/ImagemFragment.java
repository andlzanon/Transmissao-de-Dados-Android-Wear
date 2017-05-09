package andl.zanon.wearapis;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

/**
 * Created by Andre on 28/04/2017.
 */

public class ImagemFragment extends Fragment {

    private static final String EXTRA_IMAGEM = "Imagem";

    public static ImagemFragment novaInstancia(String imagem){
        ImagemFragment imagemFragment = new ImagemFragment();
        Bundle parametros = new Bundle();
        parametros.putString(EXTRA_IMAGEM, imagem);

        imagemFragment.setArguments(parametros);
        return imagemFragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        String imagem = getArguments().getString(EXTRA_IMAGEM);
        ImageView imageView = new ImageView(getActivity());
        imageView.setImageBitmap(Imagem.getImagem(getActivity(), imagem, 800, 600));
        return imageView;
    }
}
