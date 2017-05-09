package andl.zanon.wearapis;

import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by Andre on 30/04/2017.
 */

public class ApresentacaoService extends WearableListenerService implements GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks{

    private GoogleApiClient mGoogleApiClient;
    private List<Node> mNodes;

    @Override
    public void onCreate() {
        super.onCreate();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        if(!mGoogleApiClient.isConnected())
            mGoogleApiClient.connect();

        //cria uma lista de nós conectados, ou seja, celulares pareados
        Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).setResultCallback(
                new ResultCallback<NodeApi.GetConnectedNodesResult>() {
                    @Override
                    public void onResult(NodeApi.GetConnectedNodesResult getConnectedNodesResult) {
                        mNodes = getConnectedNodesResult.getNodes();
                    }
                });

        Log.d("TesteDataTransmition", "Iniciou");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mGoogleApiClient.isConnected())
            mGoogleApiClient.disconnect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }


    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    //verifica se existem nós conectados, logo, se está conectado é necessário conectar o GoogleApiClient
    @Override
    public void onPeerConnected(Node peer) {
        super.onPeerConnected(peer);

        if(!mGoogleApiClient.isConnected())
            mGoogleApiClient.connect();

    }

    //verifica nós que podem ser desconectados
    @Override
    public void onPeerDisconnected(Node peer) {
        super.onPeerDisconnected(peer);

        if(mGoogleApiClient.isConnected())
            mGoogleApiClient.disconnect();
    }

    //método que trata a mudança de dados no celular e conseguentemente no relógio.
    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        final List<DataEvent> events = FreezableUtils.freezeIterable(dataEvents);
        for(DataEvent event : events ){
            //obetem uri do caminho de dados
            Uri uri  = event.getDataItem().getUri();

            Log.d("TesteDataTransmition", "Chegou");
            //se a uri do caminho for a que desejar
            if(Constantes.CAMINHO_DADOS.equals(uri.getPath())){
                //evento de mudança de dados
                if(event.getType() == DataEvent.TYPE_CHANGED){
                    //muda de estado, ou seja, recebe novos dados
                    mudancaDeEstado(event);

                //evento de dados deletados
                } else if(event.getType() == DataEvent.TYPE_DELETED){
                    Log.d("TYPE_DELETED", "Deletou");
                    NotificationManagerCompat.from(this).cancel(Constantes.KEY_NOTIFICACAO);
                }
            }
        }

    }

    //comando dado quando o serviço esta sendo executado. Dessa maneira, o método ira falar para ir a proxima imagem, ou a imagem anterior
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(Constantes.ACAO_ANTERIOR_SLIDE.equals(intent.getAction()) && mNodes != null){
            for(Node node : mNodes){
                // envia mensagem a todos os nós.
                //como parâmetros do métodos tempos o cliente declarado para fazer o intermédio entre os nos
                // o id do nó que irá receber
                // o caminho que começa sempre por "/"
                // o Byte   que comunica o que fazer ao receptor
                Wearable.MessageApi.sendMessage(mGoogleApiClient, node.getId(), Constantes.CAMINHO_PROX_ANT,
                        new byte[]{Constantes.ANTERIOR});
            }
        }

        else if(Constantes.ACAO_PROXIMO_SLIDE.equals(intent.getAction()) && mNodes!= null){
            for(Node node : mNodes){
                Wearable.MessageApi.sendMessage(mGoogleApiClient, node.getId(), Constantes.CAMINHO_PROX_ANT,
                        new byte[]{Constantes.PROXIMO});
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    //método de recebimento de mensagem de um outro nó, no caso o celular.
    //caso receba mensagem de saída do app, ou seja onStop() da MainActivity do Mobile, notificação é excluúda
    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        super.onMessageReceived(messageEvent);
        //verifica caminho recebido com o de saída
        if(Constantes.CAMINHO_SAIR.equals(messageEvent.getPath())){
            NotificationManagerCompat.from(this).cancel(Constantes.KEY_NOTIFICACAO);
        }
    }

    //acessa o DataMap para pegar os dados para o slide atual
    private void mudancaDeEstado(DataEvent event){
        DataMap dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();

        int slideAtual = dataMap.getInt(Constantes.KEY_FOTO_ATUAL);
        int total = dataMap.getInt(Constantes.KEY_TOTAL_IMAGENS);
        Asset imagem = dataMap.getAsset(Constantes.KEY_IMAGEM);

        //lancça notificação
        notificar(slideAtual, total, imagem);
    }

    //notificação básica com extensão para android wear
    private void notificar(int slideAtual, int totalDeImagens, Asset asset){
        Intent proximoSlide = new Intent(Constantes.ACAO_PROXIMO_SLIDE);
        Intent anteriorSlide = new Intent(Constantes.ACAO_ANTERIOR_SLIDE);

        PendingIntent proxPendingIntent = PendingIntent.getService(this, 0, proximoSlide, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent antPendingIntent = PendingIntent.getService(this, 0, anteriorSlide, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setContentTitle("Imagens")
                .setContentText((slideAtual + 1) + " / " + totalDeImagens)
                .setOngoing(true)
                .setSmallIcon(R.mipmap.ic_launcher);

        NotificationCompat.WearableExtender extender = new NotificationCompat.WearableExtender();
        extender.addAction(new NotificationCompat.Action(android.R.drawable.ic_media_previous, "Anterior", antPendingIntent));
        extender.addAction(new NotificationCompat.Action(android.R.drawable.ic_media_next, "Proximo", proxPendingIntent));
        //extender.setBackground(carregaBitmapFromAsset(asset));

        extender.setHintHideIcon(true);
        builder.extend(extender);
        notificationManagerCompat.notify(Constantes.KEY_NOTIFICACAO, builder.build());
    }

    //carrega o Bitmap conforme os padrões do android wear
    private Bitmap carregaBitmapFromAsset(Asset asset){
        if(asset == null)
            throw new IllegalArgumentException("Asset must be not null");

        InputStream assetInputStream = Wearable.DataApi.getFdForAsset(
                mGoogleApiClient, asset).await().getInputStream();
        mGoogleApiClient.disconnect();

        return BitmapFactory.decodeStream(assetInputStream);
    }
}
