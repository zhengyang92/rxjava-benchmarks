package co.smartreceipts.android.ocr.apis;

import android.support.annotation.NonNull;

import co.smartreceipts.android.ocr.apis.model.RecognitionResponse;
import co.smartreceipts.android.ocr.apis.model.RecongitionRequest;
import io.reactivex.Observable;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface OcrService {

    @POST("api/recognitions")
    Observable<RecognitionResponse> scanReceipt(@NonNull @Body RecongitionRequest request);

    @GET("api/recognitions/{id}")
    Observable<RecognitionResponse> getRecognitionResult(@Path("id") String id);
}
