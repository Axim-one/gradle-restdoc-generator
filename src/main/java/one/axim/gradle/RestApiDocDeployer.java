package one.axim.gradle;

import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class RestApiDocDeployer {

    private File docDir;
    private String serverUrl;
    private String serviceId;

    public RestApiDocDeployer(File docDir, String serverUrl, String serviceId) {
        this.docDir = docDir;
        this.serverUrl = serverUrl;
        this.serviceId = serviceId;
    }

    public void deploy() throws Exception {

        Map<String, Object> service = null;
        List<Object> apis = new ArrayList<>();
        HashMap<String, Object> models = new HashMap<>();

        if (docDir.isDirectory()) {
            File[] files = docDir.listFiles((dir, name) -> name.endsWith(".json"));

            for (File file : files) {
                if (!file.isDirectory()) {
                    System.out.println("Read Service Json : " + file.getName());
                    service = jsonMap(file);
                    break;
                }
            }
        }

        File apiDocDir = new File(docDir, "api");
        if (apiDocDir.isDirectory()) {
            File[] files = apiDocDir.listFiles((dir, name) -> name.endsWith(".json"));

            for (File file : files) {
                if (!file.isDirectory()) {
                    System.out.println("Read API Json : " + file.getName());
                    List<Object> list = jsonList(file);
                    if (list != null) {
                        apis.addAll(list);
                    }
                }
            }
        }

        File modelDocDir = new File(docDir, "model");

        if (modelDocDir.isDirectory()) {
            File[] files = modelDocDir.listFiles();
            System.out.format("---> files.length: %s%n", files.length);
            System.out.format("---> files: %s%n", Arrays.toString(files));
            for (File file : files) {
                if (!file.isDirectory()) {
                    System.out.println("Read Model Json : " + file.getName());
                    models.put(file.getName().replace(".json", ""), jsonMap(file));
                }
            }
        }

        if (service != null) {
            service.put("apis", apis);
            service.put("models", models);
            service.put("errors", new ArrayList<>());
        } else {
            System.out.println("Not found service json");
            return;
        }

        String body = new Gson().toJson(service, HashMap.class);

        if (!StringUtils.isEmpty(serverUrl)) {

            System.out.println(serverUrl + "/upload/" + serviceId + " save api request start");

            try {
                Request.Post(serverUrl + "/upload/" + serviceId).bodyString(body, ContentType.APPLICATION_JSON).execute();
            } catch (IOException e) {
                e.printStackTrace();
            }

            System.out.println(serverUrl + "/upload/" + serviceId + " save api request end");
        }
    }

    @SuppressWarnings("unchecked")
    private List<Object> jsonList(File json) {
        try {
            byte[] encoded = Files.readAllBytes(Paths.get(json.toURI()));
            String s = new String(encoded, StandardCharsets.UTF_8);
            return new Gson().fromJson(s, List.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> jsonMap(File json) {
        try {
            byte[] encoded = Files.readAllBytes(Paths.get(json.toURI()));
            String s = new String(encoded, StandardCharsets.UTF_8);
            return new Gson().fromJson(s, Map.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
