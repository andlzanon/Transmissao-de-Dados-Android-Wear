package andl.zanon.wearapis;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.ByteArrayOutputStream;

public class MainActivity extends AppCompatActivity
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        MessageApi.MessageListener, ViewPager.OnPageChangeListener{

    private ViewPager mViewPager;
    private GoogleApiClient mGoogleApiClient;
    private java.util.List<Node> mNodes;
    //tarefa feita de forma assíncrona em prol de desempenho
    private AsyncTask<Void, Void, PutDataMapRequest> mAsyncTask;
    // irá armazenar a Uri da tranmissao de dados entre relógio e celular
    private Uri mUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mViewPager = (ViewPager)findViewById(R.id.viewPager);
        ImagemPagerAdapter mAdapter = new ImagemPagerAdapter(getSupportFragmentManager());
        mViewPager.setAdapter(mAdapter);
        mViewPager.addOnPageChangeListener(this);


        /* definiçao do google Client API que realiza toda interface de comunicação.
         * ponto de entrada para integração com o Google Play Services que justamente faz a interface
         * de conecção entre relogio e celilar */
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        Log.d("TesteDataTransmition", "Iniciou1");
    }

    @Override
    protected void onStart(){
        super.onStart();
        if(!mGoogleApiClient.isConnected())
            mGoogleApiClient.connect();

        Log.d("TesteDataTransmition", "Conectou");
    }

    @Override
    protected void onStop(){
        super.onStop();

        //deleta todos os dados armazenados no DataItem
        if(mUri != null){
            Wearable.DataApi.deleteDataItems(mGoogleApiClient, mUri);
        }

        //apaga todos os nós conectados ao celular
        if(mNodes != null){
            mNodes.clear();
        }

        //envia mensagem de saida para todos os nós
        if(mNodes != null && mGoogleApiClient.isConnected()){
            for(Node node : mNodes){
                Wearable.MessageApi.sendMessage(mGoogleApiClient, node.getId(), Constantes.CAMINHO_SAIR, null);
            }
        }

        //desconecta a GoogleApiClient
        if(mGoogleApiClient.isConnected())
            mGoogleApiClient.disconnect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        //conecta aos nos existentes
        conectaAosNos();
        //adciona um ouvido para ouvir mensagens
        Wearable.MessageApi.addListener(mGoogleApiClient, this);
        //atualiza pela primeira vez os dados
        atualizaDataItem(mViewPager.getCurrentItem());
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    // metodo responsável por receber a mensagem do método onStartCommand da classe ApresentacaoService
    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if(Constantes.CAMINHO_PROX_ANT.equals(messageEvent.getPath())){
            switch (messageEvent.getData()[0]){
                case 0:
                    voltaImagem();
                    break;

                case 1:
                    avancaImagem();
                    break;

                default:
                    break;
            }
        }
    }

    @Override
    public void onPageSelected(int position) {
        Log.d("Mudanca da Pag", "Mudou");
        // caso qualquer página for acessada é necessário atualizar os dados
        atualizaDataItem(position);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    /* Atualiza os dados correspondentes entre celular e relógio de forma a estarem sempre sincronizados
     * será utilizado sempre que mudarmos de foto */
    public void atualizaDataItem(final int posicaoAtual){

        if(!mGoogleApiClient.isConnected())
            return;

        mAsyncTask = new AsyncTask<Void, Void, PutDataMapRequest>() {
            @Override
            protected PutDataMapRequest doInBackground(Void... params) {
                //cria DataMapReques. O dataMap cria um caminho para as informações passares do wear para o mobile
                PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(Constantes.CAMINHO_DADOS);
                //cria DataMap. O dataMap é o "pacote" de informações que se quer passar do mobile para o wear
                DataMap map = putDataMapRequest.getDataMap();
                mUri = putDataMapRequest.getUri();

                map.putInt(Constantes.KEY_FOTO_ATUAL, posicaoAtual);
                map.putInt(Constantes.KEY_TOTAL_IMAGENS, Constantes.TOTAL_IMAGENS);

                //trata a imagem de forma correta
                Bitmap imagemAtual = escolheImagem(posicaoAtual);
                final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                imagemAtual.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
                Asset asset = Asset.createFromBytes(byteArrayOutputStream.toByteArray());
                map.putAsset(Constantes.KEY_IMAGEM, asset);

                Log.d("TesteDataTransmition", "Passou");

                return putDataMapRequest;
            }

            @Override
            protected void onPostExecute(PutDataMapRequest dataMapRequest){
                super.onPostExecute(dataMapRequest);
                //manda o DataItem para todos os dispositivos sincronizados
                Wearable.DataApi.putDataItem(mGoogleApiClient, dataMapRequest.asPutDataRequest());
                Log.d("TesteDataTransmition", "Passou");
            }

        }.execute();

    }

    public Bitmap escolheImagem(int posicaoAtual){
        Imagem imagem = new Imagem();

        //adicionar a imagem é um pouco diferente e necessita da classe Asset
        switch (posicaoAtual) {
            case 0:
                return imagem.getImagem(this, "Palmeiras_enea.jpg", 160, 160);

            case 1:
                return imagem.getImagem(this, "Palmeiras_enea2.jpg", 160, 160);

            case 2:
                return imagem.getImagem(this, "Palmeiras_enea3.jpg", 160, 160);

            case 3:
                return imagem.getImagem(this, "Palmeiras_enea4.jpg", 160, 160);

            default:
                return imagem.getImagem(this, "Palmeiras_enea.jpg", 160, 160);
        }
    }

    private void conectaAosNos(){
        ///acessa todos os nós conectados ao celular, ou seja todos os relógios
        Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).setResultCallback(
                new ResultCallback<NodeApi.GetConnectedNodesResult>() {
                    @Override
                    public void onResult(NodeApi.GetConnectedNodesResult getConnectedNodesResult) {
                        mNodes = getConnectedNodesResult.getNodes();
                    }
                });
    }

    private void voltaImagem(){
        if(mViewPager.getCurrentItem() > 0){
            mViewPager.post(new Runnable() {
                @Override
                public void run() {
                    mViewPager.setCurrentItem(mViewPager.getCurrentItem() - 1, true);
                }
            });
        }

    }

    private void avancaImagem(){
        if(mViewPager.getCurrentItem() < Constantes.TOTAL_IMAGENS){
            mViewPager.post(new Runnable() {
                @Override
                public void run() {
                    mViewPager.setCurrentItem(mViewPager.getCurrentItem() + 1, true);
                }
            });
        }
    }
}
