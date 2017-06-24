package info.androidhive.speechtotext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Locale;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends Activity {

	private TextView txtSpeechInput;
	private ImageButton btnSpeak;
	private String textSpeech;
	private final int REQ_CODE_SPEECH_INPUT = 100;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		txtSpeechInput = (TextView) findViewById(R.id.txtSpeechInput);
		btnSpeak = (ImageButton) findViewById(R.id.btnSpeak);

		// hide the action bar
		getActionBar().hide();

		btnSpeak.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				promptSpeechInput();
			}
		});

	}

	/**
	 * Showing google speech input dialog
	 * */
	private void promptSpeechInput() {
		Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
				RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
		intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
				getString(R.string.speech_prompt));
		try {
			startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
		} catch (ActivityNotFoundException a) {
			Toast.makeText(getApplicationContext(),
					getString(R.string.speech_not_supported),
					Toast.LENGTH_SHORT).show();
		}
	}

	/**
	 * Receiving speech input
	 * */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		switch (requestCode) {
		case REQ_CODE_SPEECH_INPUT: {
			if (resultCode == RESULT_OK && null != data) {

				ArrayList<String> result = data
						.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
				textSpeech = result.get(0).toLowerCase();
				CallHabitaclia(textSpeech);
			}
			break;
		}

		}
	}

	private void CallHabitaclia(String phrase)
	{
        txtSpeechInput.setText(phrase);

        new HttpAsyncTask().execute("https://m.habitaclia.com/dotnet/buscador/find");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

    private class HttpAsyncTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            return POST(urls[0]);
        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
			try {
				JSONArray jsonObj = new JSONArray (result);
				String url = jsonObj.getJSONObject(0).getString("UrlNavigation");
				OpenBrowser(url);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
    }

	private void OpenBrowser(String url) {
		Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
		startActivity(browserIntent);
	}

	public String POST(String url){
        InputStream inputStream = null;
        String result = "";
        try {

            String tipoOperacion = "V";

			if (textSpeech.contains("comprar")) {
				tipoOperacion = "V";
			}
			else if (textSpeech.contains("alquilar"))  {
				tipoOperacion = "A";
			}
			else if (textSpeech.contains("temporada"))  {
				tipoOperacion = "H";
			}
			else if (textSpeech.contains("traspaso"))  {
				tipoOperacion = "T";
			}

			String tipoProducto = "vivienda";

			if (textSpeech.contains("oficina")) {
				tipoProducto = "oficina";
			}
			else if (textSpeech.contains("local"))  {
				tipoProducto = "local_comercial";
			}
			else if (textSpeech.contains("aparcamiento") || textSpeech.contains("parking") )  {
				tipoProducto = "aparcamiento";
			}
			else if (textSpeech.contains("industrial") || textSpeech.contains("nave"))  {
				tipoProducto = "industrial";
			}
			else if (textSpeech.contains("terreno") || textSpeech.contains("solar"))  {
				tipoProducto = "terrenos_y_solares";
			}
			else if (textSpeech.contains("inmueble") || textSpeech.contains("castillo"))  {
				tipoProducto = "inmuebles_singulares";
			}

			String city = textSpeech.substring(textSpeech.indexOf(" en ") + 4, textSpeech.length());

			// 1. create HttpClient
			HttpClient httpclient = new DefaultHttpClient();

			// 2. make POST request to the given URL
			HttpPost httpPost = new HttpPost(url);

			ArrayList postParameters = new ArrayList<NameValuePair>();
			postParameters.add(new BasicNameValuePair("tipoOperacion", tipoOperacion));
			postParameters.add(new BasicNameValuePair("tipoProducto[Des_Tip]", tipoProducto));
			postParameters.add(new BasicNameValuePair("textFromBuscador", city));

			httpPost.setEntity(new UrlEncodedFormEntity(postParameters));

            // 7. Set some headers to inform server about the type of the content
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/x-www-form-urlencoded");

            // 8. Execute POST request to the given URL
            HttpResponse httpResponse = httpclient.execute(httpPost);

            // 9. receive response as inputStream
            inputStream = httpResponse.getEntity().getContent();

            // 10. convert inputstream to string
            if(inputStream != null) {
                result = convertInputStreamToString(inputStream);
            } else
                result = "Did not work!";

        } catch (Exception e) {
            Log.d("InputStream", e.getLocalizedMessage());
        }

        // 11. return result
        return result;
    }

	public String convertInputStreamToString(InputStream is) {
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		StringBuilder sb = new StringBuilder();
		String line = null;

		try {
			while ((line = reader.readLine()) != null) {
				sb.append(line + "\n");
			}
		} catch (IOException e) {
		} finally {
			try {
				is.close();
			} catch (IOException e) {
			}
		}

		return sb.toString();
	}

}

