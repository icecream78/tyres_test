package com.tyres;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.nio.file.StandardCopyOption;

class ParsedData {
    private String filename;
    private String charset;
    private String extension;

    public ParsedData() {
        this.filename = "";
        this.charset = "";
        this.extension = "";
    }

    public ParsedData(String filename, String charset, String extension) {
        this.filename = filename;
        this.charset = charset;
        this.extension = extension;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getFilename() {
        return this.filename;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public String getCharset() {
        return this.charset;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public String getExtension() {
        return this.extension;
    }
}

class ParsedCommandLine {
    private ArrayList<String> urlsList;
    private String outputFile;

    public ParsedCommandLine(ArrayList<String> urlsList, String outputFile) {
        this.urlsList = urlsList;
        this.outputFile = outputFile;
    }

    public ArrayList<String> getUrls() {
        return urlsList;
    }

    public String getOutputFile() {
        return outputFile;
    }
}

public class Main {
    public static void main(String[] args) {
        ParsedCommandLine pcl = parseCommandLine(args);
        ArrayList<String> urlsList = pcl.getUrls();
        String outputFile = pcl.getOutputFile();
        for (int i = 0; i < urlsList.size(); i++) {
            try {
                System.out.println("Downloading data from " + urlsList.get(i) + " ...");
                readFromUrl(urlsList.get(i), outputFile + "_" + Integer.toString(i+1));
                System.out.println("File was downloaded.");

//                //TODO fix downloading video
////            String res = readFromUrl("https://r4---sn-gvnuxaxjvh-n8vk.googlevideo.com/videoplayback?upn=YXLOPKn2yHw&mn=sn-gvnuxaxjvh-n8vk&mm=31&id=o-ALE0lPVfwp-rbeC8zSM4yBLYUFCf96nvlrIG25X1G0y0&gir=yes&mt=1492417216&ms=au&ip=217.107.124.204&key=yt6&initcwndbps=1087500&clen=37053466&ipbits=0&pcm2cms=yes&sparams=clen%2Cdur%2Cei%2Cgir%2Cid%2Cinitcwndbps%2Cip%2Cipbits%2Citag%2Ckeepalive%2Clmt%2Cmime%2Cmm%2Cmn%2Cms%2Cmv%2Cpcm2cms%2Cpl%2Crequiressl%2Csource%2Cupn%2Cexpire&mime=video%2Fwebm&expire=1492438960&source=youtube&keepalive=yes&itag=303&pl=24&dur=240.000&mv=m&requiressl=yes&ei=UHv0WPyoMdTRY-bMkfgM&lmt=1450745448577243&alr=yes&ratebypass=yes&signature=1CEB9D322A67E91725A4F28461E380E8798C56E3.5A81CC758CDF18F994F7C758DE984C034754316C&cpn=VgliV1aTjdk1ByC1&c=WEB&cver=1.20170412&range=10357194-11987578&rn=16&rbuf=80422");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Full download complete.");
    }

    static ParsedCommandLine parseCommandLine(String[] args) {
        if (args.length < 2) {
            throw new IllegalArgumentException("Url was not set or output file was not provided");
        }

        ArrayList<String> urlsList = new ArrayList<String>();
        String first_arg = args[0];
        if (first_arg.startsWith("www.") | first_arg.startsWith("http:") | first_arg.startsWith("https:")) {
            urlsList.add(first_arg);
        } else {
            try (BufferedReader br = new BufferedReader(new FileReader(first_arg))) {
                String currentLine;

                while ((currentLine = br.readLine()) != null) {
                    if (currentLine.startsWith("www")) {
                        currentLine = "http://" + currentLine;
                    }
                    urlsList.add(currentLine);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        String outPutFile = args[1];
        return new ParsedCommandLine(urlsList, outPutFile);
    }


    // write methods to different content-types
    static void writeData(URL url, String filename) throws IOException {
        InputStream in = url.openStream();
        Files.copy(in, Paths.get(filename), StandardCopyOption.REPLACE_EXISTING);

        in.close();
    }

    static void writeData(URLConnection uc,  String filename, String extension) throws IOException {
        final BufferedReader in = new BufferedReader(new InputStreamReader(uc.getInputStream()));
        String inputLine;

        FileOutputStream fos = new FileOutputStream(filename, true);
        while ((inputLine = in.readLine()) != null) {
            fos.write(inputLine.getBytes());
        }

        in.close();
        fos.close();
    }

    static void writeData(URLConnection uc, String filename, String charset, String extension) throws IOException {
        final BufferedReader in = new BufferedReader(new InputStreamReader(uc.getInputStream()));
        String inputLine;

        FileOutputStream fos = new FileOutputStream(filename, true);
            while ((inputLine = in.readLine()) != null) {
                fos.write(inputLine.getBytes(charset));
        }

        in.close();
        fos.close();
    }

    static URLConnection makeRequest(URL url) throws IOException{
        final URLConnection uc = url.openConnection();
        uc.connect();
        if (((HttpURLConnection) uc).getResponseCode() > 400){
            String url_path = uc.getURL().getHost() + uc.getURL().getFile();
            throw new IOException("Error while loading data from url: " + url_path);
        }
        return uc;
    }

    static ParsedData parseHeader(URLConnection uc, String filename) {
        Map<String, String> extensions_dict = new HashMap<String, String>();
        extensions_dict.put("html", "html");
        extensions_dict.put("force-download", "mp3");
        extensions_dict.put("webm", "avi");
        extensions_dict.put("x-msvideo", "avi");

        String extension = "";
        String charset = "";

        String[] content_type = uc.getContentType().split("; ");
        for (int i = 0; i < content_type.length; i++) {
            if (i == 0) {
                extension = content_type[i].split("/")[1];
            }
            if (content_type[i].contains("charset=")) {
                charset = content_type[i].split("charset=")[1];
            }
        }

        if (!filename.contains(".")) {
            if (extensions_dict.get(extension) != null){
                filename = filename + "." + extensions_dict.get(extension);
            } else {
                filename = filename + "." + extension;
            }
        }
        return new ParsedData(filename, charset, extension);
    }

    static void readFromUrl(String urlAddress, String filename) throws IOException {
        if (urlAddress.isEmpty()) {
            throw new IllegalArgumentException("Url must not be empty or null");
        }

        final URL url = new URL(urlAddress);
        final URLConnection uc = makeRequest(url);

        if (((HttpURLConnection) uc).getResponseCode() == 200) {
            ParsedData data = parseHeader(uc, filename);
            String extension = data.getExtension();
            String charset = data.getCharset();
            filename = data.getFilename();

            if (charset.length() != 0) {
                writeData(uc, filename, charset, extension);
            }
            if (extension.equals("avi")) {
                writeData(uc, filename, extension);
            } else if (extension.equals("webm")) {
                writeData(uc, filename, extension);
            } else {
                writeData(url, filename);
            }

            ((HttpURLConnection) uc).disconnect();
        }
    }
}
