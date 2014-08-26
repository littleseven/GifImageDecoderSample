package works.langley.gifimagedecodersample;

import android.app.Activity;
import android.os.Bundle;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.FileAsyncHttpResponseHandler;

import org.apache.http.Header;

import java.io.File;


public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private final MainActivity self = this;

    private static final String REQUEST_URL = "https://38.media.tumblr.com/5e0e15781f221e392d1937a7cbd5936a/tumblr_n8dtcyAUCI1sq9yswo2_500.gif";
    private GifImageView mGifImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mGifImageView = (GifImageView) findViewById(R.id.gifView);
        loadImage();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGifImageView.play();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mGifImageView.stop();
    }

    private void loadImage() {
        AsyncHttpClient client = new AsyncHttpClient();
        client.get(REQUEST_URL, new FileAsyncHttpResponseHandler(self) {

            @Override
            public void onSuccess(int statusCode, Header[] headers, File file) {
                mGifImageView.setGif(file.getPath());
                mGifImageView.play();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, File file) {
            }
        });
    }
}
