package io.tus.java.example;

import io.tus.java.client.*;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.URL;

/**
 * A representative Example class to show an usual usecase.
 */
public final class Main {
    /**
     * Main method to run a standard upload task.
     * @param args
     */
    public static void main(String[] args) {
        try {
            // When Java's HTTP client follows a redirect for a POST request, it will change the
            // method from POST to GET which can be disabled using following system property.
            // If you do not enable strict redirects, the tus-java-client will not follow any
            // redirects but still work correctly.
            System.setProperty("http.strictPostRedirect", "true");

            //String str1 = new doPost();
            //String convertedToString = String.valueOf(obj1);
            // Create a new TusClient instance
            //System.out.println(str1);


            /***************
            hgfhhhhhhhhhhhhhhhhhhhhhfghfhg
            */

            String url = "https://api.cloudflare.com/client/v4/accounts/e60f3efad5b2c687c1d6a32063fd8122/stream?direct_user=true";

            HttpsURLConnection httpClient = (HttpsURLConnection) new URL(url).openConnection();

            //add reuqest header
            httpClient.setRequestMethod("POST");
            httpClient.setRequestProperty("authorization", "bearer ke0oh9MvPfawKYEsfu2KTHP0ZkZXuOFRX30BlwFa");
            httpClient.setRequestProperty("upload-length", "100450390");
            httpClient.setRequestProperty("Tus-Resumable", "1.0.0");

            String urlParameters = "";

            // Send post request
            httpClient.setDoOutput(true);
            try (DataOutputStream wr = new DataOutputStream(httpClient.getOutputStream())) {
                wr.writeBytes(urlParameters);
                wr.flush();
            }

            String responseLocation = httpClient.getHeaderField("Location");
            System.out.println("\nSending 'POST' request to URL : " + url);
            System.out.println("Post parameters : " + urlParameters);
            System.out.println("Location : " + responseLocation);


            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(httpClient.getInputStream()))) {

                String line;
                StringBuilder response = new StringBuilder();

                while ((line = in.readLine()) != null) {
                    response.append(line);
                }

            }
            //print result
            responseLocation.replace("Location: ", "");
            System.out.println(responseLocation.toString());

            // Create a new TusClient instance
            final TusClient client = new TusClient();

            // Configure tus HTTP endpoint. This URL will be used for creating new uploads
            // using the Creation extension
            client.setUploadCreationURL(new URL(responseLocation));

            // Enable resumable uploads by storing the upload URL in memory
            client.enableResuming(new TusURLMemoryStore());

            // Open a file using which we will then create a TusUpload. If you do not have
            // a File object, you can manually construct a TusUpload using an InputStream.
            // See the documentation for more information.
            File file = new File("./example/assets/Samplevod2.mp4");
            final TusUpload upload = new TusUpload(file);

            // You can also upload from an InputStream directly using a bit more work:
            // InputStream stream = …;
            // TusUpload upload = new TusUpload();
            // upload.setInputStream(stream);
            // upload.setSize(sizeOfStream);
            // upload.setFingerprint("stream");


            System.out.println("Starting upload...");

            // We wrap our uploading code in the TusExecutor class which will automatically catch
            // exceptions and issue retries with small delays between them and take fully
            // advantage of tus' resumability to offer more reliability.
            // This step is optional but highly recommended.
            TusExecutor executor = new TusExecutor() {
                @Override
                protected void makeAttempt() throws ProtocolException, IOException {
                    // First try to resume an upload. If that's not possible we will create a new
                    // upload and get a TusUploader in return. This class is responsible for opening
                    // a connection to the remote server and doing the uploading.
                    TusUploader uploader = client.resumeOrCreateUpload(upload);

                    // Upload the file in chunks of 1KB sizes.
                    uploader.setChunkSize(1024);

                    // Upload the file as long as data is available. Once the
                    // file has been fully uploaded the method will return -1
                    do {
                        // Calculate the progress using the total size of the uploading file and
                        // the current offset.
                        long totalBytes = upload.getSize();
                        long bytesUploaded = uploader.getOffset();
                        double progress = (double) bytesUploaded / totalBytes * 100;
                        System.out.printf("Upload at %06.2f%%.\n", progress);
                    } while (uploader.uploadChunk() > -1);

                    // Allow the HTTP connection to be closed and cleaned up
                    uploader.finish();

                    System.out.println("Upload finished.");
                    System.out.format("Upload available at: %s", uploader.getUploadURL().toString());
                }
            };
            executor.makeAttempts();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    private Main() {
        throw new IllegalStateException("Utility class");
    }
}
