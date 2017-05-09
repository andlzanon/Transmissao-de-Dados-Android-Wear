package andl.zanon.wearapis;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

/**
 * Created by Andre on 28/04/2017.
 */

public class Imagem {

    public static Bitmap getImagem(Context context, String caminhoImagem, int altura, int largura){
        Bitmap bitmap = null;

        try{
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(context.getAssets().open(caminhoImagem), null, options);

            int larguraFoto = options.outWidth;
            int alturaFoto = options.outHeight;

            int escala = Math.min(larguraFoto/largura, alturaFoto/altura);
            options.inJustDecodeBounds = false;
            options.inSampleSize = escala;

            bitmap = BitmapFactory.decodeStream(context.getAssets().open(caminhoImagem), null, options);

        }catch (Exception e){
            e.printStackTrace();
        }

        return bitmap;
    }
}
