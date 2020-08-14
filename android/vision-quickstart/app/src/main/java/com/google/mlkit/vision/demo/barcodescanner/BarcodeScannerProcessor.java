/*
 * Copyright 2020 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.mlkit.vision.demo.barcodescanner;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.common.images.Size;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.barcode.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.demo.CameraSource;
import com.google.mlkit.vision.demo.GraphicOverlay;
import com.google.mlkit.vision.demo.R;
import com.google.mlkit.vision.demo.VisionProcessorBase;

import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.List;

/**
 * Barcode Detector Demo.
 */
public class BarcodeScannerProcessor extends VisionProcessorBase<List<Barcode>> {

    private static final String TAG = "BarcodeProcessor";
    private static String lastBar = "";
    private static Context cntxt;

    private final BarcodeScanner barcodeScanner;

    public BarcodeScannerProcessor(Context context) {
        super(context);
        cntxt = context;
        // Note that if you know which format of barcode your app is dealing with, detection will be
        // faster to specify the supported barcode formats one by one, e.g.
        // new BarcodeScannerOptions.Builder()
        //     .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
        //     .build();
        barcodeScanner = BarcodeScanning.getClient();
    }

    @Override
    public void stop() {
        super.stop();
        barcodeScanner.close();
    }

    @Override
    protected Task<List<Barcode>> detectInImage(InputImage image) {
        return barcodeScanner.process(image);
    }

    @Override
    protected void onSuccess(
            @NonNull List<Barcode> barcodes, @NonNull GraphicOverlay graphicOverlay) {
        if (barcodes.isEmpty()) {
            Log.v(MANUAL_TESTING_LOG, "No barcode has been detected");
        }
        for (int i = 0; i < barcodes.size(); ++i) {
            Barcode barcode = barcodes.get(i);
            graphicOverlay.add(new BarcodeGraphic(graphicOverlay, barcode));
            store2Clipboard(barcode.getRawValue());
            if (!lastBar.equals(barcode.getRawValue())) {
                lastBar = barcode.getRawValue();
                store2File(lastBar+"\n");
                //logExtrasForTesting(barcode);
            }
        }
    }

    public static File makeFileName() {//Make full file name to external storage
        String sPath = PreferenceManager
                .getDefaultSharedPreferences(cntxt)
                .getString(cntxt.getString(R.string.pref_key_file_path),
                Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator);
        return new File(new File(sPath), "barcode.txt");
    }

    public static String store2Clipboard(String sData) {
        ClipboardManager clipboard = (ClipboardManager) cntxt.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(sData, sData);
        clipboard.setPrimaryClip(clip);
        return sData;
    }

    public static String store2File(String sData) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(makeFileName(),true));
            bw.write(sData);
            bw.close();
        }
        catch (Exception e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
        return sData;
    }

    public static void clearFile() {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(makeFileName()));
            bw.write("");
            bw.close();
        }
        catch (Exception e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    public static String readFile() {
        String sRes="";
        try {
            FileReader fr = new FileReader(makeFileName());
            BufferedReader bfr = new BufferedReader(fr);
            sRes = bfr.readLine();
        }
        catch (Exception e) {
            Log.e("Exception", "File read error: " + e.toString());
        }
        return sRes;
    }

    private static void logExtrasForTesting(Barcode barcode) {
        if (barcode != null) {
            Log.v(
                    MANUAL_TESTING_LOG,
                    String.format(
                            "Detected barcode's bounding box: %s", barcode.getBoundingBox().flattenToString()));
            Log.v(
                    MANUAL_TESTING_LOG,
                    String.format(
                            "Expected corner point size is 4, get %d", barcode.getCornerPoints().length));
            for (Point point : barcode.getCornerPoints()) {
                Log.v(
                        MANUAL_TESTING_LOG,
                        String.format("Corner point is located at: x = %d, y = %d", point.x, point.y));
            }
            Log.v(MANUAL_TESTING_LOG, "barcode display value: " + barcode.getDisplayValue());
            Log.v(MANUAL_TESTING_LOG, "barcode raw value: " + barcode.getRawValue());
            Barcode.DriverLicense dl = barcode.getDriverLicense();
            if (dl != null) {
                Log.v(MANUAL_TESTING_LOG, "driver license city: " + dl.getAddressCity());
                Log.v(MANUAL_TESTING_LOG, "driver license state: " + dl.getAddressState());
                Log.v(MANUAL_TESTING_LOG, "driver license street: " + dl.getAddressStreet());
                Log.v(MANUAL_TESTING_LOG, "driver license zip code: " + dl.getAddressZip());
                Log.v(MANUAL_TESTING_LOG, "driver license birthday: " + dl.getBirthDate());
                Log.v(MANUAL_TESTING_LOG, "driver license document type: " + dl.getDocumentType());
                Log.v(MANUAL_TESTING_LOG, "driver license expiry date: " + dl.getExpiryDate());
                Log.v(MANUAL_TESTING_LOG, "driver license first name: " + dl.getFirstName());
                Log.v(MANUAL_TESTING_LOG, "driver license middle name: " + dl.getMiddleName());
                Log.v(MANUAL_TESTING_LOG, "driver license last name: " + dl.getLastName());
                Log.v(MANUAL_TESTING_LOG, "driver license gender: " + dl.getGender());
                Log.v(MANUAL_TESTING_LOG, "driver license issue date: " + dl.getIssueDate());
                Log.v(MANUAL_TESTING_LOG, "driver license issue country: " + dl.getIssuingCountry());
                Log.v(MANUAL_TESTING_LOG, "driver license number: " + dl.getLicenseNumber());
            }
        }
    }

    @Override
    protected void onFailure(@NonNull Exception e) {
        Log.e(TAG, "Barcode detection failed " + e);
    }
}
